package com.jasonwjones.hyperion.vess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.jasonwjones.hyperion.vess.schema.ServerContext;

class VessUrlTest {

	@Test
	void readsTheServerAndSettings() throws VessException {
		VessUrl url = VessUrl.parse("jdbc:essbase:vess:http://localhost:9000/essbase/;app=Sample; cube = Basic ;");
		assertEquals("http://localhost:9000/essbase", url.getServerUrl());
		assertEquals(Map.of("app", "Sample", "cube", "Basic"), url.getProperties());
		assertTrue(VessUrl.accepts("jdbc:essbase:vess:https://essbase.example.com/essbase"));
		assertFalse(VessUrl.accepts("jdbc:essbase:thriller:http://localhost:9000/essbase"));
	}

	@Test
	void saysWhatsWrong() {
		assertThrows(VessException.class, () -> VessUrl.parse("jdbc:essbase:vess:localhost:9000"));
		assertThrows(VessException.class, () -> VessUrl.parse("jdbc:essbase:vess:http://localhost:9000/essbase;app"));
	}

	@Test
	void urlSettingsWinOverProperties() throws VessException {
		Properties info = new Properties();
		info.setProperty("user", "admin");
		info.setProperty("password", "secret");
		info.setProperty("app", "Demo");
		info.setProperty("readonly", "true");
		Map<String, String> settings = VessConnection.settings(VessUrl.parse("jdbc:essbase:vess:http://localhost:9000/essbase;app=Sample"), info);
		assertEquals("Sample", settings.get("app"));
		assertEquals("true", settings.get("readonly"));
		assertFalse(settings.containsKey("password"));
	}

	@Test
	void readsTuningSettings() throws VessException {
		ServerContext.Settings settings = VessConnection.contextSettings(Map.of("outline.cache", "60", "chunk.cells", "1000", "server.catalog", "System"));
		assertEquals(Duration.ofMinutes(1), settings.outlineCache());
		assertEquals(1000, settings.chunkCells());
		assertEquals(2000, settings.chunkMembers());
		assertEquals("System", settings.serverCatalog());
		assertThrows(VessException.class, () -> VessConnection.contextSettings(Map.of("chunk.cells", "lots")));
		assertThrows(VessException.class, () -> VessConnection.contextSettings(Map.of("chunk.members", "0")));
	}

}
