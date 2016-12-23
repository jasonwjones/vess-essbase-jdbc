package com.saxifrages.vess.h2.essconn;

import java.util.HashMap;
import java.util.Map;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.session.IEssbase;

public class EssConnectionManager {

	private static EssConnectionManager INSTANCE;

	/**
	 * Connections are organized by username
	 */
	private Map<String, IEssOlapServer> connections;

	private IEssbase essbase;

	public static EssConnectionManager getInstance() throws EssException {
		if (INSTANCE == null) {
			INSTANCE = new EssConnectionManager();
		}
		return INSTANCE;
	}

	/**
	 * getInstance() should be used to get the singelton instance
	 * 
	 * @throws EssException
	 */
	private EssConnectionManager() throws EssException {
		System.out.println("Calling IEssbase.Home");
		essbase = IEssbase.Home.create(IEssbase.JAPI_VERSION);
		connections = new HashMap<String, IEssOlapServer>();
	}

	/** 
	 * Convenience method meant to provide a backdoor strategy to get to a
	 * connection from a static method, such as when a function/alias is used in
	 * H2 and it's only passed a java.sql.Connection (which is more or less
	 * useless).
	 * 
	 * @param username
	 *            the Essbase username that was used to connect to Essbase.
	 *            Case-sensitive
	 * @return the IEssOlapServer associated with that user. Should not be
	 *         closed
	 */
	public IEssOlapServer getForUser(String username) {
		if (connections.containsKey(username)) {
			return connections.get(username);
		} else {
			throw new RuntimeException("There is no connection for user " + username);
		}
	}

	public IEssOlapServer getServer(String serverName, String username, String password) throws EssException {
		if (connections.containsKey(username)) {
			System.out.println("Reusing Essbase connection for user " + username);
			IEssOlapServer server = connections.get(username);
			if (server.isConnected()) {
				return server;
			} else {
				server.connect();
				return server;
			}
		} else {
			System.out.println("Signing on to " + serverName);
			IEssOlapServer server = essbase.signOn(username, password, false, null, "embedded",
					serverName);
			connections.put(username, server);
			return server;
		}
	}

}
