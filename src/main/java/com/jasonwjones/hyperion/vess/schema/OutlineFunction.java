package com.jasonwjones.hyperion.vess.schema;

import static com.jasonwjones.hyperion.vess.schema.Column.MEMBER_NAME;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.adapter.enumerable.CallImplementor;
import org.apache.calcite.adapter.enumerable.NullPolicy;
import org.apache.calcite.adapter.enumerable.ReflectiveCallNotNullImplementor;
import org.apache.calcite.adapter.enumerable.RexImpTable;
import org.apache.calcite.adapter.enumerable.RexToLixTranslator;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.schema.FunctionParameter;
import org.apache.calcite.schema.ImplementableFunction;
import org.apache.calcite.schema.ScalarFunction;

/**
 * A SQL function that answers from a cube's outline, such as
 * IS_DESCENDANT(PRODUCT, '100'). Each cube's schema has its own, bound to
 * that cube.
 */
public final class OutlineFunction implements ScalarFunction, ImplementableFunction {

	private final String name;

	private final String remarks;

	private final List<Column> parameters;

	private final Column result;

	private final Method method;

	private final String cubeKey;

	private OutlineFunction(String name, String remarks, List<Column> parameters, Column result, String methodName, String cubeKey) {
		this.name = name;
		this.remarks = remarks;
		this.parameters = parameters;
		this.result = result;
		this.cubeKey = cubeKey;
		Class<?>[] types = new Class<?>[parameters.size() + 1];
		types[0] = String.class;
		for (int index = 1; index < types.length; index++) {
			types[index] = String.class;
		}
		try {
			this.method = OutlineFunctions.class.getMethod(methodName, types);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The functions for one cube, which find its outline by the given key.
	 */
	public static List<OutlineFunction> forCube(String cubeKey) {
		Column member = Column.text("MEMBER", MEMBER_NAME);
		List<OutlineFunction> functions = new ArrayList<>();
		functions.add(new OutlineFunction("IS_DESCENDANT",
				"Whether a member is below another, at any depth, counting where it's shared: IS_DESCENDANT(PRODUCT, '100')",
				List.of(member, Column.text("ANCESTOR", MEMBER_NAME)), Column.bool("RESULT"), "isDescendant", cubeKey));
		functions.add(new OutlineFunction("IS_CHILD", "Whether a member is directly below another: IS_CHILD(MARKET, 'East')",
				List.of(member, Column.text("PARENT", MEMBER_NAME)), Column.bool("RESULT"), "isChild", cubeKey));
		functions.add(new OutlineFunction("MEMBER_LEVEL", "A member's level: 0 for one without children", List.of(member),
				Column.integer("RESULT"), "memberLevel", cubeKey));
		functions.add(new OutlineFunction("MEMBER_GENERATION", "A member's generation: 1 for the dimension itself", List.of(member),
				Column.integer("RESULT"), "memberGeneration", cubeKey));
		functions.add(new OutlineFunction("MEMBER_PARENT", "A member's parent", List.of(member), Column.text("RESULT", MEMBER_NAME),
				"memberParent", cubeKey));
		functions.add(new OutlineFunction("MEMBER_ALIAS", "A member's alias, in the alias table the connection uses", List.of(member),
				Column.text("RESULT", MEMBER_NAME), "memberAlias", cubeKey));
		functions.add(new OutlineFunction("MEMBER_ALIAS", "A member's alias in the given alias table: MEMBER_ALIAS(PRODUCT, 'Long Names')",
				List.of(member, Column.text("ALIAS_TABLE", MEMBER_NAME)), Column.text("RESULT", MEMBER_NAME), "memberAliasIn", cubeKey));
		functions.add(new OutlineFunction("HAS_UDA", "Whether a member has a user-defined attribute: HAS_UDA(MARKET, 'Major Market')",
				List.of(member, Column.text("UDA", MEMBER_NAME)), Column.bool("RESULT"), "hasUda", cubeKey));
		functions.add(new OutlineFunction("MEMBER_ATTRIBUTE",
				"A member's attribute in an attribute dimension: MEMBER_ATTRIBUTE(PRODUCT, 'Caffeinated')",
				List.of(member, Column.text("ATTRIBUTE_DIMENSION", MEMBER_NAME)), Column.text("RESULT", MEMBER_NAME), "memberAttribute",
				cubeKey));
		functions.add(new OutlineFunction("MEMBER_DIMENSION", "The dimension a member belongs to", List.of(member),
				Column.text("RESULT", MEMBER_NAME), "memberDimension", cubeKey));
		return functions;
	}

	public String getName() {
		return name;
	}

	public String getRemarks() {
		return remarks;
	}

	public List<Column> getParameterColumns() {
		return parameters;
	}

	public Column getResult() {
		return result;
	}

	@Override
	public RelDataType getReturnType(RelDataTypeFactory typeFactory) {
		return result.toRelType(typeFactory);
	}

	@Override
	public List<FunctionParameter> getParameters() {
		List<FunctionParameter> list = new ArrayList<>();
		for (int index = 0; index < parameters.size(); index++) {
			Column column = parameters.get(index);
			int ordinal = index;
			list.add(new FunctionParameter() {

				@Override
				public int getOrdinal() {
					return ordinal;
				}

				@Override
				public String getName() {
					return column.getName();
				}

				@Override
				public RelDataType getType(RelDataTypeFactory typeFactory) {
					return column.toRelType(typeFactory);
				}

				@Override
				public boolean isOptional() {
					return false;
				}

			});
		}
		return list;
	}

	@Override
	public CallImplementor getImplementor() {
		ReflectiveCallNotNullImplementor implementor = new ReflectiveCallNotNullImplementor(method) {

			@Override
			public Expression implement(RexToLixTranslator translator, RexCall call, List<Expression> operands) {
				List<Expression> arguments = new ArrayList<>();
				arguments.add(Expressions.constant(cubeKey));
				arguments.addAll(operands);
				return super.implement(translator, call, arguments);
			}

		};
		// The functions answer null for null arguments themselves
		return RexImpTable.createImplementor(implementor, NullPolicy.NONE, false);
	}

}
