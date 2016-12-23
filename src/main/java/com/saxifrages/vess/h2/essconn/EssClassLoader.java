package com.saxifrages.vess.h2.essconn;

import java.net.URL;
import java.net.URLClassLoader;

public class EssClassLoader extends URLClassLoader {

	public EssClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		System.out.println("Initialized EssClassLoader!");
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		System.out.println("Called EssClassLoader's findClass()");
		return super.findClass(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		System.out.println("Called EssClassLoader's loadClass(): " + name);
		Class<?> found = super.loadClass(name);
		System.out.println("Got back: " + found);
		return found;
	}
	

}
