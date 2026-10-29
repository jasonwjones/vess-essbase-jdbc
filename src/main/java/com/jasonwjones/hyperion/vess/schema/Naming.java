package com.jasonwjones.hyperion.vess.schema;

import java.util.Locale;
import java.util.Set;

/**
 * How names that come from Essbase become names of tables and columns: each
 * dimension's table, the dimension columns of CELLS and FACTS, and the
 * generation, alias table and attribute columns of a dimension's table.
 *
 * <p>
 * By default they're made SQL identifiers in capitals, as Vess's own names
 * are, and dimension tables start with DIM_: the Pkg Type dimension's table is
 * DIM_PKG_TYPE. They can be kept as Essbase spells them instead.
 *
 * @param style upper or lower for identifiers, or as-is for Essbase's spelling
 * @param dimensionPrefix what each dimension table's name starts with, such as
 *            DIM_; may be empty
 */
public record Naming(Style style, String dimensionPrefix) {

	public static final Naming DEFAULTS = new Naming(Style.UPPER, "DIM_");

	/**
	 * How a name is written.
	 */
	public enum Style {

		UPPER, LOWER, AS_IS;

		public static Style parse(String text) {
			switch (text.trim().toLowerCase(Locale.ROOT).replace('_', '-')) {
			case "upper":
				return UPPER;
			case "lower":
				return LOWER;
			case "as-is":
				return AS_IS;
			default:
				throw new IllegalArgumentException("can be upper, lower or as-is, not '" + text + "'");
			}
		}

	}

	/**
	 * A name as an identifier in this style: in capitals or small letters, with
	 * each run of anything but letters and digits made an underscore, so Pkg
	 * Type is PKG_TYPE, and an underscore before one that starts with a digit.
	 * As-is, the name is left alone.
	 */
	public String identifier(String name) {
		if (style == Style.AS_IS) {
			return name;
		}
		String identifier = name.replaceAll("[^\\p{L}\\p{N}]+", "_").replaceAll("^_+|_+$", "");
		if (identifier.isEmpty()) {
			return name;
		}
		if (Character.isDigit(identifier.charAt(0))) {
			identifier = "_" + identifier;
		}
		return style == Style.UPPER ? identifier.toUpperCase(Locale.ROOT) : identifier.toLowerCase(Locale.ROOT);
	}

	/**
	 * The name of a dimension's table.
	 */
	public String dimensionTable(String dimension) {
		return identifier(dimensionPrefix + dimension);
	}

	/**
	 * A name not already taken, with the suffix, and then a number, added
	 * until it isn't. The name taken is added to those taken.
	 *
	 * @param taken the names taken, in capitals
	 * @param name the name wanted, as Essbase has it
	 * @param suffix what to add when it's taken, such as Generation
	 */
	public String unique(Set<String> taken, String name, String suffix) {
		String candidate = identifier(name);
		if (taken.contains(candidate.toUpperCase(Locale.ROOT))) {
			candidate = identifier(name + " " + suffix);
		}
		for (int counter = 2; taken.contains(candidate.toUpperCase(Locale.ROOT)); counter++) {
			candidate = identifier(name + " " + suffix + " " + counter);
		}
		taken.add(candidate.toUpperCase(Locale.ROOT));
		return candidate;
	}

}
