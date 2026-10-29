package com.jasonwjones.hyperion.vess.schema;

import static com.jasonwjones.hyperion.vess.schema.Column.MEMBER_NAME;
import static com.jasonwjones.hyperion.vess.schema.Column.TEXT;
import static com.jasonwjones.hyperion.vess.schema.Column.VARIABLE_NAME;
import static com.jasonwjones.hyperion.vess.schema.Column.VARIABLE_VALUE;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssCube;
import com.appliedolap.essbase.EssDrillthrough;
import com.appliedolap.essbase.EssEffectiveVariable;
import com.appliedolap.essbase.EssLock;
import com.appliedolap.essbase.EssScenario;
import com.appliedolap.essbase.EssScript;
import com.appliedolap.essbase.EssVariable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.cells.CellSource;
import com.jasonwjones.hyperion.vess.cells.CellsTable;
import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.outline.Member;
import com.jasonwjones.hyperion.vess.outline.Outline;

/**
 * A cube as a schema: its data, its outline, a table for each dimension, its
 * substitution variables and scripts, and its calc scripts to CALL.
 */
public final class CubeSchema extends AbstractSchema implements VessSchema {

	/** The cube's own tables, whose names a dimension's table gives way to */
	static final Set<String> TABLES = Set.of("CELLS", "FACTS", "DIMENSIONS", "MEMBERS", "GENERATIONS", "LEVELS", "ALIAS_TABLES", "ALIASES",
			"UDAS", "VARIABLES", "SCRIPTS", "DRILLTHROUGHS", "LOCKS", "SESSIONS", "SCENARIOS");

	private final ServerContext context;

	private final String application;

	private final String cube;

	private List<VessTable> tables;

	private List<VessTable> fixedTables;

	private Map<String, String> dimensionTableNames;

	private final List<OutlineFunction> functions;

	public CubeSchema(ServerContext context, String application, String cube) {
		this.context = context;
		this.application = application;
		this.cube = cube;
		String key = context.getId() + application + "/" + cube;
		OutlineFunctions.register(key, () -> context.outline(application, cube), context.getSettings().aliasTable());
		this.functions = OutlineFunction.forCube(key);
	}

	@Override
	public String getCatalogName() {
		return application;
	}

	@Override
	public String getSchemaName() {
		return cube;
	}

