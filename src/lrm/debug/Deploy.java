package lrm.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigLoader;
import com.hazelcast.nio.IOUtil;

public class Deploy {

	public static void main(String[] args) throws IOException {
		PlatformManager pm = PlatformLocator.factory.createPlatformManager();

		Gson gson = new Gson();

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
		pm.deployVerticle("lrm.verticles.Starter", conf, cp, 1, null,
				new Handler<AsyncResult<String>>() {

					@Override
					public void handle(AsyncResult<String> arg0) {
						System.out.println(arg0.succeeded());
						System.out.println(arg0.cause());
						System.out.println(arg0.result());
					}
				});

		while (true) {
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
