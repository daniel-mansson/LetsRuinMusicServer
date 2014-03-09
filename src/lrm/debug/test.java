package lrm.debug;

import lrm.verticles.DatabaseVerticle;

public class test {

	public static void main(String[] args) {
		DatabaseVerticle v = new DatabaseVerticle();
		v.start();
	}

}
