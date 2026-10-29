package com.jasonwjones.hyperion.vess;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.babel.SqlBabelParserImpl;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

import com.jasonwjones.hyperion.vess.schema.VessModel;

/**
 * The embedded SQL engine, Apache Calcite, set up the way Vess wants it.
 *
 * <p>
 * Names are matched without regard to case, as Essbase matches them, and keep
 * the case they're written in. Double quotes quote a name. The lenient parser
 * is used because the names Essbase cubes are full of - Year, Time, Period,
 * Value - are words standard SQL reserves. Validation is lenient too, so a
 * query can group or sort by an alias, but not as lenient as the parser's own
 * conformance, which reads an unknown column as an alias of itself and says
 * so instead of that the column isn't there.
 */
final class Engine {

	private Engine() {
	}

	static Connection connect() throws SQLException {
		Properties info = new Properties();
		info.setProperty("quoting", Quoting.DOUBLE_QUOTE.name());
		info.setProperty("unquotedCasing", Casing.UNCHANGED.name());
		info.setProperty("quotedCasing", Casing.UNCHANGED.name());
		info.setProperty("caseSensitive", "false");
		info.setProperty("parserFactory", SqlBabelParserImpl.class.getName() + "#FACTORY");
		info.setProperty("conformance", SqlConformanceEnum.LENIENT.name());
		Connection connection = new org.apache.calcite.jdbc.Driver().connect("jdbc:calcite:", info);
		if (connection == null) {
			throw new VessException("Could not start the SQL engine");
		}
		return connection;
	}

	/**
	 * The parser's settings, for the statements Vess reads itself.
	 */
	static SqlParser.Config parserConfig() {
		return SqlParser.config().withParserFactory(SqlBabelParserImpl.FACTORY).withConformance(SqlConformanceEnum.LENIENT)
				.withQuoting(Quoting.DOUBLE_QUOTE).withUnquotedCasing(Casing.UNCHANGED).withQuotedCasing(Casing.UNCHANGED)
				.withCaseSensitive(false);
	}

	static SqlNode parse(String sql) throws VessException {
		try {
			return SqlParser.create(sql, parserConfig()).parseStmt();
		} catch (SqlParseException e) {
			throw new VessException(e.getMessage(), e);
		}
	}

	/**
	 * The name of a schema at the top level that stands for a nested one, such
	 * as "Sample.Basic" for the Basic cube in Sample. The engine resolves
	 * unqualified names against a single schema name, so making the cube the
	 * one a connection works in takes a name of its own for it.
	 */
	static String alias(VessModel model, String catalog, String schema, Schema target) {
		String alias = catalog + "." + schema;
		model.addAlias(alias, target);
		return alias;
	}

}