	@Override
	public String getRemarks() {
		return "The " + application + "." + cube + " cube";
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
	protected Multimap<String, Function> getFunctionMultimap() {
		ImmutableMultimap.Builder<String, Function> builder = ImmutableMultimap.builder();
		for (OutlineFunction function : functions) {
			builder.put(function.getName(), function);
		}
		return builder.build();
	}

	@Override
	public List<OutlineFunction> getOutlineFunctions() {
		return functions;
	}

	/**
	 * The name of a dimension's table: DIM_PRODUCT for Product, by default, or
	 * as the connection's naming says, with a suffix where that would take the
	 * name of one of the cube's own tables or another dimension's.
	 */
	synchronized String dimensionTable(Dimension dimension) {
		if (dimensionTableNames == null) {
			Outline outline;
			try {
				outline = context.outline(application, cube);
			} catch (VessException e) {
				throw new VessException.Unchecked(e);
			}
			dimensionTableNames = dimensionTables(outline, context.getSettings().naming());
		}
		String name = dimensionTableNames.get(dimension.getName());
		return name != null ? name : context.getSettings().naming().dimensionTable(dimension.getName());
	}

	/**
	 * The names of every dimension's table, in outline order, so that which
	 * one gets a suffix doesn't depend on which is asked for first.
	 */
	public static Map<String, String> dimensionTables(Outline outline, Naming naming) {
		Set<String> taken = new HashSet<>(TABLES);
		Map<String, String> names = new LinkedHashMap<>();
		for (Dimension dimension : outline.getDimensions()) {
			names.put(dimension.getName(), naming.unique(taken, naming.dimensionPrefix() + dimension.getName(), "Dimension"));
		}
		return names;
	}

	@Override
	public synchronized List<VessTable> getVessTables() {
		if (tables == null) {
			Outline outline;
			try {
				outline = context.outline(application, cube);
			} catch (VessException e) {
				throw new VessException.Unchecked(e);
			}
			List<VessTable> list = new ArrayList<>();
			CellSource cells = CellSource.of(context, application, cube);
			Naming naming = context.getSettings().naming();
			list.add(new CellsTable("CELLS", cells, application, cube, false, this::dimensionTable, naming));
			list.add(new CellsTable("FACTS", cells, application, cube, true, this::dimensionTable, naming));
			list.addAll(fixedTables());
			for (Dimension dimension : outline.getDimensions()) {
				list.add(new DimensionTable(dimensionTable(dimension), () -> context.outline(application, cube), application, cube, dimension,
						this::dimensionTable, naming, context.getSettings().aliasTable()));
			}
			tables = list;
		}
		return tables;
	}

	/**
	 * The tables whose columns don't depend on the outline, which can be
	 * described without reading it.
	 */
	private synchronized List<VessTable> fixedTables() {
		if (fixedTables == null) {
			fixedTables = List.of(dimensions(), members(), generations(false), generations(true), aliasTables(), aliases(), udas(),
					new Variables(), new Scripts(), drillthroughs(), new Locks(), new Sessions(), scenarios());
		}
		return fixedTables;
	}

	/**
	 * Every table, unless the application is stopped and its outline isn't at
	 * hand: then only those that can be described without reading the
	 * outline, which would start the application. A query of any of them
	 * still works, starting it.
	 */
	@Override
	public List<VessTable> getBrowsableTables() {
		if (context.getSettings().browseStopped() || context.hasOutline(application, cube) || context.isStarted(application)) {
			return getVessTables();
		}
		return fixedTables();
	}

	private Outline outline() throws SQLException {
		return context.outline(application, cube);
	}

	private EssCube essCube() throws SQLException {
		return context.cube(application, cube);
	}

	private VessTable dimensions() {
		return new ListTable("DIMENSIONS", "The cube's dimensions, attribute dimensions included, in outline order",
				List.of(Column.text("NAME", MEMBER_NAME).notNull(), Column.integer("NUMBER").notNull(),
						Column.text("TYPE", 20).remarks("Regular, Accounts, Time, Attribute, Country or Currency Partition"),
						Column.text("DENSITY", 6).remarks("Dense or Sparse"), Column.integer("MEMBERS").remarks("Members, shared ones included"),
						Column.integer("STORED_MEMBERS").remarks("Members that hold data of their own"), Column.integer("GENERATIONS"),
						Column.integer("LEVELS"), Column.text("ATTRIBUTE_TYPE", 10).remarks("For an attribute dimension: Text, Numeric, Boolean or Date"),
						Column.text("BASE_DIMENSION", MEMBER_NAME).remarks("For an attribute dimension: the dimension it describes"),
						Column.text("ATTRIBUTE_DIMENSIONS", TEXT).remarks("The attribute dimensions that describe this one")),
				List.of("NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (Dimension dimension : outline().getDimensions()) {
						int stored = 0;
						for (Member member : dimension.getMembers()) {
							if (member.isStored()) {
								stored++;
							}
						}
						rows.add(new Object[] { dimension.getName(), dimension.getNumber(), dimension.getType(),
								dimension.isAttribute() ? null : dimension.getDensity(), dimension.getMembers().size(), stored,
								dimension.getGenerations(), dimension.getLevels(), dimension.getAttributeType(), dimension.getBaseDimension(),
								dimension.getAttributeDimensions().isEmpty() ? null : String.join(", ", dimension.getAttributeDimensions()) });
					}
					return rows;
				});
	}

