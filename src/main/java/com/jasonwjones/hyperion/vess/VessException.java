package com.jasonwjones.hyperion.vess;

import java.sql.SQLException;

/**
 * Something Vess couldn't do, with a message that says what and, where it can,
 * what to do instead.
 */
public class VessException extends SQLException {

	private static final long serialVersionUID = 1L;

	public VessException(String message) {
		super(message);
	}

	public VessException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * A failure inside the SQL engine, where only unchecked exceptions get
	 * through; the engine reports its message.
	 */
	public static class Unchecked extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public Unchecked(SQLException cause) {
			super(cause.getMessage(), cause);
		}

		public Unchecked(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
