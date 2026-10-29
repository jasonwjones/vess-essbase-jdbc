package com.jasonwjones.hyperion.vess.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A dimension of an outline, with its members in outline order.
 */
public final class Dimension {

	/** What a dimension can be tagged as, as Essbase names them */
	public static final String REGULAR = "Regular";

	public static final String ACCOUNTS = "Accounts";

	public static final String TIME = "Time";

	public static final String ATTRIBUTE = "Attribute";

	private final String name;

	private final int number;

	String type = REGULAR;

	String density = "Sparse";

	String attributeType;

	String baseDimension;

	final List<String> attributeDimensions = new ArrayList<>();

	/** Generation and level names, by number */
	final Map<Integer, String> generationNames = new LinkedHashMap<>();

	final Map<Integer, String> levelNames = new LinkedHashMap<>();

	final List<Member> members = new ArrayList<>();

	Member root;

	Dimension(String name, int number) {
		this.name = name;
		this.number = number;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the dimension's place in the outline, counting from 1
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @return Regular, Accounts, Time, Attribute, Country or Currency Partition
	 */
	public String getType() {
		return type;
	}

	public boolean isAttribute() {
		return ATTRIBUTE.equals(type);
	}

	/**
	 * @return Dense or Sparse; Sparse for attribute dimensions, which store
	 *         nothing
	 */
	public String getDensity() {
		return density;
	}

	/**
	 * @return for an attribute dimension, Text, Numeric, Boolean or Date;
	 *         otherwise null
	 */
	public String getAttributeType() {
		return attributeType;
	}

	/**
	 * @return for an attribute dimension, the dimension whose members it
	 *         describes; otherwise null
	 */
	public String getBaseDimension() {
		return baseDimension;
	}

	/**
	 * @return the attribute dimensions that describe this dimension's members
	 */
	public List<String> getAttributeDimensions() {
		return Collections.unmodifiableList(attributeDimensions);
	}

	/**
	 * @return the member that is the dimension itself, at the top of it
	 */
	public Member getRoot() {
		return root;
	}

	/**
	 * @return every member, in outline order, including the dimension itself
	 *         and shared members
	 */
	public List<Member> getMembers() {
		return Collections.unmodifiableList(members);
	}

	/**
	 * @return the members that aren't shared, each name once, in outline order
	 */
	public List<Member> getBaseMembers() {
		List<Member> base = new ArrayList<>();
		for (Member member : members) {
			if (!member.isShared()) {
				base.add(member);
			}
		}
		return base;
	}

	/**
	 * @return how many generations deep the dimension goes
	 */
	public int getGenerations() {
		int deepest = 0;
		for (Member member : members) {
			deepest = Math.max(deepest, member.getGeneration());
		}
		return deepest;
	}

	/**
	 * @return how many levels the dimension has
	 */
	public int getLevels() {
		return root == null ? 0 : root.getLevel() + 1;
	}

	/**
	 * @param number a generation number
	 * @return the name given to that generation, or null if it has none
	 */
	public String getGenerationName(int number) {
		return generationNames.get(number);
	}

	/**
	 * @param number a level number
	 * @return the name given to that level, or null if it has none
	 */
	public String getLevelName(int number) {
		return levelNames.get(number);
	}

	@Override
	public String toString() {
		return name;
	}

}
