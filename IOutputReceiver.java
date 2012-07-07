package webcrawler;

import java.net.URL;

/**
 *
 * @author Nicolas
 */
public interface IOutputReceiver {
	public void receive(URL url, String content);
}
