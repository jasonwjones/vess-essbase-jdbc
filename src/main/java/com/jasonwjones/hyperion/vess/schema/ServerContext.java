package com.jasonwjones.hyperion.vess.schema;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appliedolap.essbase.AbstractEssObject;
import com.appliedolap.essbase.ApiContext;
import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssApplication;
import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssServer;
import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.outline.Outline;
import com.jasonwjones.hyperion.vess.outline.OutlineParser;

/**
 * What every table of one connection shares: the server, whether the
 * connection may change it, and the outlines of its cubes.
 *
 * <p>
 * Outlines are kept for a while, across connections, because they're read
 * whole and tools open connections freely: a database tool browsing a server
 * would otherwise read every outline again each time it looked.
 */
public final class ServerContext {

	private static final Logger logger = LoggerFactory.getLogger(ServerContext.class);

	private static final Map<String, CachedOutline> OUTLINES = new ConcurrentHashMap<>();

	private static final AtomicLong CONNECTIONS = new AtomicLong();

	private record CachedOutline(Outline outline, long loaded) {
	}

	private final EssServer server;

	private final String url;

	private final String user;

	private final BooleanSupplier readOnly;

	private final Settings settings;

	private final String id = "vess" + CONNECTIONS.incrementAndGet() + "/";

	private ApiContext api;

	private MdxQuery mdx;

	/** Which applications are running, as last asked, and when */
	private Map<String, Boolean> started;

	private long startedAsked;

	/**
	 * Runs an MDX query as a result set, which the connection provides.
	 */
	@FunctionalInterface
	public interface MdxQuery {

		java.sql.ResultSet run(String mdx) throws java.sql.SQLException;

	}

	public ServerContext(EssServer server, String url, String user, BooleanSupplier readOnly, Settings settings) {
		this.server = server;
		this.url = url;
		this.user = user;
		this.readOnly = readOnly;
		this.settings = settings;
	}

	/**
	 * The settings that tune how Vess reads, from the connection's properties.
	 *
	 * @param outlineCache how long an outline is kept before it's read again
	 * @param chunkCells how many cells one MDX query reads at most, before
	 *            leaving out what's empty
	 * @param chunkMembers how many members one MDX query names at most
	 * @param serverCatalog the name of the catalog holding the server's own
	 *            tables
	 * @param browseStopped whether browsing lists the tables of a stopped
	 *            application's cubes that need its outline, which starts it
	 * @param naming how names from Essbase become names of tables and columns
	 */
	public record Settings(Duration outlineCache, long chunkCells, int chunkMembers, String serverCatalog, boolean browseStopped,
			Naming naming) {

		public static final Settings DEFAULTS = new Settings(Duration.ofMinutes(5), 500_000, 2_000, "ESSBASE", false, Naming.DEFAULTS);

	}

	/**
	 * @return what tells this connection's things apart from another's, such
	 *         as which outline a function looks in
	 */
	public String getId() {
		return id;
	}

	public void setMdxQuery(MdxQuery mdx) {
		this.mdx = mdx;
	}

	/**
	 * Runs an MDX query through the connection, for MDX scripts.
	 */
	public java.sql.ResultSet mdx(String query) throws java.sql.SQLException {
		if (mdx == null) {
			throw new VessException("This connection can't run MDX");
		}
		return mdx.run(query);
	}

