package com.jasonwjones.hyperion.vess.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A cube's outline, read whole: its dimensions, every member with its
 * properties, and the alias tables. Member names are looked up without regard
 * to case, as Essbase does.
 */
public final class Outline {

	private final String application;

	private final String cube;

	String cubeType = "BSO";

	String dateFormat;

	final List<String> aliasTables = new ArrayList<>();

	String defaultAliasTable = "Default";

	final List<Dimension> dimensions = new ArrayList<>();

	private final Map<String, Member> members = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	/** Every place each member is, shared or not */
	private final Map<String, List<Member>> positions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private final Map<String, Dimension> dimensionsByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	Outline(String application, String cube) {
		this.application = application;
		this.cube = cube;
	}

	/**
	 * Indexes the members and works out what can only be worked out once the
	 * whole outline is read: levels, outline order, and which dimension each
	 * attribute dimension describes.
	 */
	void complete() {
		int order = 0;
		for (Dimension dimension : dimensions) {
			dimensionsByName.put(dimension.getName(), dimension);
			if (dimension.root != null) {
				dimension.root.computeLevels();
			}
			for (Member member : dimension.members) {
				member.setOrder(++order);
				positions.computeIfAbsent(member.isShared() ? member.getBaseMember() : member.getName(), name -> new ArrayList<>()).add(member);
				if (!member.isShared()) {
					members.putIfAbsent(member.getName(), member);
				}
			}
		}
		for (List<Member> places : positions.values()) {
			// Where the member is stored first, wherever in the outline that is
			places.sort((one, other) -> Boolean.compare(one.isShared(), other.isShared()));
		}
		for (Dimension dimension : dimensions) {
			for (Member member : dimension.members) {
				if (member.isShared()) {
					// A shared member stands for its base member, wherever that is
					Member base = members.get(member.baseMember);
					if (base != null) {
						member.setLevel(base.getLevel());
					}
				}
			}
			for (String attributeDimension : dimension.attributeDimensions) {
				Dimension attribute = dimensionsByName.get(attributeDimension);
				if (attribute != null) {
					attribute.baseDimension = dimension.getName();
				}
			}
		}
	}

	public String getApplication() {
		return application;
	}

	public String getCube() {
		return cube;
	}

	/**
	 * @return BSO or ASO
	 */
	public String getCubeType() {
		return cubeType;
	}

	/**
	 * @return the format of date attributes' names, as the outline gives it,
	 *         such as mm-dd-yyyy; or null
	 */
	public String getDateFormat() {
		return dateFormat;
	}

	public List<String> getAliasTables() {
		return Collections.unmodifiableList(aliasTables);
	}

	public String getDefaultAliasTable() {
		return defaultAliasTable;
	}

	/**
	 * @param wanted the name of an alias table, in any case, or null
	 * @return the outline's alias table of that name, or its default one if
	 *         it has none by that name
	 */
	public String aliasTable(String wanted) {
		if (wanted != null) {
			for (String table : aliasTables) {
				if (table.equalsIgnoreCase(wanted.trim())) {
					return table;
				}
			}
		}
		return defaultAliasTable;
	}

	/**
	 * @return every dimension, attribute dimensions included, in outline order
	 */
	public List<Dimension> getDimensions() {
		return Collections.unmodifiableList(dimensions);
	}

	/**
	 * @return the dimensions that aren't attribute dimensions: those that hold
	 *         data
	 */
	public List<Dimension> getStandardDimensions() {
		List<Dimension> standard = new ArrayList<>();
		for (Dimension dimension : dimensions) {
			if (!dimension.isAttribute()) {
				standard.add(dimension);
			}
		}
		return standard;
	}

	public Dimension getDimension(String name) {
		return dimensionsByName.get(name);
	}

	/**
	 * @param name a member's name, in any case
	 * @return the member, where it's stored rather than where it's shared; or
	 *         null if there's no such member
	 */
	public Member getMember(String name) {
		return name == null ? null : members.get(name);
	}

	/**
	 * @param name a member's name, in any case
	 * @return every place the member is: where it's stored, then where it's
	 *         shared; empty if there's no such member
	 */
	public List<Member> getPositions(String name) {
		List<Member> found = name == null ? null : positions.get(name);
		return found == null ? List.of() : Collections.unmodifiableList(found);
	}

	/**
	 * @return how many members the outline has, shared ones included
	 */
	public int getMemberCount() {
		int count = 0;
		for (Dimension dimension : dimensions) {
			count += dimension.members.size();
		}
		return count;
	}

	@Override
	public String toString() {
		return application + "." + cube + " (" + dimensions.size() + " dimensions, " + getMemberCount() + " members)";
	}

}
