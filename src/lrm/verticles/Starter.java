package lrm.verticles;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class Starter extends Verticle{
	
	public Starter() {
	
	}

	@Override
	public void start() {
		JsonObject appConfig = container.config();

		JsonObject connectorConfig = appConfig.getObject("connector");
		JsonObject databaseConfig = appConfig.getObject("database");

		container.deployVerticle("lrm.verticles.ClientConnector", connectorConfig);
		container.deployWorkerVerticle("lrm.verticles.DatabaseVerticle", databaseConfig);
		
	}
}