	public EssServer getServer() {
		return server;
	}

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}

	public Settings getSettings() {
		return settings;
	}

	public boolean isReadOnly() {
		return readOnly.getAsBoolean();
	}

	/**
	 * @throws VessException if the connection is read-only, saying what it
	 *             would have done
	 */
	public void checkWritable(String what) throws VessException {
		if (isReadOnly()) {
			throw new VessException("The connection is read-only, so Vess won't " + what);
		}
	}

	public EssApplication application(String name) throws VessException {
		try {
			for (EssApplication application : server.getApplications()) {
				if (application.getName().equalsIgnoreCase(name)) {
					return application;
				}
			}
		} catch (EssApiException e) {
			throw new VessException("Could not list the applications: " + e.getMessage(), e);
		}
		throw new VessException("There's no application named " + name);
	}

	public EssCube cube(String application, String cube) throws VessException {
		EssApplication app = application(application);
		try {
			for (EssCube each : app.getCubes()) {
				if (each.getName().equalsIgnoreCase(cube)) {
					return each;
				}
			}
		} catch (EssApiException e) {
			throw new VessException("Could not list the cubes of " + application + ": " + e.getMessage(), e);
		}
		throw new VessException("There's no cube named " + cube + " in " + application);
	}

	/**
	 * A cube's outline, from the cache if it was read recently enough.
	 */
	public Outline outline(String application, String cube) throws VessException {
		String key = (url + "|" + user + "|" + application + "." + cube).toLowerCase(Locale.ROOT);
		CachedOutline cached = OUTLINES.get(key);
		long now = System.currentTimeMillis();
		if (cached != null && now - cached.loaded() < settings.outlineCache().toMillis()) {
			return cached.outline();
		}
		EssCube essCube = cube(application, cube);
		try {
			long started = System.currentTimeMillis();
			byte[] xml = essCube.getOutline().downloadXml();
			Outline outline = OutlineParser.parse(essCube.getApplication().getName(), essCube.getName(), xml);
			logger.info("Read the outline of {}.{} ({} bytes, {} members) in {} ms", application, cube, xml.length,
					outline.getMemberCount(), System.currentTimeMillis() - started);
			OUTLINES.put(key, new CachedOutline(outline, now));
			return outline;
		} catch (EssApiException e) {
			throw new VessException("Could not read the outline of " + application + "." + cube + ": " + e.getMessage(), e);
		} catch (XMLStreamException e) {
			throw new VessException("Could not make sense of the outline of " + application + "." + cube + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Whether an application is running, as the server said in the last few
	 * seconds. Reading an outline starts a stopped application, which browsing
	 * shouldn't do.
	 */
	public synchronized boolean isStarted(String application) {
		long now = System.currentTimeMillis();
		if (started == null || now - startedAsked > 15_000) {
			Map<String, Boolean> statuses = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			try {
				for (com.appliedolap.essbase.client.model.Application each : api().getApplicationsApi()
						.applicationsGetApplications(null, 0, 10_000, null, null, null, null).getItems()) {
					statuses.put(each.getName(), "started".equalsIgnoreCase(each.getStatus()));
				}
			} catch (com.appliedolap.essbase.client.ApiException | VessException | EssApiException e) {
				logger.debug("Could not tell which applications are running: {}", e.toString());
				return false;
			}
			started = statuses;
			startedAsked = now;
		}
		return started.getOrDefault(application, false);
	}

	/**
	 * Forgets which applications are running, after one is started or
	 * stopped.
	 */
	public synchronized void forgetStatuses() {
		started = null;
	}

	/**
	 * Whether a cube's outline is at hand, without asking the server.
	 */
	public boolean hasOutline(String application, String cube) {
		CachedOutline cached = OUTLINES.get((url + "|" + user + "|" + application + "." + cube).toLowerCase(Locale.ROOT));
		return cached != null && System.currentTimeMillis() - cached.loaded() < settings.outlineCache().toMillis();
	}

	/**
	 * Forgets the outlines this connection's server has, so they're read again.
	 */
	public void forgetOutlines() {
		String prefix = (url + "|" + user + "|").toLowerCase(Locale.ROOT);
		OUTLINES.keySet().removeIf(key -> key.startsWith(prefix));
	}

	/**
	 * The REST API itself, for the few things essbase-rest-client doesn't wrap:
	 * job history and streaming data loads. The library doesn't hand it out, so
	 * it's taken from the server object.
	 */
	public synchronized ApiContext api() throws VessException {
		if (api == null) {
			try {
				Field field = AbstractEssObject.class.getDeclaredField("api");
				field.setAccessible(true);
				api = (ApiContext) field.get(server);
			} catch (ReflectiveOperationException | RuntimeException e) {
				throw new VessException("Could not reach the REST API underneath essbase-rest-client: " + e, e);
			}
		}
		return api;
	}

}
