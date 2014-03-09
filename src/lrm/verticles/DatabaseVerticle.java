package lrm.verticles;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TreeMap;

import lrm.state.ClientState;
import lrm.state.ConnectionInfo;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class DatabaseVerticle extends Verticle {

	private Connection connection;
	private ConnectionInfo connectionInfo;
	private TreeMap<Integer, ClientState> clientStates;
	private EventBus eventBus;

	public DatabaseVerticle() {

	}

	@Override
	public void start() {
		clientStates = new TreeMap<>();
		//JsonObject config = container.config();
		JsonObject config = new JsonObject();

		try {
			Class.forName(config.getString("database_driver", "com.mysql.jdbc.Driver"));
		} catch (ClassNotFoundException e1) {
			System.err.println(e1.getMessage());
		}
		
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

		String dburl = config.getString("database_url", "jdbc:mysql://localhost:3306/lrm");
		try {
			connection = DriverManager.getConnection(dburl, dbprop);
		}
		catch (SQLException e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		try {
			connectionInfo = new ConnectionInfo(connection);
			
		} catch (SQLException | IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
		

		eventBus = vertx.eventBus();
		eventBus.registerHandler("database", new IncomingDataHandler());
		eventBus.registerHandler("step", new StepHandler());

		System.out.println("database verticle started");
	}

	class IncomingDataHandler implements Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> message) {
			JsonObject body = message.body();
			int id = body.getInteger("id", -1);

			if(body.getBoolean("close", false)){
				if(id >= 0) {
					clientStates.get(id).destroy();
					clientStates.remove(id);
				}
				return;
			}
			
			if (id >= 0) {
				ClientState state = clientStates.get(id);
				
				if (state == null) {
					state = new ClientState(id, body, connectionInfo);
					clientStates.put(id, state);
				}

				state.performUpdate(body);
			}
			else {
				JsonObject error = new JsonObject();
				error.putNumber("id", id);
				error.putString("error", "Invalid client id.");

				eventBus.publish("out", error);
			}
		}
	}

	class StepHandler implements Handler<Message<Boolean>> {

		@Override
		public void handle(Message<Boolean> arg0) {

			for(ClientState state : clientStates.values()) {
				JsonObject msg = state.getDiff();
				if(msg != null)
					eventBus.publish("out", msg);
			}
		}
	}
}
