package lrm.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

public class Deploy {

	public static void main(String[] args) throws IOException {
		PlatformManager pm = PlatformLocator.factory.createPlatformManager();
		
		File file = new File("config/config.json");
	    FileInputStream fis = new FileInputStream(file);
	    byte[] data = new byte[(int)file.length()];
	    fis.read(data);
	    fis.close();
	    String s = new String(data, "UTF-8");

		JsonObject conf = new JsonObject(s);
		
		URL[] cp = null;
		try {
			cp = new URL[] {
					new URL("file:./bin/"),
					new URL(
							"file:./depends/mysql-connector-java-5.1.20-bin.jar"), };
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		pm.deployVerticle("lrm.verticles.Starter", conf, cp, 1, null, null);

		while (true) {
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
