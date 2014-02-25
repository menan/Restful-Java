package edu.carleton.comp4601.assignment2.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {
	public static String CRAWL_DIR = "/Volumes/My Passport/School/workspace/data/crawler/root";

        public static void main(String[] args) throws Exception {
                int numberOfCrawlers = 1;

                CrawlConfig config = new CrawlConfig();
                config.setCrawlStorageFolder(CRAWL_DIR);
                config.setPolitenessDelay(5);
        		config.setMaxPagesToFetch(20);

                /*
                 * Instantiate the controller for this crawl.
                 */
                PageFetcher pageFetcher = new PageFetcher(config);
                RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
                RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
                CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

                /*
                 * For each crawl, you need to add some seed urls. These are the first
                 * URLs that are fetched and then the crawler starts following links
                 * which are found in these pages
                 */
                controller.addSeed("http://www.carleton.ca");
                controller.addSeed("http://sikaman.dyndns.org:8888/courses/");
                controller.addSeed("http://people.scs.carleton.ca/~jeanpier/");

                /*
                 * Start the crawl. This is a blocking operation, meaning that your code
                 * will reach the line after this only when crawling is finished.
                 */
                
                
//                controller.start(CustomWebCrawler.class, numberOfCrawlers);
//                
//                controller.waitUntilFinish();
//                System.out.println("Finished Crawling!");
//
//                CustomWebCrawler.storeGraph();
                
                
                CustomWebCrawler.calculatePageRank();
                
                
                CustomWebCrawler.searchFor("3004");


        }
}
