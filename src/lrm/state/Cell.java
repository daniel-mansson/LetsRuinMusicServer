package lrm.state;

public class Cell implements Comparable<Cell> {
	private int x;
	private int y;
	private int value;
	
	public Cell(int x, int y, int value) {
		this.x = x;
		this.y = y;
		this.value = value;
	}
	
	@Override
	public int compareTo(Cell o) {
		if(x != o.x)
			return x - o.x;
		else if(y != o.y)
			return y - o.y;
		
		return value - o.value;
	}
	
	public int getValue() {
		return value;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
}
