package com.saxifrages.vess.h2;

public class StringUtil {

	public static String emptyIfNull(String value) {
		if (value == null) {
			return "";
		} else {
			return value;
		}
	}
	
}
