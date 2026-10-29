package com.jasonwjones.hyperion.vess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SqlTest {

	@Test
	void tellsMdxFromSql() {
		assertEquals(Sql.Kind.MDX, Sql.classify("SELECT {[Jan]} ON COLUMNS, [Market].Children ON ROWS FROM Sample.Basic"));
		assertEquals(Sql.Kind.MDX, Sql.classify("select {} on 0 from Sample.Basic"));
		assertEquals(Sql.Kind.MDX, Sql.classify("SELECT x ON AXIS(0) FROM Sample.Basic"));
		assertEquals(Sql.Kind.MDX, Sql.classify("WITH MEMBER [Measures].[M] AS '1' SELECT {[M]} ON COLUMNS FROM Sample.Basic"));
		assertEquals(Sql.Kind.MDX, Sql.classify("-- a comment\n/* another */ SELECT {[Jan]} ON COLUMNS FROM Sample.Basic"));
		assertEquals(Sql.Kind.QUERY, Sql.classify("select * from Cells"));
		assertEquals(Sql.Kind.QUERY, Sql.classify("select * from a join rows on rows.id = a.id"));
		assertEquals(Sql.Kind.QUERY, Sql.classify("select * from a join b on 1 = 1"));
		assertEquals(Sql.Kind.QUERY, Sql.classify("select 'on columns' from t"));
		assertEquals(Sql.Kind.QUERY, Sql.classify("select [on rows] from t"));
		assertEquals(Sql.Kind.QUERY, Sql.classify("with t as (select 1) select * from t"));
		assertEquals(Sql.Kind.QUERY, Sql.classify("select 'keep alive'"));
	}

	@Test
	void tellsCallsAndChanges() {
		assertEquals(Sql.Kind.CALL, Sql.classify("CALL Sample.Basic.CalcAll()"));
		assertEquals(Sql.Kind.CALL, Sql.classify("{call RUN_CALC(?)}"));
		assertEquals(Sql.Kind.CALL, Sql.classify("exec CalcAll"));
		assertEquals(Sql.Kind.DML, Sql.classify("update Variables set VALUE = 'Feb'"));
		assertEquals(Sql.Kind.DML, Sql.classify("  insert into Cells values (1)"));
		assertEquals(Sql.Kind.DML, Sql.classify("/* clear */ DELETE FROM Cells"));
	}

	@Test
	void takesOffSemicolons() {
		assertEquals("select 1", Sql.withoutSemicolon("select 1;;  "));
	}

}
