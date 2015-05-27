package com.saxifrages.vess.h2;

import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.FunctionCall;
import org.h2.value.ValueResultSet;

public class FooFunctionCall implements FunctionCall {

	public String getName() {
		return "FooFunction";
	}

	public ValueResultSet getValueForColumnList(Session session, Expression[] nullArgs) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getType() {
		return 0;
	}

	public Expression optimize(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	public Expression[] getArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSQL() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isDeterministic() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isBufferResultSetToLocalTemp() {
		// TODO Auto-generated method stub
		return false;
	}

}
