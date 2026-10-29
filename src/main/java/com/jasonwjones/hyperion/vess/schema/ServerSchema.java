package com.jasonwjones.hyperion.vess.schema;

import static com.jasonwjones.hyperion.vess.schema.Column.MEMBER_NAME;
import static com.jasonwjones.hyperion.vess.schema.Column.OBJECT_NAME;
import static com.jasonwjones.hyperion.vess.schema.Column.TEXT;
import static com.jasonwjones.hyperion.vess.schema.Column.VARIABLE_NAME;
import static com.jasonwjones.hyperion.vess.schema.Column.VARIABLE_VALUE;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssApplication;
import com.appliedolap.essbase.EssApplicationConfiguration;
import com.appliedolap.essbase.EssConnection;
import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssDataSource;
import com.appliedolap.essbase.EssDataSourceColumn;
import com.appliedolap.essbase.EssGroup;
import com.appliedolap.essbase.EssJob;
import com.appliedolap.essbase.EssLogEntry;
import com.appliedolap.essbase.EssMdxFunction;
import com.appliedolap.essbase.EssMdxFunctionGroup;
import com.appliedolap.essbase.EssPermission;
import com.appliedolap.essbase.EssServer;
import com.appliedolap.essbase.EssSession;
import com.appliedolap.essbase.EssUser;
import com.appliedolap.essbase.EssVariable;
import com.appliedolap.essbase.client.ApiException;
import com.appliedolap.essbase.client.model.Application;
import com.appliedolap.essbase.client.model.Cube;
import com.appliedolap.essbase.client.model.JobRecordBean;
import com.appliedolap.essbase.client.model.JobRecordPaginatedResultWrapper;
import com.jasonwjones.hyperion.vess.VessException;

/**
 * The server's own schema: its applications and cubes, sessions, users and
 * groups, substitution variables, jobs, logs, connections, and the MDX
 * functions it knows. It lives in a catalog of its own, ESSBASE.SERVER.
 */
public final class ServerSchema extends AbstractSchema implements VessSchema {

	public static final String NAME = "SERVER";

	private final ServerContext context;

	private List<VessTable> tables;

	private List<Procedure> procedures;

	/** Run after REFRESH, to find applications that came along later */
	private final Runnable refresh;

	public ServerSchema(ServerContext context, Runnable refresh) {
		this.context = context;
		this.refresh = refresh;
	}

	@Override
	public String getCatalogName() {
		return context.getSettings().serverCatalog();
	}

	@Override
	public String getSchemaName() {
		return NAME;
	}

	@Override
	public String getRemarks() {
		return "The Essbase server itself: applications, cubes, sessions, security, variables, jobs and logs";
	}

	@Override
	protected Map<String, Table> getTableMap() {
		Map<String, Table> map = new LinkedHashMap<>();
		for (VessTable table : getVessTables()) {
			map.put(table.getName(), table);
		}
		return map;
	}

	@Override
	public synchronized List<VessTable> getVessTables() {
		if (tables == null) {
			tables = List.of(new Applications(), new Cubes(), new Variables(), new Sessions(), users(), groups(), groupMembers(),
					permissions(), jobs(), log(), configuration(), connections(), datasources(), datasourceColumns(), mdxFunctions(),
					properties());
		}
		return tables;
	}

	@Override
	public List<OutlineFunction> getOutlineFunctions() {
		return List.of();
	}

