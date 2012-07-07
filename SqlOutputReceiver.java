package webcrawler;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * Sample IOutputReceiver implementation. This one stores them in a MySQL database
 * @author Nicolas
 */
public class SqlOutputReceiver implements IOutputReceiver {
	static PreparedStatement statement = null;
	static Connection sqlConn;

	/**
	 * Receives a page and stores it in a database.
	 * @param url The crawled URL
	 * @param content The page's content
	 */
	public void receive(URL url, String content){
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			sqlConn = DriverManager.getConnection("jdbc:mysql://192.168.0.192:3306/crawler?user=root&password=canned2na");
			statement = sqlConn.prepareStatement("INSERT INTO pages (url,content) VALUES (?,?)");
			statement.setString(1, url.toString());
			statement.setString(2, content);
			statement.execute();
		}
		catch (SQLException ex){
			ex.printStackTrace();
		}
		catch (Exception ex){
			System.out.println(ex.getMessage());
		}
	}
}