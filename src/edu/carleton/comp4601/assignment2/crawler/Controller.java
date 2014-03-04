package edu.carleton.comp4601.assignment2.crawler;

import org.apache.log4j.PropertyConfigurator;

import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;
import edu.carleton.comp4601.assignment2.persistence.GraphManager;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {
	public static String CRAWL_DIR = "/Users/abdulrahmanalamoudi/Desktop/temp";
	public static int DEFAULT_CRAWL_GRAPH_ID = 100;

//	public static String CRAWL_DIR = "/Volumes/My Passport/School/workspace/data/crawler/root";
//	public static String CRAWL_DIR = "/Users/dynasty/Documents/workspace/data/crawler/root";

	public static void main(String[] args) throws Exception {
		int numberOfCrawlers = 10;

		PropertyConfigurator.configure("log4j.properties");
		
		CrawlConfig config = new CrawlConfig();

		/*
		 * The three crawlers should have different storage folders for their
		 * intermediate data
		 */
		config.setCrawlStorageFolder(CRAWL_DIR + "/crawler");

		config.setPolitenessDelay(1000);
		
		config.setMaxDepthOfCrawling(2);

		config.setMaxPagesToFetch(30);

		config.setIncludeBinaryContentInCrawling(true); // this to allow it to crawl through pdf files and other files

		
		/*
		 * Connection timeout in milliseconds
		 */
		config.setConnectionTimeout(30000); // 1 Minute

		/*
		 * This config parameter can be used to set your crawl to be resumable
		 * (meaning that you can resume the crawl from a previously
		 * interrupted/crashed crawl). Note: if you enable resuming feature and
		 * want to start a fresh crawl, you need to delete the contents of
		 * rootFolder manually.
		 */
		config.setResumableCrawling(false);

		config.setMaxDownloadSize(10485760); // 10 Mb (1 Mb = 1048576)

		/*
		 * We will use different PageFetchers for the two crawlers.
		 */
		PageFetcher pageFetcher1 = new PageFetcher(config);

		/*
		 * We will use the same RobotstxtServer for all of the crawlers.
		 */
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher1);

		// Make a controller for each major link we visit
		CrawlController controller = new CrawlController(config, pageFetcher1, robotstxtServer);

		
		String[] crawler1Domains = new String[] { "http://www.carleton.ca", "http://sikaman.dyndns.org:8888/courses/", "http://people.scs.carleton.ca/~jeanpier/"};

		controller.setCustomData(crawler1Domains);

		
		/*
		 * For each crawl, you need to add some seed urls. These are the first
		 * URLs that are fetched and then the crawler starts following links
		 * which are found in these pages
		 */
		controller.addSeed("http://www.carleton.ca");
		
		controller.addSeed("http://sikaman.dyndns.org:8888/courses/");
		controller.addSeed("http://sikaman.dyndns.org:8888/courses/2601/");
		controller.addSeed("http://sikaman.dyndns.org:8888/courses/4601/");
		
		controller.addSeed("http://people.scs.carleton.ca/~jeanpier/");

		/*
		 * Setup the databases
		 */
		DocumentsManager.getDefault().setupTable();
		GraphManager.getDefault().setupTable();
		CustomWebCrawler.setupNewGraph();

		
		/*
		 * Each crawler will have numberOfCrawlers of threads
		 */
		controller.start(CustomWebCrawler.class, numberOfCrawlers);
		
		controller.waitUntilFinish();
		System.out.println("Crawler is finished.");

		CustomWebCrawler.storeGraph(DEFAULT_CRAWL_GRAPH_ID);
		
	}
}
