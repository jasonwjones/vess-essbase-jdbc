package com.jasonwjones.hyperion.vess.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.impl.AbstractSchema;

import com.appliedolap.essbase.EssApiException;
import com.appliedolap.essbase.EssCube;
import com.jasonwjones.hyperion.vess.VessException;

/**
 * An application, which holds a schema for each of its cubes. It's a catalog
 * to tools, so that Sample.Basic.CELLS reads as the cube it is.
 */
public final class ApplicationSchema extends AbstractSchema {

	private final ServerContext context;

	private final String name;

	private Map<String, CubeSchema> cubes;

	public ApplicationSchema(ServerContext context, String name) {
		this.context = context;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	protected Map<String, Schema> getSubSchemaMap() {
		return Collections.unmodifiableMap(cubes());
	}

	/**
	 * @return the application's cubes, read the first time they're wanted
	 */
	public synchronized Map<String, CubeSchema> cubes() {
		if (cubes == null) {
			Map<String, CubeSchema> map = new LinkedHashMap<>();
			try {
				for (EssCube cube : context.application(name).getCubes()) {
					map.put(cube.getName(), new CubeSchema(context, name, cube.getName()));
				}
			} catch (VessException e) {
				throw new VessException.Unchecked(e);
			} catch (EssApiException e) {
				throw new VessException.Unchecked("Could not list the cubes of " + name + ": " + e.getMessage(), e);
			}
			cubes = map;
		}
		return cubes;
	}

	public List<CubeSchema> getCubeSchemas() {
		return new ArrayList<>(cubes().values());
	}

	/**
	 * @return the cube of that name, in any case, or null
	 */
	public CubeSchema cube(String cube) {
		for (Map.Entry<String, CubeSchema> entry : cubes().entrySet()) {
			if (entry.getKey().equalsIgnoreCase(cube)) {
				return entry.getValue();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return name;
	}

}
