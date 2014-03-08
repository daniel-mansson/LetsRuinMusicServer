package lrm.verticles;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lrm.state.ClientState;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class DatabaseVerticle extends Verticle {

	private Connection connection;
	private static ConcurrentMap<Integer, ClientState> clientStates = new ConcurrentHashMap<>();
	private EventBus eventBus;

	public DatabaseVerticle() {

	}

	@Override
	public void start() {
		JsonObject config = container.config();
		eventBus = vertx.eventBus();

		Properties dbprop = new Properties();
		try {
			dbprop.load(new FileInputStream(config.getString("database_config", "config/database.properties")));
		}
		catch (FileNotFoundException e) {
			System.err.println("Error: " + e.getMessage());
		}
		catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}

		String dburl = config.getString("database_url");
		try {
			connection = DriverManager.getConnection(dburl, dbprop);
		}
		catch (SQLException e) {
			System.err.println("Error: " + e.getMessage());
		}

		eventBus.registerHandler("database", new IncomingDataHandler());

		System.out.println("database verticle started");
	}

	class IncomingDataHandler implements Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> message) {
			JsonObject body = message.body();
			int id = body.getInteger("id", -1);

			if(body.getBoolean("close", false)){
				if(id >= 0)
					clientStates.remove(id);
				return;
			}
			
			if (id >= 0) {
				ClientState state = clientStates.get(id);
				
				if (state == null) {
					state = new ClientState(id);
					clientStates.put(id, state);
				}

				JsonObject response = state.performRequest(body, connection);
				eventBus.publish("out", response);
			}
			else {
				JsonObject error = new JsonObject();
				error.putNumber("id", id);
				error.putString("error", "Invalid client id.");

				eventBus.publish("out", error);
			}
		}
	}
}
