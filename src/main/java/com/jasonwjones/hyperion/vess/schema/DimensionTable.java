package com.jasonwjones.hyperion.vess.schema;

import static com.jasonwjones.hyperion.vess.schema.Column.MEMBER_NAME;
import static com.jasonwjones.hyperion.vess.schema.Column.TEXT;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.outline.Member;
import com.jasonwjones.hyperion.vess.outline.Outline;

/**
 * A dimension as a table: a row for each member, as a star schema would have
 * it. Besides each member's name, aliases, parent, level and generation, it has
 * a column for each generation, holding the member's ancestor there, so that
 * values can be grouped by Category or by Region; and a column for each
 * attribute dimension that describes it, holding the member's attribute with
 * its proper type. Shared members are in the Members table rather than here,
 * so each name is one row.
 */
public final class DimensionTable extends VessTable {

	private static final List<DateTimeFormatter> DATE_FORMATS = List.of(DateTimeFormatter.ofPattern("MM-dd-yyyy"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("MM/dd/yyyy"));

	private final OutlineSource source;

	private final String application;

	private final String cube;

	private final String dimensionName;

	private final Function<Dimension, String> dimensionTables;

	private final Naming naming;

	private List<Column> columns;

	/** What each column holds, in order */
	private List<Function<Member, Object>> values;

	private List<ForeignKey> foreignKeys;

	public DimensionTable(String name, OutlineSource source, String application, String cube, Dimension dimension,
			Function<Dimension, String> dimensionTables, Naming naming) {
		super(name, remarks(dimension));
		this.source = source;
		this.application = application;
		this.cube = cube;
		this.dimensionName = dimension.getName();
		this.dimensionTables = dimensionTables;
		this.naming = naming;
	}

	private static String remarks(Dimension dimension) {
		if (dimension.isAttribute()) {
			return "The " + dimension.getName() + " attribute dimension (" + dimension.getAttributeType() + "), a row for each member";
		}
		return "The " + dimension.getName() + " dimension (" + dimension.getType() + ", " + dimension.getDensity().toLowerCase(Locale.ROOT)
				+ "), a row for each member";
	}

	private Dimension dimension() throws SQLException {
		Dimension dimension = source.outline().getDimension(dimensionName);
		if (dimension == null) {
			throw new VessException("The dimension " + dimensionName + " is no longer in " + application + "." + cube);
		}
		return dimension;
	}

	@Override
	public synchronized List<Column> getColumns() {
		if (columns == null) {
			try {
				build(source.outline(), dimension());
			} catch (SQLException e) {
				throw new VessException.Unchecked(e);
			}
		}
		return columns;
	}

	private void build(Outline outline, Dimension dimension) {
		List<Column> list = new ArrayList<>();
		List<Function<Member, Object>> getters = new ArrayList<>();
		List<ForeignKey> keys = new ArrayList<>();
		Set<String> taken = new HashSet<>();

		add(list, getters, taken, Column.text("NAME", MEMBER_NAME).notNull(), Member::getName);
		String defaultTable = outline.getDefaultAliasTable();
		add(list, getters, taken, Column.text("ALIAS", MEMBER_NAME).remarks("The alias in the " + defaultTable + " alias table"),
				member -> member.getAlias(defaultTable));
		add(list, getters, taken, Column.text("PARENT", MEMBER_NAME), member -> member.getParent() == null ? null : member.getParent().getName());
		add(list, getters, taken, Column.integer("LEVEL").notNull().remarks("0 for a member without children"), Member::getLevel);
		add(list, getters, taken, Column.integer("GENERATION").notNull().remarks("1 for the dimension itself"), Member::getGeneration);
		add(list, getters, taken, Column.text("STORAGE", 25).remarks("Store, Dynamic Calc, Label Only, Never Share or Dynamic Calc and Store"),
				Member::getStorage);
		add(list, getters, taken, Column.text("CONSOLIDATION", 1).remarks("How it adds into its parent: +, -, *, /, %, ~ or ^"),
				Member::getConsolidation);
		add(list, getters, taken, Column.text("FORMULA", TEXT), Member::getFormula);
		add(list, getters, taken, Column.text("UDAS", TEXT).remarks("Its user-defined attributes, separated by commas"),
				member -> member.getUdas().isEmpty() ? null : String.join(", ", member.getUdas()));
		if (dimension.getType().equals(Dimension.ACCOUNTS)) {
			add(list, getters, taken, Column.text("TIME_BALANCE", 10).remarks("First, Last or Average"), Member::getTimeBalance);
			add(list, getters, taken, Column.text("VARIANCE_REPORTING", 10).remarks("Expense, for an expense"), Member::getVarianceReporting);
			add(list, getters, taken, Column.bool("TWO_PASS"), Member::isTwoPass);
		}
		add(list, getters, taken, Column.integer("CHILDREN").notNull(), member -> member.getChildren().size());
		add(list, getters, taken, Column.integer("OUTLINE_ORDER").notNull().remarks("Sort by it for the outline's order"), Member::getOrder);

		// The ancestor at each generation below the dimension itself
		int generations = dimension.getGenerations();
		for (int generation = 2; generation <= generations; generation++) {
			String named = dimension.getGenerationName(generation);
			String columnName = naming.unique(taken, named != null ? named : "GEN" + generation, "Generation");
			int number = generation;
			add(list, getters, taken, Column.text(columnName, MEMBER_NAME).remarks("The member's ancestor at generation " + generation
					+ (named != null ? " (" + named + ")" : "") + ", or the member itself if it's there"), member -> {
						Member ancestor = member.getAncestor(number);
						return ancestor == null ? null : ancestor.getName();
					});
		}

		// The aliases in each other table
		for (String table : outline.getAliasTables()) {
			if (table.equals(defaultTable)) {
				continue;
			}
			String columnName = naming.unique(taken, table, "Alias");
			add(list, getters, taken, Column.text(columnName, MEMBER_NAME).remarks("The alias in the " + table + " alias table"),
					member -> member.getAlias(table));
		}

		// Each attribute that describes the members, with its own type
		for (String attributeName : dimension.getAttributeDimensions()) {
			Dimension attribute = outline.getDimension(attributeName);
			if (attribute == null) {
				continue;
			}
			String columnName = naming.unique(taken, attributeName, "Attribute");
			Column column;
			Function<String, Object> convert;
			switch (String.valueOf(attribute.getAttributeType())) {
			case "Boolean":
				column = Column.bool(columnName);
				convert = text -> text.equalsIgnoreCase("True") || text.equalsIgnoreCase("Yes");
				break;
			case "Numeric":
				column = Column.number(columnName);
				convert = DimensionTable::number;
				break;
			case "Date":
				column = Column.date(columnName);
				convert = DimensionTable::date;
				break;
			default:
				column = Column.text(columnName, MEMBER_NAME);
				convert = text -> text;
				break;
			}
			add(list, getters, taken, column.remarks("The member's " + attributeName + " attribute"), member -> {
				String value = member.getAttributes().get(attributeName);
				return value == null ? null : convert.apply(value);
			});
			keys.add(ForeignKey.of(getName() + "_" + attributeName, columnName, application, cube, dimensionTables.apply(attribute), "NAME"));
		}
		keys.add(0, ForeignKey.of(getName() + "_PARENT", "PARENT", application, cube, getName(), "NAME"));
		this.columns = list;
		this.values = getters;
		this.foreignKeys = keys;
	}

	private static void add(List<Column> list, List<Function<Member, Object>> getters, Set<String> taken, Column column,
			Function<Member, Object> getter) {
		taken.add(column.getName().toUpperCase(Locale.ROOT));
		list.add(column);
		getters.add(getter);
	}

	static Double number(String text) {
		try {
			return Double.valueOf(text.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static LocalDate date(String text) {
		for (DateTimeFormatter format : DATE_FORMATS) {
			try {
				return LocalDate.parse(text.trim(), format);
			} catch (DateTimeParseException e) {
				// The next format, then
			}
		}
		return null;
	}

	@Override
	public List<String> getPrimaryKey() {
		return List.of("NAME");
	}

	@Override
	public synchronized List<ForeignKey> getForeignKeys() {
		getColumns();
		return foreignKeys;
	}

	@Override
	protected Iterable<Object[]> rows(Conditions conditions) throws SQLException {
		getColumns();
		List<Function<Member, Object>> getters;
		synchronized (this) {
			getters = values;
		}
		List<Object[]> rows = new ArrayList<>();
		for (Member member : dimension().getBaseMembers()) {
			Object[] row = new Object[getters.size()];
			for (int index = 0; index < row.length; index++) {
				row[index] = getters.get(index).apply(member);
			}
			rows.add(row);
		}
		return rows;
	}

}
