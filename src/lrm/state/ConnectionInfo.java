package lrm.state;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionInfo {
	private static final String createClientStr = "INSERT INTO clients (X, Y, W, H, NAME) VALUES (?, ?, ?, ?, ?)";
	private static final String updateClientPosStr = "UPDATE clients SET X=?,Y=? WHERE ID=?";
	private static final String updateClientFullStr = "UPDATE clients SET X=?,Y=?,W=?,H=?,NAME=? WHERE ID=?";
	private static final String deleteClientStr = "DELETE FROM clients WHERE ID = ?";
	private static final String getStateStr = "SELECT * FROM states WHERE ID=? AND X>=? AND X<? AND Y>=? AND Y<?";
	private static final String setStateStr = "INSERT INTO states (ID,X,Y,VAL) VALUES (?,?,?,?) "
			+ "ON DUPLICATE KEY UPDATE VAL=?";
	private static final String setStateZeroStr = "DELETE FROM states WHERE ID=? AND X=? AND Y=?";

	public ConnectionInfo(Connection connection) throws SQLException {
		this.connection = connection;
		createClient = connection.prepareStatement(createClientStr, Statement.RETURN_GENERATED_KEYS);
		updateClientPos = connection.prepareStatement(updateClientPosStr);
		updateClientFull = connection.prepareStatement(updateClientFullStr);
		deleteClient = connection.prepareStatement(deleteClientStr);
		getState = connection.prepareStatement(getStateStr);
		setState = connection.prepareStatement(setStateStr);
		setStateZero = connection.prepareStatement(setStateZeroStr);
	}

	public final Connection connection;
	public final PreparedStatement createClient;
	public final PreparedStatement updateClientPos;
	public final PreparedStatement updateClientFull;
	public final PreparedStatement deleteClient;
	public final PreparedStatement getState;
	public final PreparedStatement setState;
	public final PreparedStatement setStateZero;
	
	//Returns id
	public int createClient(int x, int y, int w, int h, String name) {
		try {
			createClient.setInt(1, x);
			createClient.setInt(2, y);
			createClient.setInt(3, w);
			createClient.setInt(4, h);
			createClient.setString(5, name);

			createClient.executeUpdate();
			ResultSet rs = createClient.getGeneratedKeys();

			if (rs.next())
				return rs.getInt(1);
			else
				return -1;
		} catch (SQLException e) {
			System.out.println("SQL Error(createClient): " + e.getMessage());
		}

		return -1;
	}
	
	public boolean updateClient(int id, int x, int y) {
		try {
			updateClientPos.setInt(1, x);
			updateClientPos.setInt(2, y);
			updateClientPos.setInt(3, id);

			createClient.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println("SQL Error(updateClient(id,x,y)): " + e.getMessage());		
		}
		
		return false;
	}
}
