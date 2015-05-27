package com.saxifrages.vess.util;

import java.util.Properties;
import java.util.Set;

public class EssbaseUrlProperties {
	
	/**
	 * Understands:
	 * 
	 *    map=Sample.Basic;ASOSamp.Sample
	 * @param properties
	 */
	public EssbaseUrlProperties(Properties properties) {
	}
	
	public Set<MapDatabase> getMapDatabases() {
		return null;
	}
	
	public void parseMapList(String mapList) {
		String[] cubes = mapList.split(",");
		for (String cube : cubes) {
			
		}
	}
	
	public static class MapDatabase {
		
		private String application;
		
		private String cube;
		
		public String getApplication() {
			return application;
		}
		
		public String getCube() {
			return cube;
		}
		
	}
	
}
