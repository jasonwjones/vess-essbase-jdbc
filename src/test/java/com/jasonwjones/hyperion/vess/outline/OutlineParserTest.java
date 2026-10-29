package com.jasonwjones.hyperion.vess.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.vess.Fixtures;

class OutlineParserTest {

	private final Outline outline = Fixtures.sampleBasic();

	@Test
	void readsTheDimensionsInOrder() {
		assertEquals(List.of("Year", "Measures", "Product", "Market", "Scenario", "Caffeinated", "Ounces", "Pkg Type", "Population", "Intro Date"),
				outline.getDimensions().stream().map(Dimension::getName).toList());
		assertEquals(5, outline.getStandardDimensions().size());
		assertEquals("BSO", outline.getCubeType());
	}

	@Test
	void readsWhatEachDimensionIs() {
		Dimension year = outline.getDimension("year");
		assertEquals(Dimension.TIME, year.getType());
		assertEquals("Dense", year.getDensity());
		assertEquals(Dimension.ACCOUNTS, outline.getDimension("Measures").getType());
		assertEquals("Sparse", outline.getDimension("Product").getDensity());
		Dimension ounces = outline.getDimension("Ounces");
		assertTrue(ounces.isAttribute());
		assertEquals("Numeric", ounces.getAttributeType());
		assertEquals("Product", ounces.getBaseDimension());
		assertEquals("Market", outline.getDimension("Population").getBaseDimension());
		assertEquals(List.of("Caffeinated", "Ounces", "Pkg Type", "Intro Date"), outline.getDimension("Product").getAttributeDimensions());
	}

	@Test
	void countsLevelsAsEssbaseDoes() {
		// A parent is one above the highest of its children, so a ragged
		// dimension's top is as high as its deepest branch
		assertEquals(3, outline.getMember("Measures").getLevel());
		assertEquals(2, outline.getMember("Profit").getLevel());
		assertEquals(1, outline.getMember("Inventory").getLevel());
		assertEquals(0, outline.getMember("Sales").getLevel());
		assertEquals(1, outline.getMember("Measures").getGeneration());
		assertEquals(3, outline.getMember("Margin").getGeneration());
	}

	@Test
	void readsMemberProperties() {
		Member cola = outline.getMember("100-10");
		assertEquals("Cola", cola.getAlias("Default"));
		assertEquals("Cola", cola.getAlias("GermanNames"));
		assertEquals("True", cola.getAttributes().get("Caffeinated"));
		assertEquals("12", cola.getAttributes().get("Ounces"));
		assertEquals("100", cola.getParent().getName());
		assertTrue(cola.isStored());

		Member opening = outline.getMember("Opening Inventory");
		assertEquals("First", opening.getTimeBalance());
		assertEquals("Expense", opening.getVarianceReporting());
		assertTrue(opening.getFormula().contains("@PRIOR"));

		Member qtr1 = outline.getMember("Qtr1");
		assertEquals(Member.Storage.DYNAMIC_CALC, qtr1.getStorage());
		assertFalse(qtr1.isStored());

		assertTrue(outline.getMember("New York").getUdas().contains("Major Market"));
		assertEquals("~", outline.getMember("Ratios").getConsolidation());
	}

	@Test
	void keepsSharedMembersApart() {
		List<Member> places = outline.getPositions("100-20");
		assertEquals(2, places.size());
		assertFalse(places.get(0).isShared());
		assertTrue(places.get(1).isShared());
		assertEquals("Diet", places.get(1).getParent().getName());
		assertEquals("100-20", places.get(1).getBaseMember());
		// Looking a member up finds where it's stored
		assertEquals("100", outline.getMember("100-20").getParent().getName());
		Dimension product = outline.getDimension("Product");
		assertEquals(22, product.getMembers().size());
		assertEquals(19, product.getBaseMembers().size());
	}

	@Test
	void namesGenerations() {
		Dimension product = outline.getDimension("Product");
		assertEquals("Category", product.getGenerationName(2));
		assertEquals("Product SKU", product.getGenerationName(3));
		assertNull(product.getGenerationName(4));
		assertEquals(3, product.getGenerations());
		assertEquals("100", outline.getMember("100-10").getAncestor(2).getName());
		assertNull(outline.getMember("100").getAncestor(3));
	}

	@Test
	void looksUpMembersWithoutRegardToCase() {
		assertEquals("New York", outline.getMember("new york").getName());
		assertNull(outline.getMember("Nowhere"));
		assertEquals(List.of("Default", "Long Names", "ChineseNames", "JapaneseNames", "RussianNames", "GermanNames"), outline.getAliasTables());
		assertEquals("Default", outline.getDefaultAliasTable());
	}

	@Test
	void ordersMembersAcrossTheOutline() {
		assertTrue(outline.getMember("Year").getOrder() < outline.getMember("Qtr1").getOrder());
		assertTrue(outline.getMember("Qtr1").getOrder() < outline.getMember("Jan").getOrder());
		assertTrue(outline.getMember("Jan").getOrder() < outline.getMember("Measures").getOrder());
	}

}