	private VessTable members() {
		return new ListTable("MEMBERS", "Every member of every dimension, where it sits in the outline, shared members included",
				List.of(Column.integer("OUTLINE_ORDER").notNull().remarks("Sort by it for the outline's order"),
						Column.text("DIMENSION", MEMBER_NAME).notNull(), Column.text("NAME", MEMBER_NAME).notNull(),
						Column.text("ALIAS", MEMBER_NAME), Column.text("PARENT", MEMBER_NAME), Column.integer("LEVEL").notNull(),
						Column.integer("GENERATION").notNull(), Column.text("STORAGE", 25), Column.text("CONSOLIDATION", 1),
						Column.bool("SHARED").notNull(), Column.text("BASE_MEMBER", MEMBER_NAME).remarks("For a shared member, the member it shares"),
						Column.text("FORMULA", TEXT), Column.text("TIME_BALANCE", 10),
						Column.text("VARIANCE_REPORTING", 10), Column.bool("TWO_PASS"), Column.integer("CHILDREN").notNull()),
				List.of("OUTLINE_ORDER"), conditions -> {
					Outline outline = outline();
					Set<String> wanted = conditions.values(1);
					String aliasTable = outline.aliasTable(context.getSettings().aliasTable());
					List<Object[]> rows = new ArrayList<>();
					for (Dimension dimension : outline.getDimensions()) {
						if (wanted != null && !wanted.contains(dimension.getName())) {
							continue;
						}
						for (Member member : dimension.getMembers()) {
							rows.add(new Object[] { member.getOrder(), dimension.getName(), member.getName(), member.getAlias(aliasTable),
									member.getParent() == null ? null : member.getParent().getName(), member.getLevel(), member.getGeneration(),
									member.getStorage(), member.getConsolidation(), member.isShared(), member.getBaseMember(), member.getFormula(),
									member.getTimeBalance(), member.getVarianceReporting(), member.isTwoPass(), member.getChildren().size() });
						}
					}
					return rows;
				});
	}

	private VessTable generations(boolean levels) {
		String what = levels ? "level" : "generation";
		return new ListTable(levels ? "LEVELS" : "GENERATIONS", "Each dimension's " + what + "s, with their names and how many members each has",
				List.of(Column.text("DIMENSION", MEMBER_NAME).notNull(), Column.integer("NUMBER").notNull(),
						Column.text("NAME", MEMBER_NAME).remarks("The name given to the " + what + ", if any"), Column.integer("MEMBERS").notNull()),
				List.of("DIMENSION", "NUMBER"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (Dimension dimension : outline().getDimensions()) {
						Map<Integer, Integer> counts = new java.util.TreeMap<>();
						for (Member member : dimension.getMembers()) {
							if (!member.isShared()) {
								counts.merge(levels ? member.getLevel() : member.getGeneration(), 1, Integer::sum);
							}
						}
						for (Map.Entry<Integer, Integer> count : counts.entrySet()) {
							int number = count.getKey();
							rows.add(new Object[] { dimension.getName(), number,
									levels ? dimension.getLevelName(number) : dimension.getGenerationName(number), count.getValue() });
						}
					}
					return rows;
				});
	}

	private VessTable aliasTables() {
		return new ListTable("ALIAS_TABLES", "The cube's alias tables",
				List.of(Column.text("NAME", MEMBER_NAME).notNull(), Column.bool("IS_DEFAULT").notNull()), List.of("NAME"), conditions -> {
					Outline outline = outline();
					List<Object[]> rows = new ArrayList<>();
					for (String table : outline.getAliasTables()) {
						rows.add(new Object[] { table, table.equals(outline.getDefaultAliasTable()) });
					}
					return rows;
				});
	}

	private VessTable aliases() {
		return new ListTable("ALIASES", "Every member's alias in every alias table, a row each",
				List.of(Column.text("DIMENSION", MEMBER_NAME).notNull(), Column.text("MEMBER", MEMBER_NAME).notNull(),
						Column.text("ALIAS_TABLE", MEMBER_NAME).notNull(), Column.text("ALIAS", MEMBER_NAME).notNull()),
				List.of("MEMBER", "ALIAS_TABLE"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (Dimension dimension : outline().getDimensions()) {
						for (Member member : dimension.getBaseMembers()) {
							for (Map.Entry<String, String> alias : member.getAliases().entrySet()) {
								rows.add(new Object[] { dimension.getName(), member.getName(), alias.getKey(), alias.getValue() });
							}
						}
					}
					return rows;
				});
	}

