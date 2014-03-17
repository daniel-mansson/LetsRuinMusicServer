package lrm.verticles;

import java.sql.Time;
import java.util.TreeMap;

import lrm.socket.NetSocketInfo;
import lrm.socket.SocketInfo;
import lrm.socket.WebSocketInfo;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

public class ClientConnector extends Verticle {

	private EventBus eventBus;
	private HttpServer webServer;
	private NetServer netServer;
	private int nextSocketId;
	private TreeMap<Integer, SocketInfo> sockets;

	@Override
	public void start() {
		nextSocketId = 1;
		sockets = new TreeMap<>();

		eventBus = vertx.eventBus();

		JsonObject appConfig = container.config();

		int port;

		webServer = vertx.createHttpServer();
		webServer.websocketHandler(new WebConnectionHandler());

		try {
			port = Integer.parseInt(appConfig.getString("web_port"));
		}
		catch (Exception e) {
			port = 12001;
		}
		webServer.listen(port);
		System.out.println(Util.now() + " Web server listening to port " + port + ".");

		netServer = vertx.createNetServer();
		netServer.connectHandler(new NetConnectionHandler());

		try {
			port = Integer.parseInt(appConfig.getString("net_port"));
		}
		catch (Exception e) {
			port = 12002;
		}
		netServer.listen(Integer.parseInt(appConfig.getString("net_port")));
		System.out.println(Util.now() + " Net server listening to port " + port + ".");

		eventBus.registerHandler("out", new OutgoingDataHandler());

		final int stepIntervalTime = appConfig.getInteger("step_interval_time", 1000);
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(stepIntervalTime);
					} catch (InterruptedException e) {
					}
					
					eventBus.publish("step", true);
				}
			}
		}).start();
		

		System.out.println(Util.now() + " Client connector started.");
	}

	
	class WebConnectionHandler implements Handler<ServerWebSocket> {

		public WebConnectionHandler() {
		}

		@Override
		public void handle(ServerWebSocket socket) {
			final SocketInfo socketInfo = new WebSocketInfo(nextSocketId++, socket);
			sockets.put(socketInfo.getId(), socketInfo);
			
			socket.dataHandler(new Handler<Buffer>() {
				@Override
				public void handle(Buffer buffer) {
					try {
						JsonObject msg = new JsonObject(buffer.toString());
						msg.putNumber("id", socketInfo.getId());
						eventBus.publish("database", msg);
					}
					catch(DecodeException e) {
						socketInfo.close();
					}
				}
			});
			
			socket.closeHandler(new Handler<Void>() {				
				@Override
				public void handle(Void arg0) {

					JsonObject msg = new JsonObject();
					msg.putNumber("id", socketInfo.getId());
					msg.putBoolean("close", true);
					eventBus.publish("database", msg);
					
					sockets.remove(socketInfo.getId());
					System.out.println(Util.now() + " Web socket connection closed (id: " + socketInfo.getId() + ")");
				}
			});
			
			System.out.println(Util.now() + " Web socket connection opened (id: " + socketInfo.getId() + ")");
		}
	};

	class NetConnectionHandler implements Handler<NetSocket> {

		public NetConnectionHandler() {
		}

		@Override
		public void handle(NetSocket socket) {
			final SocketInfo socketInfo = new NetSocketInfo(nextSocketId++, socket);
			sockets.put(socketInfo.getId(), socketInfo);
			
			socket.dataHandler(new Handler<Buffer>() {
				@Override
				public void handle(Buffer buffer) {
					try {
						JsonObject msg = new JsonObject(buffer.toString());
						msg.putNumber("id", socketInfo.getId());
						eventBus.publish("database", msg);
					}
					catch(DecodeException e) {
						socketInfo.close();
					}
				}
			});
			
			socket.closeHandler(new Handler<Void>() {
				@Override
				public void handle(Void arg0) {
					JsonObject json = new JsonObject();
					json.putNumber("id", socketInfo.getId());
					json.putBoolean("close", true);		
					eventBus.publish("database", json);
					
					sockets.remove(socketInfo.getId());
				}
			});
		}
	}

	class OutgoingDataHandler implements Handler<Message<JsonObject>> {
		@Override
		public void handle(Message<JsonObject> msg) {
			JsonObject response = msg.body();
			int id = response.getNumber("id", -1).intValue();
			response.removeField("id");
			SocketInfo socket = sockets.get(id);
			if(socket != null) {
				socket.write(response.encode());
			}
		}
	}
}
