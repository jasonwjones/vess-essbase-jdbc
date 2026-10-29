package com.jasonwjones.hyperion.vess;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * What kind of statement some text is, which decides who runs it: MDX goes to
 * Essbase through Thriller, a CALL runs a procedure, INSERT, UPDATE and DELETE
 * change the server, and anything else is a query for the SQL engine.
 */
final class Sql {

	enum Kind {
		QUERY, MDX, CALL, DML
	}

	/**
	 * An axis, which only MDX has: ON COLUMNS, ON ROWS, ON AXIS(2), ON 0. Not
	 * followed by a dot or a comparison, which would make it a SQL join on a
	 * table that happens to be called rows.
	 */
	private static final Pattern AXIS = Pattern.compile(
			"\\bON\\s+(?:(?:COLUMNS|ROWS|PAGES|CHAPTERS|SECTIONS)\\b(?!\\s*[.=<>!])|AXIS\\s*\\(|\\d+\\b(?!\\s*[.=<>!]))",
			Pattern.CASE_INSENSITIVE);

	/** MDX's own WITH, which defines members and sets rather than a subquery */
	private static final Pattern MDX_WITH = Pattern.compile("^WITH\\s+(?:MEMBER|SET|CALCULATED|DYNAMIC)\\b", Pattern.CASE_INSENSITIVE);

	private Sql() {
	}

	static Kind classify(String sql) {
		String text = withoutLeadingComments(sql);
		if (text.startsWith("{")) {
			return Kind.CALL;
		}
		String first = firstWord(text);
		switch (first) {
		case "CALL":
		case "EXEC":
		case "EXECUTE":
			return Kind.CALL;
		case "INSERT":
		case "UPDATE":
		case "DELETE":
		case "UPSERT":
		case "MERGE":
			return Kind.DML;
		case "SELECT":
		case "WITH":
			return MDX_WITH.matcher(text).find() || AXIS.matcher(withoutQuoted(text)).find() ? Kind.MDX : Kind.QUERY;
		default:
			return Kind.QUERY;
		}
	}

	static String firstWord(String text) {
		int end = 0;
		while (end < text.length() && Character.isLetter(text.charAt(end))) {
			end++;
		}
		return text.substring(0, end).toUpperCase(Locale.ROOT);
	}

	/**
	 * The text with whitespace and comments at the start taken off.
	 */
	static String withoutLeadingComments(String sql) {
		int index = 0;
		while (index < sql.length()) {
			if (Character.isWhitespace(sql.charAt(index))) {
				index++;
			} else if (sql.startsWith("--", index)) {
				int end = sql.indexOf('\n', index);
				index = end < 0 ? sql.length() : end + 1;
			} else if (sql.startsWith("/*", index)) {
				int end = sql.indexOf("*/", index + 2);
				index = end < 0 ? sql.length() : end + 2;
			} else {
				break;
			}
		}
		return sql.substring(index);
	}

	/**
	 * The text with the insides of quotes, brackets and comments blanked, so
	 * that a word inside a member name or a string doesn't count.
	 */
	static String withoutQuoted(String text) {
		StringBuilder out = new StringBuilder(text.length());
		char closing = 0;
		boolean lineComment = false;
		boolean blockComment = false;
		for (int index = 0; index < text.length(); index++) {
			char c = text.charAt(index);
			if (lineComment) {
				if (c == '\n') {
					lineComment = false;
					out.append(c);
				} else {
					out.append(' ');
				}
			} else if (blockComment) {
				if (c == '*' && index + 1 < text.length() && text.charAt(index + 1) == '/') {
					blockComment = false;
					index++;
					out.append("  ");
				} else {
					out.append(' ');
				}
			} else if (closing != 0) {
				if (c == closing) {
					closing = 0;
					out.append(c);
				} else {
					out.append(' ');
				}
			} else if (c == '\'' || c == '"') {
				closing = c;
				out.append(c);
			} else if (c == '[') {
				closing = ']';
				out.append(c);
			} else if (c == '-' && index + 1 < text.length() && text.charAt(index + 1) == '-') {
				lineComment = true;
				out.append(' ');
			} else if (c == '/' && index + 1 < text.length() && text.charAt(index + 1) == '*') {
				blockComment = true;
				out.append(' ');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	/**
	 * The statement without a semicolon at the end, which the engine won't
	 * take.
	 */
	static String withoutSemicolon(String sql) {
		String trimmed = sql.strip();
		while (trimmed.endsWith(";")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
		}
		return trimmed;
	}

}
