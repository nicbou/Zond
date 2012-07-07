package webcrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Runnable crawler thread
 * @author Nicolas
 */
public class Crawler extends Thread{
	private static Crawler instance = new Crawler(null);

	//The OutputReceiver receives all crawled pages for further processing
	private static IOutputReceiver outputReceiver;

	//The link queues
	private BlockingQueue<URL> linksToCrawl = new LinkedBlockingQueue<URL>(500);
	private BlockingQueue<URL> crawledLinks = new LinkedBlockingQueue<URL>();

	//The threadpool used to process the queue
	private static ExecutorService threadPool = Executors.newFixedThreadPool(50);

	//The pattern used to find URLs in the content
	private static Pattern urlMatcherPattern = Pattern.compile("(http|https|ftp)\\://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(:[a-zA-Z0-9]*)?/?([a-zA-Z0-9\\-\\._\\?\\,/\\\\\\+&amp;%\\$#\\=~])*");

	/**
	 * Instanciates a crawler with its OutputReceiver
	 * @param or Sets the OutputReceiver to be used
	 */
	private Crawler(IOutputReceiver or){
		outputReceiver = or;
	}

	/**
	 * @param The OutputReceiver to use
	 * @return The crawler's instance
	 */
	public static Crawler getCrawler(IOutputReceiver or){
		outputReceiver = or;
		return instance;
	}

	public static Crawler getCrawler(){
		return instance;
	}

	/**
	 * Takes an initial URL and starts the crawler.
	 * @param url The URL to start crawling from.
	 */
	public void start(URL url){
		try {
			linksToCrawl.put(url);
		}
		catch (InterruptedException ex) {
			//This shouldn't be a problem for us.
		}
		start();
	}

	/**
	 * Crawls pages, finds URLs and adds them to the queue until there are no
	 * more URLs left to crawl. The page content is also dispatched to the
	 * outputReceiver, which is free to use the crawled content.
	 */
	@Override
	public void run(){
		try {
			URL url;
			while ((url = linksToCrawl.take()) != null) {
				threadPool.execute(Crawler.getCrawler());
				String pageContent = getPageContent(url);
				if( pageContent.length()>0 ){
					parseURLs(pageContent);
					if (outputReceiver != null)
						outputReceiver.receive(url, pageContent);
				}
				crawledLinks.put(url);
			}
		}
		catch (InterruptedException ex) {
			System.out.println(ex);
		}
		threadPool.shutdown();
	}

	/**
	 * Removes all remaining links from the linksToCrawl queue
	 */
	public void clearQueue(){
		linksToCrawl.clear();
	}

	/**
	 * Removes all links from the crawled links queue
	 */
	public void clearCrawledQueue(){
		crawledLinks.clear();
	}

	/**
	 * @return The uncrawled links
	 */
	public BlockingQueue<URL> getLinksToCrawl(){
		return linksToCrawl;
	}

	/**
	 * @return The crawled links
	 */
	public BlockingQueue<URL> getCrawledLinks(){
		return crawledLinks;
	}

	/**
	 * Fetches the content from a page.
	 * @param url The URL to fetch
	 * @return The page's content as a string
	 */
	public String getPageContent(URL url) {
		BufferedReader bufferedReader = null;
		StringBuilder buffer = new StringBuilder();
		try {
			URLConnection urlc = url.openConnection();
			if (urlc.getContentType().startsWith("text/html")){
				bufferedReader = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
				String inputLine;
				while ((inputLine = bufferedReader.readLine()) != null)
					buffer.append(inputLine);
			}
		} catch (IOException ioe) {
			 System.out.println("Could not crawl " + url);
		} finally {
			try {
				if ( bufferedReader!=null )
					bufferedReader.close();
			} catch (IOException ex) {}
		}
		return buffer.toString();
	}

	/**
	 * @return the urlMatcherPattern
	 */
	public static Pattern getUrlMatcherPattern() {
		return urlMatcherPattern;
	}

	/**
	 * Sets the regex pattern used to find URLs to enqueue in the crawled pages.
	 * @param urlMatcherPattern the urlMatcherPattern to set
	 */
	public static void setUrlMatcherPattern(Pattern urlMatcherPattern) {
		Crawler.urlMatcherPattern = urlMatcherPattern;
	}

	/**
	 * Parses a string to find URLs using the urlMatcherPattern defined in the class.
	 * @param content The content from which to parse URLs
	 * @throws InterruptedException
	 */
	private void parseURLs(String content) throws InterruptedException {
		Matcher matcher = getUrlMatcherPattern().matcher(content);
		URL newURL;
		while(matcher.find()){
			String urlMatch = matcher.group();
			try {
				newURL = new URL(urlMatch);
				if ( ! crawledLinks.contains(newURL) && ! linksToCrawl.contains(newURL) && !urlMatch.contains("w3.org")){
					linksToCrawl.offer(newURL);
					//System.out.println("Added: " + newURL + ". Queue size: " + linksToCrawl.size());
				}
				else{
					//System.out.println("Duplicate: " + newURL);
				}
			} catch (MalformedURLException ex) {
				System.out.println("Cannot open connection to " + urlMatch.toString());
			}
		}
	}
}