	private VessTable udas() {
		return new ListTable("UDAS", "Each member's user-defined attributes, a row each",
				List.of(Column.text("DIMENSION", MEMBER_NAME).notNull(), Column.text("MEMBER", MEMBER_NAME).notNull(),
						Column.text("UDA", MEMBER_NAME).notNull()),
				List.of("MEMBER", "UDA"), conditions -> {
					Set<String> wanted = conditions.values(0);
					List<Object[]> rows = new ArrayList<>();
					for (Dimension dimension : outline().getDimensions()) {
						if (wanted != null && !wanted.contains(dimension.getName())) {
							continue;
						}
						for (Member member : dimension.getBaseMembers()) {
							for (String uda : member.getUdas()) {
								rows.add(new Object[] { dimension.getName(), member.getName(), uda });
							}
						}
					}
					return rows;
				});
	}

	/**
	 * The variables that apply to the cube: its own, its application's and the
	 * server's, with those a narrower one hides marked as not in effect.
	 */
	private final class Variables extends VessTable {

		private final List<Column> columns = List.of(Column.text("SCOPE", 11).notNull().remarks("Cube, Application or Server"),
				Column.text("NAME", VARIABLE_NAME).notNull(), Column.text("VALUE", VARIABLE_VALUE).notNull().writable(),
				Column.bool("IN_EFFECT").notNull().remarks("False when a variable of the same name at a narrower scope hides it"));

		Variables() {
			super("VARIABLES", "The substitution variables this cube sees, at every scope; insert, update and delete them here");
		}

		@Override
		public List<Column> getColumns() {
			return columns;
		}

