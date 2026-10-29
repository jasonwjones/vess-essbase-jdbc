package com.jasonwjones.hyperion.vess.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A member of an outline, at one place in its hierarchy. A shared member is a
 * second place for a member stored elsewhere, and is a member of its own here.
 */
public final class Member {

	private final String name;

	private final Dimension dimension;

	private final Member parent;

	private final List<Member> children = new ArrayList<>();

	private final int generation;

	private int level;

	private int order;

	String storage = Storage.STORE;

	String consolidation = "+";

	String timeBalance;

	String varianceReporting;

	boolean twoPass;

	boolean shared;

	String baseMember;

	String formula;

	String comment;

	final Map<String, String> aliases = new LinkedHashMap<>();

	final List<String> udas = new ArrayList<>();

	final Map<String, String> attributes = new LinkedHashMap<>();

	Member(String name, Dimension dimension, Member parent) {
		this.name = name;
		this.dimension = dimension;
		this.parent = parent;
		this.generation = parent == null ? 1 : parent.generation + 1;
		if (parent != null) {
			parent.children.add(this);
		}
	}

	/**
	 * What a member's storage can be, as Essbase names them.
	 */
	public static final class Storage {

		public static final String STORE = "Store";

		public static final String DYNAMIC_CALC = "Dynamic Calc";

		public static final String DYNAMIC_CALC_AND_STORE = "Dynamic Calc and Store";

		public static final String LABEL_ONLY = "Label Only";

		public static final String NEVER_SHARE = "Never Share";

		public static final String SHARED = "Shared";

		private Storage() {
		}

		static String parse(String xml) {
			if (xml == null) {
				return STORE;
			}
			switch (xml) {
			case "DynamicCalc":
				return DYNAMIC_CALC;
			case "DynamicCalcAndStore":
				return DYNAMIC_CALC_AND_STORE;
			case "LabelOnly":
				return LABEL_ONLY;
			case "NeverShare":
				return NEVER_SHARE;
			case "ShareData":
				return SHARED;
			case "StoreData":
				return STORE;
			default:
				return xml;
			}
		}

	}

	public String getName() {
		return name;
	}

	public Dimension getDimension() {
		return dimension;
	}

	/**
	 * @return the parent, or null for the dimension itself
	 */
	public Member getParent() {
		return parent;
	}

	public List<Member> getChildren() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * @return 1 for the dimension, 2 for its children, and so on
	 */
	public int getGeneration() {
		return generation;
	}

	/**
	 * @return 0 for a member without children, otherwise one more than the
	 *         highest of its children's, as Essbase counts levels
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return the member's place in the outline, counting from 1 across every
	 *         dimension, so that sorting by it gives the outline's order
	 */
	public int getOrder() {
		return order;
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}

	public boolean isDimension() {
		return parent == null;
	}

	/**
	 * @return Store, Dynamic Calc, Label Only, Never Share, Shared, or Dynamic
	 *         Calc and Store
	 */
	public String getStorage() {
		return storage;
	}

	/**
	 * Whether the member holds data of its own: not shared, not dynamically
	 * calculated, and not just a label.
	 */
	public boolean isStored() {
		return !shared && !storage.equals(Storage.DYNAMIC_CALC) && !storage.equals(Storage.LABEL_ONLY);
	}

	/**
	 * @return the consolidation operator: +, -, *, /, %, ~ or ^
	 */
	public String getConsolidation() {
		return consolidation;
	}

	/**
	 * @return First, Last or Average, or null for none
	 */
	public String getTimeBalance() {
		return timeBalance;
	}

	/**
	 * @return Expense, or null
	 */
	public String getVarianceReporting() {
		return varianceReporting;
	}

	public boolean isTwoPass() {
		return twoPass;
	}

	public boolean isShared() {
		return shared;
	}

	/**
	 * @return for a shared member, the name of the member it shares; otherwise
	 *         null
	 */
	public String getBaseMember() {
		return baseMember;
	}

	/**
	 * @return the member's formula, or null
	 */
	public String getFormula() {
		return formula;
	}

	public String getComment() {
		return comment;
	}

	/**
	 * @return the member's aliases, by alias table
	 */
	public Map<String, String> getAliases() {
		return Collections.unmodifiableMap(aliases);
	}

	/**
	 * @param table an alias table
	 * @return the member's alias in that table, or null
	 */
	public String getAlias(String table) {
		return aliases.get(table);
	}

	public List<String> getUdas() {
		return Collections.unmodifiableList(udas);
	}

	/**
	 * @return the attributes the member is associated with: the name of each
	 *         attribute dimension, and the member of it
	 */
	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	/**
	 * @param generation a generation number
	 * @return this member's ancestor at that generation, this member itself if
	 *         it's there, or null if the member is above it
	 */
	public Member getAncestor(int generation) {
		if (generation > this.generation || generation < 1) {
			return null;
		}
		Member member = this;
		while (member.generation > generation) {
			member = member.parent;
		}
		return member;
	}

	/**
	 * @return whether this member is below the given one, at any depth
	 */
	public boolean isDescendantOf(Member ancestor) {
		for (Member member = parent; member != null; member = member.parent) {
			if (member == ancestor) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Works out the level of this member and the members below it, which can
	 * only be done once they're all read.
	 */
	int computeLevels() {
		int highest = -1;
		for (Member child : children) {
			highest = Math.max(highest, child.computeLevels());
		}
		level = highest + 1;
		return level;
	}

	void setLevel(int level) {
		this.level = level;
	}

	void setOrder(int order) {
		this.order = order;
	}

	@Override
	public String toString() {
		return name;
	}

}
