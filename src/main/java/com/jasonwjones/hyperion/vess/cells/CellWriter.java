package com.jasonwjones.hyperion.vess.cells;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appliedolap.essbase.client.ApiClient;
import com.appliedolap.essbase.client.ApiException;
import com.appliedolap.essbase.util.NativeHttp;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonwjones.hyperion.vess.VessException;
import com.jasonwjones.hyperion.vess.schema.ServerContext;

/**
 * Writes values into a cube with Essbase's streaming data load, which takes
 * records of a member from every dimension and a value, with no load rule.
 * A value of null clears the cell.
 *
 * <p>
 * The requests are made directly rather than through the generated client,
 * which expects a body from pushing data that the server doesn't always send.
 * A stream belongs to the session that started it, which the client keeps.
 */
final class CellWriter {

	private static final Logger logger = LoggerFactory.getLogger(CellWriter.class);

	private static final ObjectMapper JSON = new ObjectMapper();

	/** How many records go in each request to the server */
	private static final int BATCH = 5_000;

	private CellWriter() {
	}

	/**
	 * @param records each a member of every dimension, then the value or null
	 *            to clear it
	 */
	static void write(ServerContext context, String application, String cube, List<Object[]> records) throws SQLException {
		if (records.isEmpty()) {
			return;
		}
		ApiClient client = context.api().getClient();
		String path = "/applications/" + ApiClient.urlEncode(application) + "/databases/" + ApiClient.urlEncode(cube) + "/dataload";
		String stream = null;
		try {
			String started = NativeHttp.sendForString(client, NativeHttp.request(client, path).header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{}")), "dataloadStart");
			stream = field(started, "streamId");
			if (stream == null) {
				throw new VessException("Essbase didn't start a data load into " + application + "." + cube + ": " + started);
			}
			StringBuilder batch = new StringBuilder();
			int count = 0;
			for (Object[] record : records) {
				batch.append(line(record)).append('\n');
				if (++count % BATCH == 0) {
					push(client, path + "/" + stream, batch);
				}
			}
			if (batch.length() > 0) {
				push(client, path + "/" + stream, batch);
			}
			String ended = end(client, path + "/" + stream);
			stream = null;
			String status = field(ended, "status");
			logger.info("Loaded {} values into {}.{}: {}", records.size(), application, cube, status);
			if (status != null && !status.toLowerCase(Locale.ROOT).contains("success")) {
				throw new VessException("Essbase didn't load all of the values into " + application + "." + cube + ": " + status);
			}
		} catch (ApiException e) {
			throw new VessException("Could not write to " + application + "." + cube + ": " + message(e), e);
		} catch (IOException e) {
			throw new VessException("Could not write to " + application + "." + cube + ": " + e, e);
		} finally {
			if (stream != null) {
				try {
					end(client, path + "/" + stream);
				} catch (ApiException | IOException | RuntimeException e) {
					logger.debug("Could not end the data load stream {}: {}", stream, e.toString());
				}
			}
		}
	}

	private static void push(ApiClient client, String path, StringBuilder batch) throws ApiException, IOException {
		NativeHttp.sendAndDiscard(client,
				NativeHttp.request(client, path).header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString(batch.toString())),
				"dataloadStreamData");
		batch.setLength(0);
	}

	private static String end(ApiClient client, String path) throws ApiException, IOException {
		return NativeHttp.sendForString(client, NativeHttp.request(client, path).DELETE(), "dataloadEnd");
	}

	/**
	 * A field of a JSON answer, or null if it isn't there.
	 */
	static String field(String json, String name) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			Object value = JSON.readValue(json, Map.class).get(name);
			return value == null ? null : value.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * A record as Essbase reads one without a load rule: each member in double
	 * quotes, then the value, or #Missing to clear it.
	 */
	static String line(Object[] record) {
		StringBuilder line = new StringBuilder();
		for (int index = 0; index < record.length - 1; index++) {
			line.append('"').append(String.valueOf(record[index]).replace("\"", "")).append("\" ");
		}
		Object value = record[record.length - 1];
		if (value == null) {
			line.append("#Missing");
		} else {
			double number = ((Number) value).doubleValue();
			line.append(number == Math.rint(number) && !Double.isInfinite(number) && Math.abs(number) < 1e15 ? Long.toString((long) number)
					: Double.toString(number));
		}
		return line.toString();
	}

	private static String message(ApiException e) {
		if (e.getResponseBody() != null && !e.getResponseBody().isBlank()) {
			String error = field(e.getResponseBody(), "errorMessage");
			return error != null ? error : e.getResponseBody();
		}
		return e.getMessage();
	}

}