		@Override
		public List<String> getPrimaryKey() {
			return List.of("SCOPE", "NAME");
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
			List<Object[]> rows = new ArrayList<>();
			try {
				for (EssEffectiveVariable variable : essCube().getEffectiveVariables()) {
					rows.add(new Object[] { variable.getScope().getLabel(), variable.getName(), variable.getValue(), true });
					for (EssVariable shadowed : variable.getShadowed()) {
						rows.add(new Object[] { shadowed.getScope().getLabel(), shadowed.getName(), shadowed.getValue(), false });
					}
				}
			} catch (EssApiException e) {
				throw new VessException("Could not read the variables of " + application + "." + cube + ": " + e.getMessage(), e);
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
				String scope = row[0] == null ? "Cube" : ServerSchema.capitalized(row[0].toString().trim());
				String name = (String) row[1];
				String value = (String) row[2];
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
					case "Cube":
						essCube().createVariable(name, value);
						break;
					default:
						throw new VessException("SCOPE can be Cube, Application or Server, not '" + row[0] + "'");
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
				EssVariable variable = find(before.get(index));
				context.checkWritable("change the variable " + variable.getName());
				try {
					variable.setValue((String) after.get(index)[2]);
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
			String name = (String) row[1];
			List<? extends EssVariable> variables;
			switch (scope) {
			case "Server":
				variables = context.getServer().getVariables();
				break;
			case "Application":
				variables = context.application(application).getVariables();
				break;
			default:
				variables = essCube().getVariables();
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
	 * The cube's calc and MDX scripts, whose text can be changed here.
	 */
	private final class Scripts extends VessTable {

		private final List<Column> columns = List.of(Column.text("TYPE", 4).notNull().remarks("Calc or MDX"),
				Column.text("NAME", MEMBER_NAME).notNull(), Column.text("CONTENT", TEXT).writable().remarks("The script; change it to save it"),
				Column.bigint("SIZE"), Column.timestamp("MODIFIED"));

		Scripts() {
			super("SCRIPTS", "The cube's calc and MDX scripts; insert, update CONTENT and delete them here, and CALL a calc script to run it");
		}

		@Override
		public List<Column> getColumns() {
			return columns;
		}

		@Override
		public List<String> getPrimaryKey() {
			return List.of("TYPE", "NAME");
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
			List<Object[]> rows = new ArrayList<>();
			String type = conditions.value(0);
			Set<String> names = conditions.values(1);
			boolean content = conditions.wants(2);
			try {
				for (EssScript.ScriptType scriptType : EssScript.ScriptType.values()) {
					if (type != null && !type.equalsIgnoreCase(label(scriptType))) {
						continue;
					}
					for (EssScript script : essCube().getScripts(scriptType)) {
						if (names != null && !names.contains(script.getName())) {
							continue;
						}
						Long modified = script.getModifiedTime();
						rows.add(new Object[] { label(scriptType), script.getName(), content ? script.getContent() : null, script.getSize(),
								modified == null ? null : Instant.ofEpochMilli(modified) });
					}
				}
			} catch (EssApiException e) {
				throw new VessException("Could not read the scripts of " + application + "." + cube + ": " + e.getMessage(), e);
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
				EssScript.ScriptType type = type(row[0]);
				String name = (String) row[1];
				if (name == null) {
					throw new VessException("A script needs a NAME");
				}
				context.checkWritable("create the script " + name);
				try {
					essCube().createScript(type, name, row[2] == null ? "" : row[2].toString());
				} catch (EssApiException e) {
					throw new VessException("Could not create the script " + name + ": " + e.getMessage(), e);
				}
			}
		}

		@Override
		public void update(List<Object[]> before, List<Object[]> after) throws SQLException {
			for (int index = 0; index < before.size(); index++) {
				List<String> changed = changedReadOnly(before.get(index), after.get(index));
				if (!changed.isEmpty()) {
					throw new VessException("Of a script, only CONTENT can be changed, not " + String.join(", ", changed));
				}
			}
			for (int index = 0; index < before.size(); index++) {
				EssScript script = find(before.get(index));
				context.checkWritable("save the script " + script.getName());
				try {
					Object content = after.get(index)[2];
					script.save(content == null ? "" : content.toString());
				} catch (EssApiException e) {
					throw new VessException("Could not save the script " + script.getName() + ": " + e.getMessage(), e);
				}
			}
		}

		@Override
		public void delete(List<Object[]> rows) throws SQLException {
			for (Object[] row : rows) {
				EssScript script = find(row);
				context.checkWritable("delete the script " + script.getName());
				try {
					script.delete();
				} catch (EssApiException e) {
					throw new VessException("Could not delete the script " + script.getName() + ": " + e.getMessage(), e);
				}
			}
		}

		private EssScript find(Object[] row) throws SQLException {
			EssScript.ScriptType type = type(row[0]);
			for (EssScript script : essCube().getScripts(type)) {
				if (script.getName().equalsIgnoreCase((String) row[1])) {
					return script;
				}
			}
			throw new VessException("There's no longer a script " + row[1]);
		}

	}

	static String label(EssScript.ScriptType type) {
		return type == EssScript.ScriptType.MDX ? "MDX" : "Calc";
	}

	static EssScript.ScriptType type(Object label) throws VessException {
		String text = label == null ? "Calc" : label.toString().trim();
		if (text.equalsIgnoreCase("Calc") || text.equalsIgnoreCase("csc")) {
			return EssScript.ScriptType.CALC;
		}
		if (text.equalsIgnoreCase("MDX")) {
			return EssScript.ScriptType.MDX;
		}
		throw new VessException("A script's TYPE is Calc or MDX, not '" + label + "'");
	}

	private VessTable drillthroughs() {
		return new ListTable("DRILLTHROUGHS", "The cube's drill-through definitions",
				List.of(Column.text("NAME", MEMBER_NAME).notNull(), Column.text("TYPE", 20), Column.text("URL", TEXT),
						Column.text("REGIONS", TEXT).remarks("The drillable regions, one to a line"), Column.text("DATASOURCE", MEMBER_NAME),
						Column.bool("USE_TEMP_TABLES")),
				List.of("NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					for (EssDrillthrough drillthrough : essCube().getDrillthroughs()) {
						rows.add(new Object[] { drillthrough.getName(),
								drillthrough.getDrillthroughType() == null ? null : drillthrough.getDrillthroughType().toString(),
								drillthrough.getUrl(),
								drillthrough.getDrillableRegions() == null ? null : String.join("\n", drillthrough.getDrillableRegions()),
								drillthrough.getDataSourceName(), drillthrough.isUseTempTables() });
					}
					return rows;
				});
	}

	/**
	 * Objects of the cube that someone has locked; deleting one unlocks it.
	 */
	private final class Locks extends VessTable {

		private final List<Column> columns = List.of(Column.text("NAME", MEMBER_NAME).notNull(), Column.text("TYPE", 30).notNull(),
				Column.text("USER_NAME", MEMBER_NAME), Column.timestamp("LOCKED"));

		Locks() {
			super("LOCKS", "The cube's locked objects; deleting a lock unlocks the object");
		}

		@Override
		public List<Column> getColumns() {
			return columns;
		}

		@Override
		public List<String> getPrimaryKey() {
			return List.of("NAME", "TYPE");
		}

		@Override
		protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
			List<Object[]> rows = new ArrayList<>();
			for (EssLock lock : locks()) {
				rows.add(new Object[] { lock.getName(), String.valueOf(lock.getLockObjectType()), lock.getUser(),
						lock.getTime() == null ? null : Instant.ofEpochMilli(lock.getTime()) });
			}
			return rows;
		}

		private List<EssLock> locks() throws SQLException {
			try {
				return essCube().getLockedObjects(0, 10_000);
			} catch (EssApiException e) {
				throw new VessException("Could not read the locks of " + application + "." + cube + ": " + e.getMessage(), e);
			}
		}

		@Override
		public boolean canDelete() {
			return true;
		}

		@Override
		public void delete(List<Object[]> rows) throws SQLException {
			List<EssLock> locks = locks();
			for (Object[] row : rows) {
				context.checkWritable("unlock " + row[0]);
				for (EssLock lock : locks) {
					if (lock.getName().equals(row[0]) && String.valueOf(lock.getLockObjectType()).equals(row[1])) {
						try {
							essCube().unlockObject(lock);
						} catch (EssApiException e) {
							throw new VessException("Could not unlock " + row[0] + ": " + e.getMessage(), e);
						}
					}
				}
			}
		}

	}

	private final class Sessions extends VessTable {

		private final List<Column> columns = ServerSchema.sessionColumns();

		Sessions() {
			super("SESSIONS", "Who's connected to this cube; deleting a session logs it off");
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
				return ServerSchema.sessionRows(essCube().getSessions());
			} catch (EssApiException e) {
				throw new VessException("Could not list the sessions of " + application + "." + cube + ": " + e.getMessage(), e);
			}
		}

		@Override
		public boolean canDelete() {
			return true;
		}

		@Override
		public void delete(List<Object[]> rows) throws SQLException {
			ServerSchema.killSessions(context, essCube().getSessions(), rows);
		}

	}

	private VessTable scenarios() {
		return new ListTable("SCENARIOS", "The cube's sandboxed scenarios, when it has scenarios enabled",
				List.of(Column.text("NAME", MEMBER_NAME).notNull()), List.of("NAME"), conditions -> {
					List<Object[]> rows = new ArrayList<>();
					EssCube essCube = essCube();
					if (essCube.isScenariosEnabled()) {
						for (EssScenario scenario : essCube.getScenarios()) {
							rows.add(new Object[] { scenario.getName() });
						}
					}
					return rows;
				});
	}

	@Override
	public List<Procedure> getProcedures() {
		List<Procedure> procedures = new ArrayList<>();
		procedures.add(new Procedure("RUN_CALC", "Runs calc script text against the cube: CALL " + call("RUN_CALC") + "('CALC ALL;')",
				List.of(Column.text("SCRIPT", TEXT)), true, arguments -> {
					String script = ServerSchema.argument(arguments, 0, "the calc script to run");
					context.checkWritable("run a calc script on " + application + "." + cube);
					long started = System.currentTimeMillis();
					try {
						essCube().calculate(script);
					} catch (EssApiException e) {
						throw new VessException(e.getMessage(), e);
					}
					return ServerSchema.result("Calculated " + application + "." + cube + " in " + seconds(started));
				}));
		procedures.add(new Procedure("DEFAULT_CALC", "Runs the cube's default calculation: CALL " + call("DEFAULT_CALC") + "()", List.of(),
				true, arguments -> {
					context.checkWritable("calculate " + application + "." + cube);
					long started = System.currentTimeMillis();
					try {
						essCube().calculate();
					} catch (EssApiException e) {
						throw new VessException(e.getMessage(), e);
					}
					return ServerSchema.result("Calculated " + application + "." + cube + " in " + seconds(started));
				}));
		try {
			for (EssScript script : essCube().getScripts(EssScript.ScriptType.CALC)) {
				String name = script.getName();
				String remarks = "Runs the calc script " + name + ": CALL " + call(name) + "()" + preview(script);
				procedures.add(new Procedure(name, remarks, List.of(), true, arguments -> {
					context.checkWritable("run the calc script " + name);
					long started = System.currentTimeMillis();
					try {
						findScript(EssScript.ScriptType.CALC, name).execute();
					} catch (EssApiException e) {
						throw new VessException(e.getMessage(), e);
					}
					return ServerSchema.result("Ran " + name + " in " + seconds(started));
				}));
			}
			for (EssScript script : essCube().getScripts(EssScript.ScriptType.MDX)) {
				String name = script.getName();
				procedures.add(new Procedure(name, "Runs the MDX script " + name + " and answers with its result: CALL " + call(name) + "()"
						+ preview(script), List.of(), false,
						arguments -> Procedure.Result.of(context.mdx(findScript(EssScript.ScriptType.MDX, name).getContent()))));
			}
		} catch (SQLException | EssApiException e) {
			// The scripts can't be listed, so there are only the built-in procedures
		}
		return procedures;
	}

	/**
	 * How to name one of the cube's procedures in a CALL, quoting what needs
	 * it.
	 */
	private String call(String procedure) {
		return quoted(application) + "." + quoted(cube) + "." + quoted(procedure);
	}

	static String quoted(String name) {
		return name.matches("[A-Za-z_][A-Za-z0-9_]*") ? name : '"' + name.replace("\"", "\"\"") + '"';
	}

	/**
	 * The start of a script, on one line, for someone deciding whether to run
	 * it; or nothing if it can't be read.
	 */
	private static String preview(EssScript script) {
		try {
			String content = script.getContent();
			if (content == null || content.isBlank()) {
				return "";
			}
			String line = content.replaceAll("\\s+", " ").strip();
			return ". It reads: " + (line.length() > 200 ? line.substring(0, 200) + "…" : line);
		} catch (EssApiException e) {
			return "";
		}
	}

	private EssScript findScript(EssScript.ScriptType type, String name) throws SQLException {
		try {
			for (EssScript script : essCube().getScripts(type)) {
				if (script.getName().equalsIgnoreCase(name)) {
					return script;
				}
			}
		} catch (EssApiException e) {
			throw new VessException("Could not list the scripts of " + application + "." + cube + ": " + e.getMessage(), e);
		}
		throw new VessException("There's no " + label(type) + " script named " + name + " in " + application + "." + cube);
	}

	private static String seconds(long started) {
		return String.format(Locale.ROOT, "%.1f s", (System.currentTimeMillis() - started) / 1000.0);
	}

	@Override
	public String toString() {
		return application + "." + cube;
	}

}
