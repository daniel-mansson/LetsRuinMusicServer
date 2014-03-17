package lrm.verticles;

import java.sql.Time;
import java.text.DateFormat;

public class Util {
	
	private static DateFormat date = DateFormat.getDateInstance();
	
	public static String now() {
		Time t = new Time(System.currentTimeMillis());
		return date.format(t) + " " + t.toString();
	}
}
