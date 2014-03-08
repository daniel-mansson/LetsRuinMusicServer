package lrm.socket;

import org.vertx.java.core.net.NetSocket;

public class NetSocketInfo extends SocketInfo{
	NetSocket socket;
	
	public NetSocketInfo(int id, NetSocket socket) {
		super(id);
		
		this.socket = socket;
	}
	
	@Override
	public void close() {
		socket.close();
	}
	
	@Override
	public void write(String data) {
		socket.write(data);
	}
}
