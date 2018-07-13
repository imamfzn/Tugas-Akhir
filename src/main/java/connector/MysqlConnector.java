package connector;

import java.sql.*;
import io.github.cdimascio.dotenv.Dotenv;

public class MysqlConnector {
	private final static String DB_URL_FORMAT = "jdbc:mysql://localhost:%s/%s?useSSL=false";
	private Connection db = null;
	
	public MysqlConnector() throws Exception {
		if (db == null){
			Dotenv dotenv = Dotenv.configure()
	        		  .directory("./")
	        		  .ignoreIfMalformed()
	        		  .ignoreIfMissing()
	        		  .load();
			try {
				Class.forName("com.mysql.jdbc.Driver");
				String port = dotenv.get("DB_PORT");
				String dbName = dotenv.get("DB_NAME");
				String username = dotenv.get("DB_USERNAME");
				String password = dotenv.get("DB_PASSWORD");
				String dbURL = String.format(DB_URL_FORMAT, port, dbName); 

				db = DriverManager.getConnection(dbURL, username, password);
			} catch (Exception e){
				throw e;
			}
		}
	}
	
	public Connection getConnection(){
		return this.db;
	}
}