package com.jasonwjones.hyperion.vess.schema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.outline.Member;
import com.jasonwjones.hyperion.vess.outline.Outline;

/**
 * What the outline functions do, such as IS_DESCENDANT(Product, 'Colas'). The
 * SQL engine calls these from the code it generates, with the cube's key put
 * in front of the arguments the query gives, which is how a function knows
 * which outline to look in.
 *
 * <p>
 * A member that's shared somewhere counts as being there too, so the members
 * under Diet in Sample.Basic include the colas it shares.
 */
public final class OutlineFunctions {

	/** Where a cube's outline is, and the alias table its ALIAS means */
	private record Cube(OutlineSource source, String aliasTable) {
	}

	private static final Map<String, Cube> CUBES = new ConcurrentHashMap<>();

	private OutlineFunctions() {
	}

	/**
	 * @param aliasTable the alias table MEMBER_ALIAS(member) looks in, or null
	 *            for the cube's default
	 */
	public static void register(String key, OutlineSource source, String aliasTable) {
		CUBES.put(key, new Cube(source, aliasTable));
	}

	/**
	 * Forgets the cubes registered under keys starting with the given prefix,
	 * when a connection closes.
	 */
	public static void forget(String prefix) {
		CUBES.keySet().removeIf(key -> key.startsWith(prefix));
	}

	static Outline outline(String key) {
		Cube cube = CUBES.get(key);
		if (cube == null) {
			throw new IllegalStateException("The outline functions were used after their connection closed");
		}
		try {
			return cube.source().outline();
		} catch (VessException e) {
			throw new VessException.Unchecked(e);
		}
	}

	public static Boolean isDescendant(String key, String member, String ancestor) {
		if (member == null || ancestor == null) {
			return null;
		}
		Outline outline = outline(key);
		Member above = outline.getMember(ancestor);
		if (above == null) {
			return false;
		}
		for (Member position : outline.getPositions(member)) {
			if (position.isDescendantOf(above)) {
				return true;
			}
			// Under a member that's shared below the ancestor, too
			for (Member shared : outline.getPositions(above.getName())) {
				if (position.isDescendantOf(shared)) {
					return true;
				}
			}
		}
		return false;
	}

	public static Boolean isChild(String key, String member, String parent) {
		if (member == null || parent == null) {
			return null;
		}
		for (Member position : outline(key).getPositions(member)) {
			if (position.getParent() != null && position.getParent().getName().equalsIgnoreCase(parent)) {
				return true;
			}
		}
		return false;
	}

	public static Integer memberLevel(String key, String member) {
		Member found = outline(key).getMember(member);
		return found == null ? null : found.getLevel();
	}

	public static Integer memberGeneration(String key, String member) {
		Member found = outline(key).getMember(member);
		return found == null ? null : found.getGeneration();
	}

	public static String memberParent(String key, String member) {
		Member found = outline(key).getMember(member);
		return found == null || found.getParent() == null ? null : found.getParent().getName();
	}

	public static String memberAlias(String key, String member) {
		Outline outline = outline(key);
		Member found = outline.getMember(member);
		return found == null ? null : found.getAlias(outline.aliasTable(CUBES.get(key).aliasTable()));
	}

	public static String memberAliasIn(String key, String member, String table) {
		Member found = outline(key).getMember(member);
		if (found == null || table == null) {
			return null;
		}
		for (Map.Entry<String, String> alias : found.getAliases().entrySet()) {
			if (alias.getKey().equalsIgnoreCase(table)) {
				return alias.getValue();
			}
		}
		return null;
	}

	public static Boolean hasUda(String key, String member, String uda) {
		if (member == null || uda == null) {
			return null;
		}
		for (Member position : outline(key).getPositions(member)) {
			for (String each : position.getUdas()) {
				if (each.equalsIgnoreCase(uda)) {
					return true;
				}
			}
		}
		return false;
	}

	public static String memberAttribute(String key, String member, String attributeDimension) {
		Member found = outline(key).getMember(member);
		if (found == null || attributeDimension == null) {
			return null;
		}
		for (Map.Entry<String, String> attribute : found.getAttributes().entrySet()) {
			if (attribute.getKey().equalsIgnoreCase(attributeDimension)) {
				return attribute.getValue();
			}
		}
		return null;
	}

	public static String memberDimension(String key, String member) {
		Member found = outline(key).getMember(member);
		if (found == null) {
			return null;
		}
		Dimension dimension = found.getDimension();
		return dimension.getName();
	}

}
