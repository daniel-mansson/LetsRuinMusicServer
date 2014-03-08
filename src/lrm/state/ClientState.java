package lrm.state;

import java.sql.Connection;
import java.util.Random;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ClientState {
	private int id;

	public ClientState(int id) {
		this.id = id;
	}

	public JsonObject performRequest(JsonObject body, Connection connection) {
		JsonObject response = new JsonObject();
		response.putNumber("id", id);

		JsonArray data = new JsonArray();
		JsonObject cell;

		Random random = new Random();
		for (int i = 0; i < 20; ++i) {
			cell = new JsonObject();
			cell.putNumber("x", 16 + random.nextInt(16));
			cell.putNumber("y", 16 + random.nextInt(16));
			cell.putNumber("v", 1);
			data.add(cell);
		}

		response.putArray("data", data);

		return response;
	}

}
