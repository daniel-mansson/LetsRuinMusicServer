package lrm.state;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeSet;

import com.jolbox.bonecp.BoneCP;

public class ConnectionInfo {
	private static final String createClientStr = "INSERT INTO clients (X, Y, W, H, NAME) VALUES (?, ?, ?, ?, ?)";
	private static final String updateClientPosStr = "UPDATE clients SET X=?,Y=? WHERE ID=?";
	//private static final String updateClientFullStr = "UPDATE clients SET X=?,Y=?,W=?,H=?,NAME=? WHERE ID=?";
	private static final String deleteClientStr = "DELETE FROM clients WHERE ID = ?";
	private static final String getStateStr = "SELECT (X,Y,VAL) FROM states WHERE ID=? AND X>=? AND X<? AND Y>=? AND Y<?";
	//private static final String getStateDiffStr = "SELECT (X,Y,VAL) FROM states WHERE ID=? AND X>=? AND X<? AND Y>=? AND Y<?";
	private static final String setStateStr = "INSERT INTO states (ID,X,Y,VAL) VALUES (?,?,?,?) "
			+ "ON DUPLICATE KEY UPDATE VAL=?";
	private static final String setStateZeroStr = "DELETE FROM states WHERE ID=? AND X=? AND Y=?";

	private String getGlobalStateDiffStr;

	public final BoneCP connectionPool;
	public Connection connection;
	
	public ConnectionInfo(BoneCP connectionPool) throws SQLException, IOException {
		this.connectionPool = connectionPool;
		
		byte[] encoded = Files.readAllBytes(Paths.get("config/globaldiff.sql"));
		getGlobalStateDiffStr = Charset.defaultCharset()
				.decode(ByteBuffer.wrap(encoded)).toString();
		
	}
	
	public void openConnection() {
		if(connection == null) {
			try {
				connection = connectionPool.getConnection();
			} catch (SQLException e) {
				System.out.println("SQL Error(openConnection): " + e.getMessage());
				connection = null;
			} 
		}
		else {
			System.out.println("Logic error(openConnection): " + "Trying to open a new connection when one already exists.");
		}
	}
	
	public void closeConnection() {
		if(connection != null) {
			try {
				connection.close();
				connection = null;
			} catch (SQLException e) {
				System.out.println("SQL Error(closeConnection): " + e.getMessage());
			} 
		}
		else {
			System.out.println("Logic error(openConnection): " + "Trying to close a connection when there is none.");
		}
	}
	
	// Returns id
	public int createClient(int x, int y, int w, int h, String name) {
		PreparedStatement createClient;
		try {
			createClient = connection.prepareStatement(createClientStr,
					Statement.RETURN_GENERATED_KEYS);

		} catch (SQLException e) {
			System.out.println("SQL Error(createClient): " + e.getMessage());
			return -1;
		}
		
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
		} finally {
			try {
				createClient.close();
			} catch (SQLException e) {
				System.out.println("SQL Error(createClient): " + e.getMessage());
			}
		}
		
