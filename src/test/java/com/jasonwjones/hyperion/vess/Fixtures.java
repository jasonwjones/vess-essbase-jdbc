package com.jasonwjones.hyperion.vess;

import java.io.IOException;
import java.io.InputStream;

import com.jasonwjones.hyperion.vess.outline.Outline;
import com.jasonwjones.hyperion.vess.outline.OutlineParser;

/**
 * What the tests read instead of a server: Sample.Basic's outline, as Essbase
 * 26.1 exports it.
 */
public final class Fixtures {

	private Fixtures() {
	}

	public static Outline sampleBasic() {
		try (InputStream in = Fixtures.class.getResourceAsStream("/sample-basic-outline.xml")) {
			return OutlineParser.parse("Sample", "Basic", in);
		} catch (IOException | javax.xml.stream.XMLStreamException e) {
			throw new IllegalStateException("Could not read the Sample.Basic outline: " + e.getMessage(), e);
		}
	}

}
