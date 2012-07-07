package webcrawler;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Nicolas
 */
public class Main {

    public static void main(String[] args) throws MalformedURLException {
		if( args.length == 0 ){
			throw new IllegalArgumentException("You must provide a URL as the first parameter.");
		}
		else{
			Crawler crawler = Crawler.getCrawler(new SqlOutputReceiver());
			crawler.start(new URL("http://nicolasbouliane.com"));
		}
    }
}
