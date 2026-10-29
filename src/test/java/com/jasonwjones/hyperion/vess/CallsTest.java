package com.jasonwjones.hyperion.vess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class CallsTest {

	@Test
	void readsTheProcedureAndItsArguments() throws VessException {
		assertEquals(new Calls.Parsed(List.of("Sample", "Basic", "CalcAll"), ""), Calls.parse("CALL Sample.Basic.CalcAll()"));
		assertEquals(new Calls.Parsed(List.of("CalcAll"), ""), Calls.parse("call CalcAll;"));
		assertEquals(new Calls.Parsed(List.of("RUN_CALC"), "'CALC ALL;'"), Calls.parse("{call RUN_CALC('CALC ALL;')}"));
		assertEquals(new Calls.Parsed(List.of("Sample", "Basic", "Calc Q1"), "?"), Calls.parse("CALL \"Sample\".\"Basic\".\"Calc Q1\"(?)"));
		assertEquals(new Calls.Parsed(List.of("Server", "START_APPLICATION"), "'Sample'"), Calls.parse("EXEC Server . START_APPLICATION ('Sample')"));
		assertEquals(new Calls.Parsed(List.of("Say \"hi\""), ""), Calls.parse("call \"Say \"\"hi\"\"\"()"));
	}

	@Test
	void saysWhatsWrong() {
		assertThrows(VessException.class, () -> Calls.parse("CALL"));
		assertThrows(VessException.class, () -> Calls.parse("CALL a.b.c.d()"));
		assertThrows(VessException.class, () -> Calls.parse("CALL CalcAll now"));
	}

}
