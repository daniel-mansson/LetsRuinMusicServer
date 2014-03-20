package lrm.verticles;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import lrm.state.ClientState;
import lrm.state.ConnectionInfo;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class DatabaseVerticle extends Verticle {

	private ConnectionInfo connectionInfo;
	private TreeMap<Integer, ClientState> clientStates;
	private EventBus eventBus;
	private boolean anyChange;
	private BoneCP connectionP;
	private TreeSet<Integer> clientsViewChanged;
	private TreeSet<Integer> clientsNameChanged;
	private TreeSet<Integer> clientsDead;

	public DatabaseVerticle() {

	}

	@Override
	public void start() {
		clientsViewChanged = new TreeSet<>();
		clientsNameChanged = new TreeSet<>();
		clientsDead = new TreeSet<>();
		clientStates = new TreeMap<>();
		anyChange = true;
		JsonObject config = container.config();

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

		BoneCPConfig connectionConfig = new BoneCPConfig();
		connectionConfig.setJdbcUrl(config.getString("database_url", "jdbc:mysql://localhost:3306/lrm")); 

		connectionConfig.setUsername(config.getString("database_user", "sa"));
		connectionConfig.setPassword(config.getString("database_password", ""));
		connectionConfig.setMinConnectionsPerPartition(2);
		connectionConfig.setMaxConnectionsPerPartition(5);
		connectionConfig.setPartitionCount(1);
		
		try {
			connectionP = new BoneCP(connectionConfig);
		} catch (SQLException e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		
		try {
			Connection connection = connectionP.getConnection();
			Statement s = connection.createStatement();
			s.execute("CREATE TABLE IF NOT EXISTS states (ID INT NOT NULL, X INT NOT NULL, Y INT NOT NULL, VAL INT NOT NULL, PRIMARY KEY(ID, X, Y))");
			s.execute("CREATE TABLE IF NOT EXISTS clients (ID INT NOT NULL PRIMARY KEY AUTO_INCREMENT, X INT, Y INT, W INT, H INT, NAME VARCHAR(64))");
			s.execute("INSERT INTO clients VALUES (1,0,0,0,0,\"Global\") ON DUPLICATE KEY UPDATE ID=1");
			connection.close();

		} catch (SQLException e) {
			System.err.println("Error: " + e.getMessage());
		}

		try {
			connectionInfo = new ConnectionInfo(connectionP);
			
		} catch (SQLException | IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		eventBus = vertx.eventBus();
		eventBus.registerHandler("database", new IncomingDataHandler());
		eventBus.registerHandler("step", new StepHandler());

		System.out.println(Util.now() + " Database verticle started.");
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
					
					clientsDead.add(id);
					anyChange = true;
				}
				return;
			}
			
			if (id >= 0) {
				ClientState state = clientStates.get(id);

				boolean isNew = false;
				anyChange = true;
				if (state == null) {
					state = new ClientState(id, body, connectionInfo);
					clientStates.put(id, state);
					isNew = true;
				}
				state.performUpdate(body);

				if(body.getObject("view") != null)	
					clientsViewChanged.add(id);

				if(body.getString("name") != null)	
					clientsNameChanged.add(id);		
				
				if(isNew) {
					welcome(state);
				}
			}
			else {
				JsonObject error = new JsonObject();
				error.putNumber("id", id);
				error.putString("error", "Invalid client id.");

				eventBus.publish("out", error);
			}
		}
	}
	
	private void welcome(ClientState state) {
		int id = state.getId();
		
		JsonObject msg = new JsonObject();
		JsonArray clients = new JsonArray();
		
		for(ClientState s : clientStates.values()) {
			JsonObject entry  = new JsonObject();
			entry.putNumber("id", s.getId());
			entry.putString("name", s.getName());
			entry.putObject("view", s.getView());
			clients.add(entry);
		}

		msg.putNumber("id", id);
		msg.putNumber("your_id", id);
		msg.putArray("clients", clients);
		eventBus.publish("out", msg);		
	}

	class StepHandler implements Handler<Message<Boolean>> {

		@Override
		public void handle(Message<Boolean> arg0) {

			if(!anyChange)
				return;
			
			JsonArray clients = new JsonArray();

			for(ClientState state : clientStates.values()) {		
				JsonObject entry = new JsonObject();
				boolean hasData = false;
				
				entry.putNumber("id", state.getId());
				if(clientsNameChanged.contains(state.getId())) {
					entry.putString("name", state.getName());
					hasData = true;
				}
				if(clientsViewChanged.contains(state.getId())) {
					entry.putObject("view", state.getView());
					hasData = true;
				}
				
				if(hasData) {
					clients.add(entry);
				}
			}
			
			for(Integer i : clientsDead) {
				JsonObject entry = new JsonObject();
				entry.putNumber("id", i);
				entry.putBoolean("dead", true);
				clients.add(entry);
			}
			
			clientsDead.clear();
			clientsViewChanged.clear();
			clientsNameChanged.clear();
			
			for(ClientState state : clientStates.values()) {

				JsonObject msg = state.getDiff();
				
				if(msg != null) {
					if(clients.size() > 0)
						msg.putArray("clients", clients);
					
					eventBus.publish("out", msg);
				}
				else if(clients.size() > 0){
					msg = new JsonObject();
					msg.putNumber("id", state.getId());
					msg.putArray("clients", clients);
					eventBus.publish("out", msg);
				}
			}

			anyChange = false;
		}
	}
}
