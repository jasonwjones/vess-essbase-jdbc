package com.jasonwjones.hyperion.vess.cells;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexExecutable;
import org.apache.calcite.rex.RexExecutorImpl;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.util.ImmutableBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jasonwjones.hyperion.vess.outline.Dimension;
import com.jasonwjones.hyperion.vess.outline.Member;
import com.jasonwjones.hyperion.vess.schema.Conditions;

/**
 * Works out which members of each dimension a query of the cells wants.
 *
 * <p>
 * Each dimension starts with every member it could have in the table. Then
 * every condition of the WHERE clause that's about that dimension alone - an
 * equality, an IN list, a LIKE, an outline function such as IS_DESCENDANT, any
 * expression at all of just that column - is tried against each of those
 * members, and only the members it holds for are kept. The engine still applies
 * every condition to the rows that come back, so this changes how much is read,
 * never what the query answers. A condition that mentions two dimensions, or the
 * value, is left to the engine.
 */
final class MemberSets {

	private static final Logger logger = LoggerFactory.getLogger(MemberSets.class);

	private MemberSets() {
	}

	/**
	 * @param dimensions the table's dimension columns, in order
	 * @param domains each dimension's members, before the conditions
	 * @param conditions the query's conditions
	 * @return each dimension's members, after the conditions, in outline order
	 */
	static List<List<Member>> narrow(List<Dimension> dimensions, List<List<Member>> domains, Conditions conditions) {
		List<List<Member>> sets = new ArrayList<>();
		for (int column = 0; column < dimensions.size(); column++) {
			List<Member> members = domains.get(column);
			for (RexNode conjunct : conditions.getConjuncts()) {
				ImmutableBitSet columns = RelOptUtil.InputFinder.bits(conjunct);
				if (columns.cardinality() != 1 || columns.nth(0) != column) {
					continue;
				}
				members = keep(members, conjunct, column, conditions);
				if (members.isEmpty()) {
					break;
				}
			}
			sets.add(members);
		}
		return sets;
	}

	/**
	 * The members a condition about one column holds for.
	 */
	private static List<Member> keep(List<Member> members, RexNode conjunct, int column, Conditions conditions) {
		// Equalities and IN lists, the commonest, are looked up rather than run
		Set<String> values = new Conditions(List.of(conjunct), null).values(column);
		if (values != null) {
			List<Member> kept = new ArrayList<>();
			for (Member member : members) {
				if (values.contains(member.getName())) {
					kept.add(member);
				}
			}
			return kept;
		}
		DataContext root = conditions.getRoot();
		if (root == null || root.getTypeFactory() == null) {
			return members;
		}
		try {
			JavaTypeFactory typeFactory = root.getTypeFactory();
			RexBuilder rexBuilder = new RexBuilder(typeFactory);
			RelDataType columnType = null;
			for (RexInputRef reference : references(conjunct)) {
				columnType = reference.getType();
			}
			RexNode alone = conjunct.accept(new RexShuttle() {

				@Override
				public RexNode visitInputRef(RexInputRef reference) {
					return new RexInputRef(0, reference.getType());
				}

			});
			RelDataType rowType = typeFactory.builder().add("MEMBER", columnType).build();
			RexExecutable executable = RexExecutorImpl.getExecutable(rexBuilder, List.of(alone), rowType);
			OneRow row = new OneRow(root);
			executable.setDataContext(row);
			List<Member> kept = new ArrayList<>();
			for (Member member : members) {
				row.values[0] = member.getName();
				Object[] result = executable.execute();
				if (result != null && Boolean.TRUE.equals(result[0])) {
					kept.add(member);
				}
			}
			return kept;
		} catch (RuntimeException | LinkageError e) {
			// It's left to the engine, which will apply it to every row
			logger.debug("Could not try {} against the members, so it reads them all: {}", conjunct, e.toString());
			return members;
		}
	}

	private static List<RexInputRef> references(RexNode node) {
		List<RexInputRef> references = new ArrayList<>();
		node.accept(new RexShuttle() {

			@Override
			public RexNode visitInputRef(RexInputRef reference) {
				references.add(reference);
				return reference;
			}

		});
		return references;
	}

	/**
	 * The context an expression runs in, holding the one value it's tried
	 * against, and otherwise the query's own.
	 */
	private static final class OneRow implements DataContext {

		private final DataContext query;

		final Object[] values = new Object[1];

		OneRow(DataContext query) {
			this.query = query;
		}

		@Override
		public SchemaPlus getRootSchema() {
			return query.getRootSchema();
		}

		@Override
		public JavaTypeFactory getTypeFactory() {
			return query.getTypeFactory();
		}

		@Override
		public QueryProvider getQueryProvider() {
			return query.getQueryProvider();
		}

		@Override
		public Object get(String name) {
			if ("inputRecord".equals(name)) {
				return values;
			}
			return query.get(name);
		}

	}

}
