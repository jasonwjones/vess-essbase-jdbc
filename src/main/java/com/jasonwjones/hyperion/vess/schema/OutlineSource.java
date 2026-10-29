package com.jasonwjones.hyperion.vess.schema;

import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.outline.Outline;

/**
 * Where a cube's outline comes from: the connection's cache of what it read
 * from the server, or for tests, a file.
 */
@FunctionalInterface
public interface OutlineSource {

	Outline outline() throws VessException;

}