	@Override
	public synchronized List<Procedure> getProcedures() {
		if (procedures == null) {
			String prefix = "CALL " + getCatalogName() + "." + NAME + ".";
			List<Column> application = List.of(Column.text("APPLICATION", OBJECT_NAME));
			procedures = List.of(new Procedure("START_APPLICATION", "Starts an application: " + prefix + "START_APPLICATION('Sample')",
					application, true, arguments -> {
						EssApplication started = context.application(argument(arguments, 0, "the application"));
						context.checkWritable("start " + started.getName());
						started.start();
						context.forgetStatuses();
						return result("Started " + started.getName());
					}), new Procedure("STOP_APPLICATION", "Stops an application: " + prefix + "STOP_APPLICATION('Sample')", application, true,
							arguments -> {
								EssApplication stopped = context.application(argument(arguments, 0, "the application"));
								context.checkWritable("stop " + stopped.getName());
								stopped.stop();
								context.forgetStatuses();
								return result("Stopped " + stopped.getName());
							}),
					new Procedure("REFRESH", "Forgets the outlines and applications Vess has read, so it reads them again: " + prefix + "REFRESH()",
							List.of(), false, arguments -> {
								context.forgetOutlines();
								refresh.run();
								return result("Vess will read the server afresh");
							}));
		}
		return procedures;
	}

	static String argument(List<Object> arguments, int index, String what) throws VessException {
		if (arguments.size() <= index || arguments.get(index) == null) {
			throw new VessException("Give " + what);
		}
		return arguments.get(index).toString();
	}

	static Procedure.Result result(String message) {
		List<Object[]> rows = new ArrayList<>();
		rows.add(new Object[] { message });
		return Procedure.Result.of(List.of(Column.text("RESULT", TEXT)), rows);
	}

	static Instant instant(Long millis) {
		return millis == null || millis <= 0 ? null : Instant.ofEpochMilli(millis);
	}