		return -1;
	}

	public boolean updateClient(int id, int x, int y) {
		

		PreparedStatement updateClientPos;
		try {
			updateClientPos = connection.prepareStatement(updateClientPosStr);
		} catch (SQLException e) {
			System.out.println("SQL Error(updateClient(id,x,y)): " + "(" + id + "," + x + "," + y + ")  "
					+ e.getMessage());
			return false;
		}
		
		try {
			
			updateClientPos.setInt(1, x);
			updateClientPos.setInt(2, y);
			updateClientPos.setInt(3, id);

			updateClientPos.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println("SQL Error(updateClient(id,x,y)): " + "(" + id + "," + x + "," + y + ")  "
					+ e.getMessage());
		} finally {
			try {
				updateClientPos.close();
			} catch (SQLException e) {
				System.out.println("SQL Error(updateClient(id,x,y)): " + "(" + id + "," + x + "," + y + ")  "
						+ e.getMessage());
			}
		}

		return false;
	}

	public boolean updateClient(int id, int x, int y, int w, int h, String name) {
		
		PreparedStatement updateClientPos;
		try {
			updateClientPos = connection.prepareStatement(updateClientPosStr);
		} catch (SQLException e) {
			System.out.println("SQL Error(updateClient(id,x,y)): " + "(" + id
					+ "," + x + "," + y + "," + w + "," + h + "," + name
					+ ")  " + e.getMessage());
			return false;
		}
		
		try {
			updateClientPos.setInt(1, x);
			updateClientPos.setInt(2, y);
			updateClientPos.setInt(3, w);
			updateClientPos.setInt(4, h);
			updateClientPos.setString(5, name);
			updateClientPos.setInt(6, id);

			updateClientPos.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println("SQL Error(updateClient(id,x,y)): " + "(" + id
					+ "," + x + "," + y + "," + w + "," + h + "," + name
					+ ")  " + e.getMessage());
		} finally {
			try {
				updateClientPos.close();
			} catch (SQLException e) {
				System.out.println("SQL Error(updateClient(id,x,y)): " + "(" + id
						+ "," + x + "," + y + "," + w + "," + h + "," + name
						+ ")  " + e.getMessage());
			}
		}

		return false;
	}

	public boolean deleteClient(int id) {

		PreparedStatement deleteClient;
		try {
			deleteClient = connection.prepareStatement(deleteClientStr);
		} catch (SQLException e) {
			System.out.println("SQL Error(deleteClient): " + "(" + id + ")" + e.getMessage());
			return false;
		}
		
		try {
			deleteClient.setInt(1, id);
			deleteClient.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println("SQL Error(deleteClient): " + "(" + id + ")" + e.getMessage());
		} finally {
			try {
				deleteClient.close();
			} catch (SQLException e) {
				System.out.println("SQL Error(deleteClient): " + "(" + id + ")" + e.getMessage());
			}
		}

		return false;
	}
	
	public TreeSet<Cell> getState(int id, int x, int y, int w, int h) {
		TreeSet<Cell> state = new TreeSet<>();

		PreparedStatement getState;
		try {
			getState = connection.prepareStatement(getStateStr);
		} catch (SQLException e) {
			System.out.println("SQL Error(getState): " + "(" + id
					+ "," + x + "," + y + "," + w + "," + h + ") " + e.getMessage());
			return state;
		}
		
		try {
			getState.setInt(1, id);
			getState.setInt(2, x);
			getState.setInt(3, x + w);
			getState.setInt(4, y);
			getState.setInt(5, y + h);
			ResultSet rs = getState.executeQuery();

			while (rs.next()) {
				state.add(new Cell(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
			}
		} catch (SQLException e) {
			System.out.println("SQL Error(getState): " + "(" + id
					+ "," + x + "," + y + "," + w + "," + h + ") " + e.getMessage());
		} finally {
			try {
				getState.close();
			} catch (SQLException e) {
				System.out.println("SQL Error(getState): " + "(" + id
						+ "," + x + "," + y + "," + w + "," + h + ") " + e.getMessage());
			}
		}

		return state;
	}

	public boolean setState(int id, int x, int y, int value) {

		if (value != 0) {
			PreparedStatement setState;
			try {
				setState = connection.prepareStatement(setStateStr);
			} catch (SQLException e) {
				System.out.println("SQL Error(setState): " + "(" + id
						+ "," + x + "," + y + "," + value + ") " + e.getMessage());
				return false;
			}
			
			try {
				setState.setInt(1, id);
				setState.setInt(2, x);
				setState.setInt(3, y);
				setState.setInt(4, value);
				setState.setInt(5, value);
				setState.executeUpdate();
				return true;
			} catch (SQLException e) {
				System.out.println("SQL Error(setState): " + "(" + id
						+ "," + x + "," + y + "," + value + ") " + e.getMessage());
			} finally {
				try {
					setState.close();
				} catch (SQLException e) {
					System.out.println("SQL Error(setState): " + "(" + id
							+ "," + x + "," + y + "," + value + ") " + e.getMessage());
				}
			}
		} else {

			PreparedStatement setStateZero;
			try {
				setStateZero = connection.prepareStatement(setStateZeroStr);
			} catch (SQLException e) {
				System.out.println("SQL Error(setState): " + "(" + id
						+ "," + x + "," + y + "," + value + ") " + e.getMessage());
				return false;
			}
			
			try {
				setStateZero.setInt(1, id);
				setStateZero.setInt(2, x);
				setStateZero.setInt(3, y);
				setStateZero.executeUpdate();
				return true;
			} catch (SQLException e) {
				System.out.println("SQL Error(setState): " + "(" + id
						+ "," + x + "," + y + "," + value + ") " + e.getMessage());
			} finally {
				try {
					setStateZero.close();
				} catch (SQLException e) {
					System.out.println("SQL Error(setState): " + "(" + id
							+ "," + x + "," + y + "," + value + ") " + e.getMessage());
				}
			}
		}

		return false;
	}
	
	public TreeSet<Cell> getGlobalDiff(int id) {
		TreeSet<Cell> state = new TreeSet<>();

		PreparedStatement getGlobalStateDiff;
		try {
			getGlobalStateDiff = connection.prepareStatement(getGlobalStateDiffStr);
		} catch (SQLException e) {
			System.out.println("SQL Error(getGlobalDiff): (" + id + ") " + e.getMessage());
			return state;
		}	
		
		try {
			getGlobalStateDiff.setInt(1, id);
			getGlobalStateDiff.setInt(2, id);
			ResultSet rs = getGlobalStateDiff.executeQuery();

			while (rs.next()) {
				int i = rs.getInt(1);
				int x = rs.getInt(2);
				int y = rs.getInt(3);
				int val = rs.getInt(4);
				
				if(i == 0)
					state.add(new Cell(x, y, val));
				else
					state.add(new Cell(x, y, 0));
			}
		} catch (SQLException e) {
			System.out.println("SQL Error(getGlobalDiff): (" + id + ") " + e.getMessage());
		} finally {
			try {
				getGlobalStateDiff.close();
			} catch (SQLException e) {
				System.out.println("SQL Error(getGlobalDiff): (" + id + ") " + e.getMessage());
			}
		}

		return state;
	}
}
