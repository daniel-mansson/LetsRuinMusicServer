package lrm.verticles;

import java.sql.Time;

public class Util {
	public static String now() {
		return new Time(System.currentTimeMillis()).toString();
	}
}
