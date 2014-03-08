package lrm.socket;

import org.vertx.java.core.http.ServerWebSocket;

public class WebSocketInfo extends SocketInfo{
	ServerWebSocket socket;
	
	public WebSocketInfo(int id, ServerWebSocket socket) {
		super(id);
		
		this.socket = socket;
	}

	@Override
	public void close() {
		socket.close();
	}
	
	@Override
	public void write(String data) {
		socket.writeTextFrame(data);
	}
}
