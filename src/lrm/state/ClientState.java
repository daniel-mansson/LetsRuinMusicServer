package lrm.state;

import java.util.TreeSet;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ClientState {
	private int id;
	private int databaseId;
	private ConnectionInfo connectionInfo;

	private boolean initialized;
	private int x;
	private int y;
	private int w;
	private int h;
	private String name;

	public ClientState(int id, JsonObject body, ConnectionInfo connectionInfo) {
		this.id = id;
		initialized = false;
		
		x = 0;
		y = 0;
		w = 16;
		h = 16;

		this.connectionInfo = connectionInfo;
		name = body.getString("name", "unnamed");

		connectionInfo.openConnection();
		try {
			databaseId = connectionInfo.createClient(x, y, w, h, name);
		} finally {
			connectionInfo.closeConnection();
		}
	}

	public void performUpdate(JsonObject body) {
		JsonArray data = body.getArray("data");
		JsonObject view = body.getObject("view");
		String name = body.getString("name");
		
		connectionInfo.openConnection();
		try {
			if(view != null) {
				x = view.getInteger("x");
				y = view.getInteger("y");
				w = view.getInteger("w");
				h = view.getInteger("h");
				
				connectionInfo.updateClient(databaseId, x, y, w, h);
			}
			
			if(name != null){
				this.name = name; 
				connectionInfo.updateClient(databaseId, name);
			}

			if (data != null) {
				for (int i = 0; i < data.size(); ++i) {
					JsonObject obj = data.get(i);

					int x = obj.getInteger("x");
					int y = obj.getInteger("y");
					int value = obj.getInteger("v");

					connectionInfo.setState(0, x, y, value);
				}
			}
		} finally {
			connectionInfo.closeConnection();
		}

		initialized = true;
	}

	public JsonObject getDiff() {
		if (!initialized)
			return null;

		JsonObject response = new JsonObject();
		response.putNumber("id", id);

		JsonArray data = new JsonArray();
		JsonObject cell;

		connectionInfo.openConnection();
		try {
			TreeSet<Cell> diff = connectionInfo.getGlobalDiff(databaseId);

			if (diff.size() == 0)
				return null;

			for (Cell c : diff) {
				cell = new JsonObject();

				int x = c.getX();
				int y = c.getY();
				int v = c.getValue();

				cell.putNumber("x", x);
				cell.putNumber("y", y);
				cell.putNumber("v", v);
				data.add(cell);

				connectionInfo.setState(databaseId, x, y, v);
			}

			response.putArray("data", data);
			
			
		} finally {
			connectionInfo.closeConnection();
		}
		
		return response;
	}
	
	public JsonObject getView() {
		JsonObject view = new JsonObject();

		view.putNumber("x", x);
		view.putNumber("y", y);
		view.putNumber("w", w);
		view.putNumber("h", h);
		
		return view;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public void destroy() {
		connectionInfo.openConnection();
		try {
			connectionInfo.deleteClient(databaseId);
		} finally {
			connectionInfo.closeConnection();
		}
	}
}
