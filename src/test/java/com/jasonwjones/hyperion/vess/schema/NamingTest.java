package com.jasonwjones.hyperion.vess.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.vess.Fixtures;

class NamingTest {

	@Test
	void makesIdentifiers() {
		Naming naming = Naming.DEFAULTS;
		assertEquals("PKG_TYPE", naming.identifier("Pkg Type"));
		assertEquals("VARIANCE", naming.identifier("Variance %"));
		assertEquals("_100_10", naming.identifier("100-10"));
		assertEquals("DIM_INTRO_DATE", naming.dimensionTable("Intro Date"));
		assertEquals("dim_pkg_type", new Naming(Naming.Style.LOWER, "DIM_").dimensionTable("Pkg Type"));
		assertEquals("Pkg Type", new Naming(Naming.Style.AS_IS, "").dimensionTable("Pkg Type"));
		assertEquals("Dim Product", new Naming(Naming.Style.AS_IS, "Dim ").dimensionTable("Product"));
	}

	@Test
	void addsASuffixWhenANameIsTaken() {
		Naming naming = Naming.DEFAULTS;
		Set<String> taken = new HashSet<>(Set.of("NAME"));
		assertEquals("NAME_GENERATION", naming.unique(taken, "Name", "Generation"));
		assertEquals("NAME_GENERATION_2", naming.unique(taken, "Name", "Generation"));
		assertEquals("CATEGORY", naming.unique(taken, "Category", "Generation"));
	}

	@Test
	void namesEachDimensionsTable() {
		Map<String, String> tables = CubeSchema.dimensionTables(Fixtures.sampleBasic(), Naming.DEFAULTS);
		assertEquals(List.of("DIM_YEAR", "DIM_MEASURES", "DIM_PRODUCT", "DIM_MARKET", "DIM_SCENARIO", "DIM_CAFFEINATED", "DIM_OUNCES",
				"DIM_PKG_TYPE", "DIM_POPULATION", "DIM_INTRO_DATE"), List.copyOf(tables.values()));
		// Without a prefix, a dimension can't take the name of the cube's own tables
		Map<String, String> bare = CubeSchema.dimensionTables(Fixtures.sampleBasic(), new Naming(Naming.Style.UPPER, ""));
		assertEquals("PRODUCT", bare.get("Product"));
	}

	@Test
	void readsStyles() {
		assertEquals(Naming.Style.AS_IS, Naming.Style.parse("as-is"));
		assertEquals(Naming.Style.AS_IS, Naming.Style.parse("AS_IS"));
		assertEquals(Naming.Style.LOWER, Naming.Style.parse(" Lower "));
		assertThrows(IllegalArgumentException.class, () -> Naming.Style.parse("title"));
	}

}
