package com.saxifrages.vess.h2.models.util;

import java.util.ArrayList;
import java.util.List;

import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueInt;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

/**
 * ValueBuilder is a helper class for building an array of Value objects, which
 * are needed to be returned from most EssModel classes.
 * 
 * @author jasonwjones
 *
 */
public class ValueBuilder {

	private List<Value> values;

	public ValueBuilder() {
		values = new ArrayList<Value>();
	}

	/**
	 * Adds a string value to the value chain. If a null is passed in, it will
	 * be converted to a ValueNull object (otherwise, trying to create a
	 * ValueString from NULL will cause an NPE)
	 * 
	 * @param value the string to create a value with, can be null 
	 * @return the ValueBuilder itself
	 */
	public ValueBuilder add(String value) {
		if (value == null) {
			return add(ValueNull.INSTANCE);
		} else {
			return add(ValueString.get(value));
		}
	}

	public ValueBuilder add(Boolean boolValue) {
		return add(ValueBoolean.get(boolValue));
	}

	public ValueBuilder add(int intValue) {
		return add(ValueInt.get(intValue));
	}

	public ValueBuilder add(Value value) {
		values.add(value);
		return this;
	}

	public Value[] build() {
		return values.toArray(new Value[0]);
	}

	public static Value stringValue(String value) {
		if (value == null) {
			return ValueNull.INSTANCE;
		} else {
			return ValueString.get(value);
		}
	}
	
}
