package lrm.socket;

public abstract class SocketInfo implements Comparable<SocketInfo>{
	private int id;
	
	public SocketInfo(int id) {
		this.id = id;
	}

	abstract public void write(String data);
	abstract public void close();
	
	public int getId() {
		return id;
	}
	
	
	@Override
	public int compareTo(SocketInfo o) {
		return o.getId() - id;
	}
}
