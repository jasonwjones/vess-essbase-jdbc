package com.jasonwjones.hyperion.vess.schema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.calcite.DataContext;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.util.NlsString;
import org.apache.calcite.util.Sarg;

import com.google.common.collect.Range;

/**
 * The conditions a query puts on a table's rows, which a table can use to read
 * only what's wanted. They're the WHERE clause split into the parts that must
 * all hold, and each refers to the table's columns by position.
 */
public final class Conditions {

	private final List<RexNode> conjuncts;

	private final DataContext root;

	private final int[] projects;

	public Conditions(List<RexNode> filters, DataContext root) {
		this(filters, root, null);
	}

	/**
	 * @param projects the columns the query reads, or null for all of them
	 */
	public Conditions(List<RexNode> filters, DataContext root, int[] projects) {
		this.conjuncts = new ArrayList<>();
		for (RexNode filter : filters) {
			conjuncts.addAll(RelOptUtil.conjunctions(filter));
		}
		this.root = root;
		this.projects = projects;
	}

	/**
	 * @return no conditions at all
	 */
	public static Conditions none() {
		return new Conditions(List.of(), null);
	}

	/**
	 * @return the parts of the WHERE clause, all of which must hold
	 */
	public List<RexNode> getConjuncts() {
		return conjuncts;
	}

	/**
	 * Whether the query reads a column, so a table can skip one that's costly
	 * to fill when it doesn't.
	 */
	public boolean wants(int column) {
		if (projects == null) {
			return true;
		}
		for (int project : projects) {
			if (project == column) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the query's context, or null when there's no query
	 */
	public DataContext getRoot() {
		return root;
	}

	/**
	 * The values a column must have, where the conditions say so outright: it
	 * equals one, or is IN a list, or one of several such alternatives.
	 *
	 * @param column the column's position
	 * @return the values, or null if the conditions don't limit the column to a
	 *         list of them
	 */
	public Set<String> values(int column) {
		Set<String> values = null;
		for (RexNode conjunct : conjuncts) {
			Set<String> these = valuesOf(conjunct, column);
			if (these != null) {
				if (values == null) {
					values = these;
				} else {
					values.retainAll(these);
				}
			}
		}
		return values;
	}

	/**
	 * @return the single value a column must have, or null if it isn't held
	 *         to one
	 */
	public String value(int column) {
		Set<String> values = values(column);
		return values != null && values.size() == 1 ? values.iterator().next() : null;
	}

	private static Set<String> valuesOf(RexNode node, int column) {
		switch (node.getKind()) {
		case EQUALS: {
			RexCall call = (RexCall) node;
			String value = literalFor(call.getOperands().get(0), call.getOperands().get(1), column);
			if (value == null) {
				value = literalFor(call.getOperands().get(1), call.getOperands().get(0), column);
			}
			if (value == null) {
				return null;
			}
			Set<String> values = new LinkedHashSet<>();
			values.add(value);
			return values;
		}
		case SEARCH: {
			RexCall call = (RexCall) node;
			if (!isColumn(call.getOperands().get(0), column) || !(call.getOperands().get(1) instanceof RexLiteral)) {
				return null;
			}
			Sarg<?> sarg = ((RexLiteral) call.getOperands().get(1)).getValueAs(Sarg.class);
			if (sarg == null || !sarg.isPoints()) {
				return null;
			}
			Set<String> values = new LinkedHashSet<>();
			for (Range<?> range : sarg.rangeSet.asRanges()) {
				values.add(text(range.lowerEndpoint()));
			}
			return values;
		}
		case OR: {
			Set<String> values = new LinkedHashSet<>();
			for (RexNode operand : ((RexCall) node).getOperands()) {
				Set<String> these = valuesOf(operand, column);
				if (these == null) {
					return null;
				}
				values.addAll(these);
			}
			return values;
		}
		default:
			return null;
		}
	}

	private static String literalFor(RexNode reference, RexNode literal, int column) {
		if (isColumn(reference, column) && literal instanceof RexLiteral) {
			Object value = ((RexLiteral) literal).getValue();
			return value == null ? null : text(value);
		}
		return null;
	}

	/**
	 * Whether a node is the column, allowing for the casts the engine adds when
	 * comparing text of different lengths.
	 */
	static boolean isColumn(RexNode node, int column) {
		RexNode stripped = RexUtil.removeCast(node);
		return stripped instanceof RexInputRef && ((RexInputRef) stripped).getIndex() == column;
	}

	static String text(Object value) {
		return value instanceof NlsString ? ((NlsString) value).getValue() : String.valueOf(value);
	}

}