	static String capitalized(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1).toLowerCase(Locale.ROOT);
	}

	/**
	 * The applications a query wants: those it names, or all of them.
	 */
	private List<EssApplication> applications(Set<String> names) throws VessException {
		List<EssApplication> applications = new ArrayList<>();
		try {
			for (EssApplication application : context.getServer().getApplications()) {
				if (names == null || names.stream().anyMatch(name -> name.equalsIgnoreCase(application.getName()))) {
					applications.add(application);
				}
			}
		} catch (EssApiException e) {
			throw new VessException("Could not list the applications: " + e.getMessage(), e);
		}
		return applications;
	}

	private final class Applications extends VessTable {

		private final List<Column> columns = List.of(Column.text("NAME", OBJECT_NAME).notNull(), Column.text("TYPE", 10).remarks("BSO or ASO"),
				Column.text("STATUS", 10).writable().remarks("Started or Stopped; set it to start or stop the application"),
				Column.text("OWNER", MEMBER_NAME), Column.text("DESCRIPTION", TEXT), Column.timestamp("CREATED"),
				Column.timestamp("MODIFIED"), Column.text("MODIFIED_BY", MEMBER_NAME), Column.timestamp("STARTED"),
				Column.integer("CONNECTED_USERS"));

		Applications() {
			super("APPLICATIONS", "The server's applications; setting STATUS starts or stops one");
		}

		@Override
		public List<Column> getColumns() {
			return columns;
		}

		@Override
		public List<String> getPrimaryKey() {
			return List.of("NAME");
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
			List<Object[]> rows = new ArrayList<>();
			try {
				for (Application application : context.api().getApplicationsApi().applicationsGetApplications(null, 0, 10_000, null, null, null, null)
						.getItems()) {
					rows.add(new Object[] { application.getName(), application.getType() == null ? null : application.getType().getValue(),
							capitalized(application.getStatus()), application.getOwner(), application.getDescription(),
							instant(application.getCreationTime()), instant(application.getModifiedTime()), application.getModifiedBy(),
							instant(application.getStartTime()), application.getConnectedUsersCount() });
				}
			} catch (ApiException | EssApiException e) {
				throw new VessException("Could not list the applications: " + e.getMessage(), e);
			}
			return rows;
		}

		@Override
		public boolean canUpdate() {
			return true;
		}

		@Override
		public void update(List<Object[]> before, List<Object[]> after) throws SQLException {
			for (int index = 0; index < before.size(); index++) {
				List<String> changed = changedReadOnly(before.get(index), after.get(index));
				if (!changed.isEmpty()) {
					throw new VessException("Of an application, only STATUS can be changed, not " + String.join(", ", changed));
				}
			}
			for (int index = 0; index < before.size(); index++) {
				String name = (String) before.get(index)[0];
				String status = (String) after.get(index)[2];
				EssApplication application = context.application(name);
				if (startStop(status)) {
					context.checkWritable("start " + name);
					application.start();
				} else {
					context.checkWritable("stop " + name);
					application.stop();
				}
				context.forgetStatuses();
			}
		}

	}

	/**
	 * @return true to start, false to stop
	 */
	static boolean startStop(String status) throws VessException {
		String wanted = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
		switch (wanted) {
		case "started":
		case "start":
		case "running":
			return true;
		case "stopped":
		case "stop":
			return false;
		default:
			throw new VessException("STATUS can be set to Started or Stopped, not '" + status + "'");
		}
	}

	private final class Cubes extends VessTable {

		private final List<Column> columns = List.of(Column.text("APPLICATION", OBJECT_NAME).notNull(), Column.text("NAME", OBJECT_NAME).notNull(),
				Column.text("TYPE", 10).remarks("BSO or ASO"),
				Column.text("STATUS", 10).writable().remarks("Started or Stopped; set it to start or stop the cube"),
				Column.text("OWNER", MEMBER_NAME), Column.text("DESCRIPTION", TEXT), Column.timestamp("CREATED"),
				Column.timestamp("MODIFIED"), Column.text("MODIFIED_BY", MEMBER_NAME), Column.timestamp("STARTED"));

		Cubes() {
			super("CUBES", "Every application's cubes; setting STATUS starts or stops one");
		}

		@Override
		public List<Column> getColumns() {
			return columns;
		}

		@Override
		public List<String> getPrimaryKey() {
			return List.of("APPLICATION", "NAME");
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
			List<Object[]> rows = new ArrayList<>();
			for (EssApplication application : applications(conditions.values(0))) {
				try {
					for (Cube cube : context.api().getApplicationsApi().applicationsGetCubes(application.getName(), null, null, null).getItems()) {
						rows.add(new Object[] { application.getName(), cube.getName(), cube.getType() == null ? null : cube.getType().getValue(),
								capitalized(cube.getStatus()), cube.getOwner(), cube.getDescription(), instant(cube.getCreationTime()),
								instant(cube.getModifiedTime()), cube.getModifiedBy(), instant(cube.getStartTime()) });
					}
				} catch (ApiException | EssApiException e) {
					throw new VessException("Could not list the cubes of " + application.getName() + ": " + e.getMessage(), e);
				}
			}
			return rows;
		}

		@Override
		public boolean canUpdate() {
			return true;
		}

		@Override
		public void update(List<Object[]> before, List<Object[]> after) throws SQLException {
			for (int index = 0; index < before.size(); index++) {
				List<String> changed = changedReadOnly(before.get(index), after.get(index));
				if (!changed.isEmpty()) {
					throw new VessException("Of a cube, only STATUS can be changed, not " + String.join(", ", changed));
				}
			}
			for (int index = 0; index < before.size(); index++) {
				String application = (String) before.get(index)[0];
				String name = (String) before.get(index)[1];
				EssCube cube = context.cube(application, name);
				if (startStop((String) after.get(index)[3])) {
					context.checkWritable("start " + application + "." + name);
					cube.start();
				} else {
					context.checkWritable("stop " + application + "." + name);
					cube.stop();
				}
				context.forgetStatuses();
			}
		}

	}

	/**
	 * Substitution variables at every level: the server's, each
	 * application's, and each cube's.
	 */
	private final class Variables extends VessTable {

		private final List<Column> columns = List.of(Column.text("SCOPE", 11).notNull().remarks("Server, Application or Cube"),
				Column.text("APPLICATION", OBJECT_NAME).remarks("The application, for an application's or a cube's variable"),
				Column.text("CUBE", OBJECT_NAME).remarks("The cube, for a cube's variable"), Column.text("NAME", VARIABLE_NAME).notNull(),
				Column.text("VALUE", VARIABLE_VALUE).notNull().writable());

		Variables() {
			super("VARIABLES", "Substitution variables at every scope; insert, update and delete them here");
		}

		@Override
		public List<Column> getColumns() {
			return columns;
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
			List<Object[]> rows = new ArrayList<>();
			String scope = conditions.value(0);
			Set<String> applications = conditions.values(1);
			try {
				if (scope == null || scope.equalsIgnoreCase("Server")) {
					for (EssVariable variable : context.getServer().getVariables()) {
						rows.add(new Object[] { "Server", null, null, variable.getName(), variable.getValue() });
					}
				}
				if (scope != null && scope.equalsIgnoreCase("Server")) {
					return rows;
				}
				for (EssApplication application : applications(applications)) {
					if (scope == null || scope.equalsIgnoreCase("Application")) {
						for (EssVariable variable : application.getVariables()) {
							rows.add(new Object[] { "Application", application.getName(), null, variable.getName(), variable.getValue() });
						}
					}
					if (scope == null || scope.equalsIgnoreCase("Cube")) {
						for (EssCube cube : application.getCubes()) {
							for (EssVariable variable : cube.getVariables()) {
								rows.add(new Object[] { "Cube", application.getName(), cube.getName(), variable.getName(), variable.getValue() });
							}
						}
					}
				}
			} catch (EssApiException e) {
				throw new VessException("Could not read the substitution variables: " + e.getMessage(), e);
			}
			return rows;
		}

		@Override
		public boolean canInsert() {
			return true;
		}

		@Override
		public boolean canUpdate() {
			return true;
		}

		@Override
		public boolean canDelete() {
			return true;
		}

		@Override
		public void insert(List<Object[]> rows) throws SQLException {
			for (Object[] row : rows) {
				String application = (String) row[1];
				String cube = (String) row[2];
				String name = (String) row[3];
				String value = (String) row[4];
				String scope = scopeOf((String) row[0], application, cube);
				if (name == null || value == null) {
					throw new VessException("A substitution variable needs a NAME and a VALUE");
				}
				context.checkWritable("create the variable " + name);
				try {
					switch (scope) {
					case "Server":
						context.getServer().createVariable(name, value);
						break;
					case "Application":
						context.application(application).createVariable(name, value);
						break;
					default:
						context.cube(application, cube).createVariable(name, value);
						break;
					}
				} catch (EssApiException e) {
					throw new VessException("Could not create the variable " + name + ": " + e.getMessage(), e);
				}
			}
		}

		@Override
		public void update(List<Object[]> before, List<Object[]> after) throws SQLException {
			for (int index = 0; index < before.size(); index++) {
				List<String> changed = changedReadOnly(before.get(index), after.get(index));
				if (!changed.isEmpty()) {
					throw new VessException("Of a substitution variable, only VALUE can be changed, not " + String.join(", ", changed)
							+ "; to rename or move one, delete it and insert it again");
				}
			}
			for (int index = 0; index < before.size(); index++) {
				Object[] row = before.get(index);
				EssVariable variable = find(row);
				context.checkWritable("change the variable " + variable.getName());
				try {
					variable.setValue((String) after.get(index)[4]);
				} catch (EssApiException e) {
					throw new VessException("Could not change the variable " + variable.getName() + ": " + e.getMessage(), e);
				}
			}
		}

		@Override
		public void delete(List<Object[]> rows) throws SQLException {
			for (Object[] row : rows) {
				EssVariable variable = find(row);
				context.checkWritable("delete the variable " + variable.getName());
				try {
					variable.delete();
				} catch (EssApiException e) {
					throw new VessException("Could not delete the variable " + variable.getName() + ": " + e.getMessage(), e);
				}
			}
		}

		private EssVariable find(Object[] row) throws SQLException {
			String scope = (String) row[0];
			String name = (String) row[3];
			List<? extends EssVariable> variables;
			switch (scope) {
			case "Server":
				variables = context.getServer().getVariables();
				break;
			case "Application":
				variables = context.application((String) row[1]).getVariables();
				break;
			default:
				variables = context.cube((String) row[1], (String) row[2]).getVariables();
				break;
			}
			for (EssVariable variable : variables) {
				if (variable.getName().equalsIgnoreCase(name)) {
					return variable;
				}
			}
			throw new VessException("There's no longer a variable " + name);
		}

	}

	/**
	 * Which scope a new variable has: what SCOPE says, or what the application
	 * and cube given imply.
	 */
	static String scopeOf(String scope, String application, String cube) throws VessException {
		String implied = application == null ? "Server" : cube == null ? "Application" : "Cube";
		if (scope == null) {
			return implied;
		}
		String given = capitalized(scope.trim());
		if (!given.equals(implied)) {
			throw new VessException("A variable with SCOPE " + given + " takes " + (given.equals("Server") ? "no APPLICATION or CUBE"
					: given.equals("Application") ? "an APPLICATION and no CUBE" : "an APPLICATION and a CUBE"));
		}
		return given;
	}

	private final class Sessions extends VessTable {

		private final List<Column> columns = sessionColumns();

		Sessions() {
			super("SESSIONS", "Who's connected, and to what; deleting a session logs it off");
		}

		@Override
		public List<Column> getColumns() {
			return columns;
		}

		@Override
		public List<String> getPrimaryKey() {
			return List.of("ID");
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
			try {
				return sessionRows(context.getServer().getSessions());
			} catch (EssApiException e) {
				throw new VessException("Could not list the sessions: " + e.getMessage(), e);
			}
		}

		@Override
		public boolean canDelete() {
			return true;
		}

		@Override
		public void delete(List<Object[]> rows) throws SQLException {
			killSessions(context, context.getServer().getSessions(), rows);
		}

	}

	static List<Column> sessionColumns() {
		return List.of(Column.bigint("ID").notNull(), Column.text("USER_NAME", MEMBER_NAME), Column.timestamp("LOGGED_IN"),
				Column.bigint("LOGIN_SECONDS").remarks("How long ago the session logged in"), Column.text("CONNECTION_SOURCE", MEMBER_NAME),
				Column.text("APPLICATION", OBJECT_NAME), Column.text("CUBE", OBJECT_NAME));
	}

	static List<Object[]> sessionRows(List<EssSession> sessions) {
		List<Object[]> rows = new ArrayList<>();
		Instant now = Instant.now();
		for (EssSession session : sessions) {
			long seconds = session.getLoginTimeInSeconds();
			rows.add(new Object[] { session.getSessionId(), session.getUserId(), now.minusSeconds(seconds), seconds,
					session.getConnectionSource(), emptyToNull(session.getApplication()), emptyToNull(session.getDatabase()) });
		}
		return rows;
	}

	static void killSessions(ServerContext context, List<EssSession> sessions, List<Object[]> rows) throws SQLException {
		for (Object[] row : rows) {
			Long id = (Long) row[0];
			context.checkWritable("log off session " + id);
			for (EssSession session : sessions) {
				if (id.equals(session.getSessionId())) {
					try {
						session.kill(true);
					} catch (EssApiException e) {
						throw new VessException("Could not log off session " + id + ": " + e.getMessage(), e);
					}
				}
			}
		}
	}

	static String emptyToNull(String text) {
		return text == null || text.isEmpty() ? null : text;
	}

	private VessTable users() {
		return new ListTable("USERS", "The users the server knows",
				List.of(Column.text("NAME", MEMBER_NAME).notNull(), Column.text("DISPLAY_NAME", MEMBER_NAME), Column.text("EMAIL", MEMBER_NAME)),
				List.of("NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (EssUser user : context.getServer().getUsers()) {
						rows.add(new Object[] { user.getName(), user.getDisplayName(), user.getEmail() });
					}
					return rows;
				});
	}

	private VessTable groups() {
		return new ListTable("GROUPS", "The groups the server knows, and the role each has",
				List.of(Column.text("NAME", MEMBER_NAME).notNull(), Column.text("DESCRIPTION", TEXT), Column.text("ROLE", MEMBER_NAME)),
				List.of("NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (EssGroup group : context.getServer().getGroups()) {
						rows.add(new Object[] { group.getName(), group.getDescription(), group.getRole() });
					}
					return rows;
				});
	}

	private VessTable groupMembers() {
		return new ListTable("GROUP_MEMBERS", "The users and groups in each group",
				List.of(Column.text("GROUP_NAME", MEMBER_NAME).notNull(), Column.text("MEMBER_NAME", MEMBER_NAME).notNull(),
						Column.text("MEMBER_TYPE", 5).notNull().remarks("User or Group")),
				List.of("GROUP_NAME", "MEMBER_NAME", "MEMBER_TYPE"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					Set<String> wanted = conditions.values(0);
					for (EssGroup group : context.getServer().getGroups()) {
						if (wanted != null && wanted.stream().noneMatch(name -> name.equalsIgnoreCase(group.getName()))) {
							continue;
						}
						for (EssUser user : group.getUsers()) {
							rows.add(new Object[] { group.getName(), user.getName(), "User" });
						}
						for (EssGroup member : group.getGroups()) {
							rows.add(new Object[] { group.getName(), member.getName(), "Group" });
						}
					}
					return rows;
				});
	}

	private VessTable permissions() {
		return new ListTable("PERMISSIONS", "Who has which role: on the server, or on an application",
				List.of(Column.text("APPLICATION", OBJECT_NAME).remarks("The application, or null for a role on the whole server"),
						Column.text("NAME", MEMBER_NAME).notNull(), Column.text("ROLE", MEMBER_NAME), Column.bool("IS_GROUP").notNull()),
				List.of(), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					Set<String> wanted = conditions.values(0);
					if (wanted == null) {
						for (EssPermission permission : context.getServer().getPermissions()) {
							rows.add(new Object[] { null, permission.getName(), permission.getRole(), permission.isGroup() });
						}
					}
					for (EssApplication application : applications(wanted)) {
						for (EssPermission permission : application.getPermissions()) {
							rows.add(new Object[] { application.getName(), permission.getName(), permission.getRole(), permission.isGroup() });
						}
					}
					return rows;
				});
	}

	private VessTable jobs() {
		return new ListTable("JOBS", "The server's job history, newest first: calcs, loads, imports and exports",
				List.of(Column.bigint("ID").notNull(), Column.text("APPLICATION", OBJECT_NAME), Column.text("CUBE", OBJECT_NAME),
						Column.text("TYPE", MEMBER_NAME), Column.text("FILE_NAME", TEXT), Column.text("USER_NAME", MEMBER_NAME),
						Column.timestamp("STARTED"), Column.timestamp("ENDED"), Column.text("STATUS", 30), Column.text("MESSAGE", TEXT)),
				List.of("ID"), conditions -> {
					String application = conditions.value(1);
					String cube = conditions.value(2);
					return () -> new JobPages(application, cube);
				});
	}

	/**
	 * The job history, a page at a time, so that a query wanting the latest
	 * few reads only those.
	 */
	private final class JobPages implements Iterator<Object[]> {

		private static final long PAGE = 100;

		private final String application;

		private final String cube;

		private final List<Object[]> page = new ArrayList<>();

		private int position;

		private long offset;

		private boolean more = true;

		JobPages(String application, String cube) {
			this.application = application;
			this.cube = cube;
		}

		@Override
		public boolean hasNext() {
			if (position < page.size()) {
				return true;
			}
			if (!more) {
				return false;
			}
			read();
			return position < page.size();
		}

		@Override
		public Object[] next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return page.get(position++);
		}

		private void read() {
			page.clear();
			position = 0;
			try {
				JobRecordPaginatedResultWrapper jobs = context.api().getJobsApi().jobsGetAllJobRecords(null, application,
						application == null ? null : cube, "job_ID:desc", offset, PAGE, false, null, null, null, null, null);
				for (JobRecordBean job : jobs.getItems()) {
					String status = null;
					if (job.getStatusCode() != null) {
						try {
							status = capitalized(EssJob.Status.fromCode(job.getStatusCode()).name().replace('_', ' '));
						} catch (IllegalArgumentException e) {
							status = String.valueOf(job.getStatusCode());
						}
					}
					page.add(new Object[] { job.getJobID(), emptyToNull(job.getAppName()), emptyToNull(job.getDbName()), job.getJobType(),
							emptyToNull(job.getJobfileName()), job.getUserName(), instant(job.getStartTime()), instant(job.getEndTime()), status,
							emptyToNull(job.getStatusMessage()) });
				}
				offset += jobs.getItems().size();
				more = Boolean.TRUE.equals(jobs.getHasMore()) && !jobs.getItems().isEmpty();
			} catch (ApiException | SQLException | EssApiException e) {
				throw new VessException.Unchecked("Could not read the job history: " + e.getMessage(), e);
			}
		}

	}

	private VessTable log() {
		return new ListTable("LOG", "Each application's latest log, an entry a row; say which APPLICATION to read only its log",
				List.of(Column.text("APPLICATION", OBJECT_NAME).notNull(), Column.timestamp("LOGGED"), Column.text("SEVERITY", 20),
						Column.integer("SEVERITY_LEVEL"), Column.text("MESSAGE_ID", 20), Column.text("COMPONENT", MEMBER_NAME),
						Column.text("MESSAGE", TEXT)),
				List.of(), conditions -> {
					List<EssApplication> applications = applications(conditions.values(0));
					return () -> new Iterator<Object[]>() {

						private final Iterator<EssApplication> remaining = applications.iterator();

						private Iterator<Object[]> current = List.<Object[]>of().iterator();

						@Override
						public boolean hasNext() {
							while (!current.hasNext() && remaining.hasNext()) {
								EssApplication application = remaining.next();
								List<Object[]> rows = new ArrayList<>();
								try {
									for (EssLogEntry entry : application.readLatestLog()) {
										rows.add(new Object[] { application.getName(), entry.getTimestamp(),
												entry.getSeverity() == null ? null : entry.getSeverity().name(), entry.getSeverityLevel(),
												entry.getMessageId(), entry.getComponent(), entry.getMessage() });
									}
								} catch (EssApiException e) {
									throw new VessException.Unchecked("Could not read the log of " + application.getName() + ": " + e.getMessage(), e);
								}
								current = rows.iterator();
							}
							return current.hasNext();
						}

						@Override
						public Object[] next() {
							if (!hasNext()) {
								throw new NoSuchElementException();
							}
							return current.next();
						}

					};
				});
	}

	private VessTable configuration() {
		return new ListTable("CONFIGURATION", "Each application's configuration settings, set or not",
				List.of(Column.text("APPLICATION", OBJECT_NAME).notNull(), Column.text("NAME", MEMBER_NAME).notNull(), Column.text("VALUE", TEXT),
						Column.bool("CONFIGURED").remarks("Whether the application sets it, rather than taking the default"),
						Column.text("DESCRIPTION", TEXT), Column.text("SYNTAX", TEXT)),
				List.of("APPLICATION", "NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (EssApplication application : applications(conditions.values(0))) {
						for (EssApplicationConfiguration setting : application.getConfigurations()) {
							rows.add(new Object[] { application.getName(), setting.getKey(), setting.getValue(), setting.isConfigured(),
									setting.getDescription(), setting.getSyntax() });
						}
					}
					return rows;
				});
	}

	private VessTable connections() {
		return new ListTable("CONNECTIONS", "The server's global connections to files and databases",
				List.of(Column.text("NAME", MEMBER_NAME).notNull(), Column.text("TYPE", 30), Column.text("DESCRIPTION", TEXT),
						Column.text("HOST", MEMBER_NAME), Column.text("PATH", TEXT)),
				List.of("NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (EssConnection connection : context.getServer().getConnections()) {
						rows.add(new Object[] { connection.getName(),
								connection.getConnectionType() == null ? null : connection.getConnectionType().name(), connection.getDescription(),
								connection.getHost(), connection.getPath() });
					}
					return rows;
				});
	}

	private VessTable datasources() {
		return new ListTable("DATASOURCES", "The server's global datasources", List.of(Column.text("NAME", MEMBER_NAME).notNull()), List.of("NAME"),
				conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (EssDataSource datasource : context.getServer().getDataSources()) {
						rows.add(new Object[] { datasource.getName() });
					}
					return rows;
				});
	}

	private VessTable datasourceColumns() {
		return new ListTable("DATASOURCE_COLUMNS", "The columns of each global datasource",
				List.of(Column.text("DATASOURCE", MEMBER_NAME).notNull(), Column.integer("COLUMN_INDEX"), Column.text("NAME", MEMBER_NAME).notNull(),
						Column.text("TYPE", 30)),
				List.of("DATASOURCE", "NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					Set<String> wanted = conditions.values(0);
					for (EssDataSource datasource : context.getServer().getDataSources()) {
						if (wanted != null && wanted.stream().noneMatch(name -> name.equalsIgnoreCase(datasource.getName()))) {
							continue;
						}
						for (EssDataSourceColumn column : context.getServer().getDataSource(datasource.getName()).getColumns()) {
							rows.add(new Object[] { datasource.getName(), column.getIndex(), column.getName(), column.getDataType() });
						}
					}
					return rows;
				});
	}

	private VessTable mdxFunctions() {
		return new ListTable("MDX_FUNCTIONS", "The MDX functions the server knows, with their syntax",
				List.of(Column.text("CATEGORY", MEMBER_NAME), Column.text("NAME", MEMBER_NAME).notNull(), Column.text("SYNTAX", TEXT),
						Column.text("DESCRIPTION", TEXT)),
				List.of(), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (EssMdxFunctionGroup group : context.getServer().getMdxFunctions()) {
						for (EssMdxFunction function : group.getFunctions()) {
							rows.add(new Object[] { group.getName(), function.getName(), function.getSyntax(), function.getComment() });
						}
					}
					return rows;
				});
	}

	private VessTable properties() {
		return new ListTable("PROPERTIES", "What the server says about itself: its version, build and settings",
				List.of(Column.text("SOURCE", 10).notNull().remarks("About or Instance"), Column.text("NAME", MEMBER_NAME).notNull(),
						Column.text("VALUE", TEXT)),
				List.of("SOURCE", "NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					EssServer server = context.getServer();
					flatten("About", "", server.getAboutDetails(), rows);
					flatten("Instance", "", server.getInstanceDetails(), rows);
					return rows;
				});
	}

	private static void flatten(String source, String prefix, Map<String, Object> values, List<Object[]> rows) {
		if (values == null) {
			return;
		}
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			if (entry.getKey().equals("links")) {
				continue;
			}
			String name = prefix + entry.getKey();
			if (entry.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> nested = (Map<String, Object>) entry.getValue();
				flatten(source, name + ".", nested, rows);
			} else {
				rows.add(new Object[] { source, name, entry.getValue() == null ? null : String.valueOf(entry.getValue()) });
			}
		}
	}

}
