package edu.carleton.comp4601.assignment2.crawler;

import org.apache.log4j.PropertyConfigurator;

import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;
import edu.carleton.comp4601.assignment2.persistence.LuceneManager;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {
	public static String CRAWL_DIR = "/Users/abdulrahmanalamoudi/Desktop/temp";
//	public static String CRAWL_DIR = "/Volumes/My Passport/School/workspace/data/crawler/root";
//	public static String CRAWL_DIR = "/Users/dynasty/Documents/workspace/data/crawler/root";

	public static void main(String[] args) throws Exception {
		int numberOfCrawlers = 3;

		PropertyConfigurator.configure("log4j.properties");
		
		CrawlConfig config1 = new CrawlConfig();
		CrawlConfig config2 = new CrawlConfig();
		CrawlConfig config3 = new CrawlConfig();

		/*
		 * The three crawlers should have different storage folders for their
		 * intermediate data
		 */
		config1.setCrawlStorageFolder(CRAWL_DIR + "/crawler1");
		config2.setCrawlStorageFolder(CRAWL_DIR + "/crawler2");
		config3.setCrawlStorageFolder(CRAWL_DIR + "/crawler3");

		
		config1.setPolitenessDelay(1000);
		config2.setPolitenessDelay(1000);
		config3.setPolitenessDelay(1000);
		
		config1.setMaxDepthOfCrawling(2);
		config2.setMaxDepthOfCrawling(2);
		config3.setMaxDepthOfCrawling(1);

		config1.setMaxPagesToFetch(5);
		config2.setMaxPagesToFetch(5);
		config3.setMaxPagesToFetch(5);


		
		/*
		 * Connection timeout in milliseconds
		 */
		config1.setConnectionTimeout(30000); // 1 Minute
		config2.setConnectionTimeout(30000); // 1 Minute
		config3.setConnectionTimeout(30000); // 1 Minute

		/*
		 * This config parameter can be used to set your crawl to be resumable
		 * (meaning that you can resume the crawl from a previously
		 * interrupted/crashed crawl). Note: if you enable resuming feature and
		 * want to start a fresh crawl, you need to delete the contents of
		 * rootFolder manually.
		 */
		config1.setResumableCrawling(false);
		config2.setResumableCrawling(false);
		config3.setResumableCrawling(false);

		config1.setMaxDownloadSize(10485760); // 10 Mb (1 Mb = 1048576)
		config2.setMaxDownloadSize(10485760); // 10 Mb (1 Mb = 1048576)
		config3.setMaxDownloadSize(10485760); // 10 Mb (1 Mb = 1048576)

		/*
		 * We will use different PageFetchers for the two crawlers.
		 */
		PageFetcher pageFetcher1 = new PageFetcher(config1);
		PageFetcher pageFetcher2 = new PageFetcher(config2);
		PageFetcher pageFetcher3 = new PageFetcher(config3);

		/*
		 * We will use the same RobotstxtServer for all of the crawlers.
		 */
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher1);

		// Make a controller for each major link we visit
		CrawlController controller1 = new CrawlController(config1, pageFetcher1, robotstxtServer);
		CrawlController controller2 = new CrawlController(config2, pageFetcher2, robotstxtServer);
		CrawlController controller3 = new CrawlController(config3, pageFetcher3, robotstxtServer);

		
		String[] crawler1Domains = new String[] { "http://www.carleton.ca"};
		String[] crawler2Domains = new String[] { "http://sikaman.dyndns.org:8888/courses/"};
		String[] crawler3Domains = new String[] { "http://people.scs.carleton.ca/~jeanpier/"};

		controller1.setCustomData(crawler1Domains);
		controller2.setCustomData(crawler2Domains);
		controller3.setCustomData(crawler3Domains);

		
		/*
		 * For each crawl, you need to add some seed urls. These are the first
		 * URLs that are fetched and then the crawler starts following links
		 * which are found in these pages
		 */
		controller1.addSeed("http://www.carleton.ca");
		
		controller2.addSeed("http://sikaman.dyndns.org:8888/courses/");
		controller2.addSeed("http://sikaman.dyndns.org:8888/courses/2601/");
		controller2.addSeed("http://sikaman.dyndns.org:8888/courses/4601/");
		
		controller3.addSeed("http://people.scs.carleton.ca/~jeanpier/");

		/*
		 * Setup the databases
		 */
		DocumentsManager.getDefault().setupTable();
		LuceneManager.getDefault().setupTable();
		
		
		
		/*
		 * Each crawler will have numberOfCrawlers of threads
		 */
		controller1.startNonBlocking(CustomWebCrawler.class, numberOfCrawlers);
		controller2.startNonBlocking(CustomWebCrawler.class, numberOfCrawlers);
		controller3.startNonBlocking(CustomWebCrawler.class, numberOfCrawlers);
		
		controller1.waitUntilFinish();
		System.out.println("Crawler 1 is finished.");

		controller2.waitUntilFinish();
		System.out.println("Crawler 2 is finished.");

		controller3.waitUntilFinish();
		System.out.println("Crawler 3 is finished.");
		

	}
}
