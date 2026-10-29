package com.jasonwjones.hyperion.vess.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssApplication;
import com.jasonwjones.hyperion.vess.VessException;

/**
 * The whole server as the SQL engine sees it: a catalog for the server's own
 * tables, and one for each application, holding a schema for each cube.
 */
public final class VessModel {

	private final ServerContext context;

	private final SchemaPlus root;

	private final ServerSchema server;

	private final Map<String, ApplicationSchema> applications = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private final java.util.Set<String> aliases = new java.util.HashSet<>();

	public VessModel(ServerContext context, SchemaPlus root) throws VessException {
		this.context = context;
		this.root = root;
		this.server = new ServerSchema(context, this::refresh);
		root.add(context.getSettings().serverCatalog(), new AbstractSchema() {

			@Override
			protected Map<String, Schema> getSubSchemaMap() {
				return Map.of(ServerSchema.NAME, server);
			}

		});
		// Server.Sessions as well as Essbase.Server.Sessions; an application
		// named Server, added after, takes the name
		root.add(ServerSchema.NAME, server);
		refresh();
	}

	/**
	 * Reads the list of applications again, adding any that are new.
	 */
	public synchronized void refresh() {
		try {
			for (EssApplication application : context.getServer().getApplications()) {
				String name = application.getName();
				if (name.equalsIgnoreCase(context.getSettings().serverCatalog()) || applications.containsKey(name)) {
					continue;
				}
				ApplicationSchema schema = new ApplicationSchema(context, name);
				applications.put(name, schema);
				root.add(name, schema);
			}
		} catch (EssApiException e) {
			throw new VessException.Unchecked("Could not list the applications: " + e.getMessage(), e);
		}
	}

	/**
	 * Adds a top-level name for a nested schema, such as "Sample.Basic", which
	 * the engine can then resolve unqualified names against.
	 */
	public synchronized void addAlias(String alias, Schema target) {
		if (aliases.add(alias)) {
			root.add(alias, target);
		}
	}

	public ServerContext getContext() {
		return context;
	}

	public ServerSchema getServerSchema() {
		return server;
	}

	/**
	 * @return the applications, in order of name
	 */
	public synchronized List<ApplicationSchema> getApplications() {
		return new ArrayList<>(applications.values());
	}

	/**
	 * @return the application of that name, in any case, or null
	 */
	public synchronized ApplicationSchema application(String name) {
		return name == null ? null : applications.get(name);
	}

	/**
	 * @return the schema a catalog and schema name, in any case, are; or null
	 */
	public VessSchema schema(String catalog, String schema) {
		if (catalog == null || schema == null) {
			return null;
		}
		if (catalog.equalsIgnoreCase(context.getSettings().serverCatalog()) && schema.equalsIgnoreCase(ServerSchema.NAME)) {
			return server;
		}
		ApplicationSchema application = application(catalog);
		return application == null ? null : application.cube(schema);
	}

	/**
	 * Finds the table a statement names, by up to three parts: catalog,
	 * schema and table. Fewer parts are taken from the default catalog and
	 * schema.
	 *
	 * @throws VessException if there's no such table
	 */
	public VessTable table(List<String> names, String defaultCatalog, String defaultSchema) throws VessException {
		String catalog;
		String schema;
		String table = names.get(names.size() - 1);
		if (names.size() >= 3) {
			catalog = names.get(names.size() - 3);
			schema = names.get(names.size() - 2);
		} else if (names.size() == 2) {
			// Server.Table, Cube.Table within the default catalog, or
			// "App.Cube".Table, as the engine resolves them
			catalog = defaultCatalog;
			schema = names.get(0);
			if (schema.contains(".")) {
				catalog = schema.substring(0, schema.indexOf('.'));
				schema = schema.substring(schema.indexOf('.') + 1);
			}
		} else {
			catalog = defaultCatalog;
			schema = defaultSchema;
		}
		if (ServerSchema.NAME.equalsIgnoreCase(schema) && schema(catalog, schema) == null) {
			catalog = context.getSettings().serverCatalog();
		}
		VessSchema found = schema(catalog, schema);
		if (found == null) {
			throw new VessException("There's no table " + String.join(".", names) + (names.size() < 3
					? "; name it in full, as Application.Cube.Table, or choose a cube to work in first" : ""));
		}
		for (VessTable each : found.getVessTables()) {
			if (each.getName().equalsIgnoreCase(table)) {
				return each;
			}
		}
		throw new VessException("There's no table " + table + " in " + catalog + "." + schema);
	}

}
