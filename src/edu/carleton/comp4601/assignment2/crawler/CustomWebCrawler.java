package edu.carleton.comp4601.assignment2.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import Jama.Matrix;


import edu.carleton.comp4601.assignment2.dao.Document;
import edu.carleton.comp4601.assignment2.dao.DocumentCollection;
import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;
import edu.carleton.comp4601.assignment2.persistence.GraphManager;
import edu.carleton.comp4601.assignment2.persistence.LuceneManager;
import edu.carleton.comp4601.assignment2.utility.Marshaller;

public class CustomWebCrawler extends WebCrawler {


    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|java|jar" 
                                                      + "|mid|mp2|mp3|mp4"
                                                      + "|wav|avi|mov|mpeg|ram|m4v" 
                                                      + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    
    private final static Pattern FILTERS_TIKA = Pattern.compile(".*(\\.(jpeg|tiff|gif|png|pdf|doc|docx|xls|xlsx|ppt|pptx))$");
    
	private DirectedGraph<Integer, DefaultEdge> g;

	public static long MIN_POLITNESS_TIME_IN_MS = 200;

	public static long MAX_POLITNESS_TIME_IN_MS = 150000; // 2.5 min

	public static long MAX_TIME_TO_WAIT_IN_SEC = 60; // 1 min

	
	private Map<String, Duration > durationsToVisitDomains;

	/**
	 * Before starting crawling, the onStart() method is called.
	 * You may initialize some local variables if you like. 
	 */
	@Override
	public void onStart() {
		durationsToVisitDomains = new ConcurrentHashMap< String, Duration >();
		g = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
	}
	
	/**
	 * Before ending crawling, the onBeforeExit() method is called.
	 * You may initialize some local variables if you like. 
	 */
    @Override
    public void onBeforeExit(){
    	storeGraph();
    	calculatePageRank();
    }
	
    /**
     * This function is called once the header of a page is fetched.
     * It can be overwritten by sub-classes to perform custom logic
     * for different status codes. For example, 404 pages can be logged, etc.
     */
    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
    	startingVisit(webUrl.getURL());
    }

    
    /**
     * You should implement this function to specify whether
     * the given url should be crawled or not (based on your
     * crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
//        System.out.println("Not sure if I should visit " + href);
		if (!FILTERS.matcher(href).matches()
				&& (href.contains("carleton.ca/")
						|| href.startsWith("http://sikaman.dyndns.org:8888/courses/") || href
							.startsWith("http://people.scs.carleton.ca/~jeanpier/"))) {
			if (FILTERS_TIKA.matcher(href).matches()) {
      			parseDoc(url);
			}
			return true;
		}
		return false;
    }

    /**
     * This function is called when a page is fetched and ready 
     * to be processed by your program.
     */
    @Override
    public void visit(Page page) {          
            String url = page.getWebURL().getURL();
            System.out.println("Crawling " + url);
    		
            if (page.getParseData() instanceof HtmlParseData) {
                    HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                    
                    
                    String html = htmlParseData.getHtml();

                    int docID = page.getWebURL().getDocid();
                    int parentDocID = page.getWebURL().getParentDocid();

                    org.jsoup.nodes.Document jDoc	=	Jsoup.parse(html.toString());
            		String title = jDoc.select("title").first().text();
            		Elements textE = jDoc.select("p");
            		Elements linksE = jDoc.select("a");
            		String text = jDoc.select("body").first().text();
            		
            		ArrayList<String> tags = new ArrayList<String>();
            		ArrayList<String> links = new ArrayList<String>();
            		for(Element t : textE){
            			tags.add(t.text());
            		}

            		for(Element l : linksE){
            			links.add(l.text());
            		}
            		System.out.println("Title:" +  title);
            		System.out.println("Text Size:" +  text.length());
            		System.out.println("Link Size" +  links.size());
            		System.out.println("Tags Size:" +  tags.size());
        	        System.out.println("docid:" + page.getWebURL().getDocid());
            		
            		
            		
            		Document d = new Document(page.getWebURL().getDocid());
            		d.setLinks(links);
            		d.setName(title);
            		d.setTags(tags);
            		d.setText(text);

            		boolean created = DocumentsManager.getDefault().create(d);
            		
            		if(created)
            			System.out.println("Document added to the db successfully");
            		else
            			System.out.println("There was an error creating the document");


            		
            		
            		boolean graphed = graph(docID, parentDocID);

            		if(graphed)
            			System.out.println("Just graphed too.");
            		else
            			System.out.println("There was an error graphing the document");

//            		boolean indexed = LuceneManager.getDefault().indexDocument(url, page.getWebURL().getDocid(), new Date(), text, "text/html");

//            		if(indexed)
//            			System.out.println("Document indexed to the lucene successfully");
//            		else
//            			System.out.println("There was an error indexing the document");
            		endingVisit(url);

            		// ... to be implemented
            		System.out.println("_____________=========-------==========-------========__________");	
		} 

    }
    
    public void parseDoc(WebURL weburl){
		URL url;
		InputStream input;
		String url_string = weburl.getURL();
		try {
			url = new URL(url_string);
			input = TikaInputStream.get(url);

			Integer docid = myController.getDocIdServer().getNewDocID(url_string);

	        ToHTMLContentHandler toHTMLHandler = new ToHTMLContentHandler();
	        
			Metadata	metadata	=	new	Metadata();	
			ParseContext	context	=	new	ParseContext();	
			Parser parser	=	 new AutoDetectParser();	

			parser.parse(input,	toHTMLHandler,	metadata,	context);

            org.jsoup.nodes.Document jDoc	=	Jsoup.parse(toHTMLHandler.toString());
			
    		String text = metadata.toString() + jDoc.select("body").first().text();

        	System.out.println("Gonna parse url: " + weburl.getURL());
        	
	        System.out.println("Type:" + metadata.get(HttpHeaders.CONTENT_TYPE));
	        System.out.println("Title:" + metadata.get("title"));
	       // System.out.println("Text:" + text);
	        System.out.println("docid:" + docid);
	        System.out.println("Metadata:" + metadata.toString());
	        
	        String title = "";
	        if(metadata.get("title") != null)
	        	title = metadata.get("title");
        	else
        		title = weburl.getURL();

    		Document d = new Document(docid);
    		d.setName(title);
    		d.setText(text);
    		
    		
    		boolean created = DocumentsManager.getDefault().create(d);

    		
    		if(created)
    			System.out.println("Document added to the db successfully");
    		else
    			System.out.println("There was an error creating the document");
    		
//    		boolean graphed = graph(Integer.toString(weburl.getDocid()), weburl.getParentUrl());
//    		if(graphed)
//    			System.out.println("Just graphed too.");
//    		else
//    			System.out.println("There was an error graphing the document");
    		
//    		boolean indexed = LuceneManager.getDefault().indexDocument(weburl.getURL(), weburl.getDocid(), new Date(), text, metadata.toString());

//    		if(indexed)
//    			System.out.println("Document indexed to the lucene successfully");
//    		else
//    			System.out.println("There was an error indexing the document");

    		
    		
		}
		catch (IOException | SAXException | TikaException e) {
			e.printStackTrace();
		}
		
    	endingVisit(url_string);
		System.out.println("_____________=========-------==========-------========__________");
        
    }

    public void calculatePageRank(){
//		DirectedGraph<Integer, DefaultEdge> newG = GraphManager.getDefault().loadGraph();
		
		for(int v: g.vertexSet()){
			System.out.println("vertex: " + v + ", rank:" + pageRank(v));
			boolean updated = DocumentsManager.getDefault().updateScore(v, pageRank(v));
			if(updated)
				System.out.println("updated the score on the database as well");
			else
				System.out.println("Error updating the score to the db");
				
			
			
		}
    }
    
    public boolean storeGraph(){
    	GraphManager.getDefault().save(g);
		System.out.println("and its saved to the DB as bytes :)");
    	return true;
    }
    
    
    public void printGraph(){
//		DirectedGraph<Integer, DefaultEdge> newG = GraphManager.getDefault().loadGraph();
		System.out.println("Graph fetched from the db is:");
		System.out.println(g);
		
		pageRank(0);
		
    }
    
    public int pageRank(int docID){
//    	DirectedGraph<Integer, DefaultEdge> newG = GraphManager.getDefault().loadGraph();
//		System.out.println("Out degree of " + docID  + " is :" + newG.outDegreeOf(docID));
		return g.outDegreeOf(docID);
    }

    public boolean graph(int currentDocID, int prentDocID){

		g.addVertex(currentDocID);
    	if (prentDocID == -1){
    		g.addVertex(0);
    	}
		g.addVertex(currentDocID);
		g.addVertex(prentDocID);
		g.addEdge(prentDocID, currentDocID);
		

		return true;
    }
    
    
    public static void searchFor(String query){
    	LuceneManager.getDefault().search(query, 2);
    }
    
    protected void startingVisit(String url){
    	if(myController.getDocIdServer().isSeenBefore(url)){
//        	System.out.println("MyCrawler - "+url);
    	}
		durationsToVisitDomains.put(url, new Duration());
		long start = System.nanoTime();
		durationsToVisitDomains.get(url).setStartTime(start);

    }
    
    protected void endingVisit(String url){
		long end = System.nanoTime();
        Duration d = durationsToVisitDomains.get(url);
        if(d != null){
	        d.setEndTime(end);
	        int visit_duration = d.getDurationInSec();
	        long delay_nextTime_ms = d.getTimeToWaitInMilliseconds(visit_duration);
        	myController.getPageFetcher().getConfig().setPolitenessDelay((int) delay_nextTime_ms);
        	System.out.println("URL: "+url+" took ("+visit_duration+" sec) to process :: wait: "+delay_nextTime_ms+" ms before visiting next time");
        }
    }

    
    protected class Duration
    {
        private long startTime;
        private long endTime;

        public Duration(){
        	
        }

        public Duration(long start_time, long end_time)
        {
        	startTime   = start_time;
        	endTime = end_time;
        }

        public long getStartTime()   { return startTime; }
        public long getEndTime() { return endTime; }
        public void setStartTime(long start_time)   { startTime = start_time; }
        public void setEndTime(long end_time)   { endTime = end_time; }

        public int getDurationInSec() { 
        	long elapsedTime = endTime - startTime;
        	return (int)  TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        }
        
        
        public long getTimeToWaitInMilliseconds(int durationTookToVisit_Sec) {
        	long timeToWait = durationTookToVisit_Sec * 10;
        	if(timeToWait <= 0){
        		return MIN_POLITNESS_TIME_IN_MS;
        	}
        	else if(timeToWait > MAX_TIME_TO_WAIT_IN_SEC){
        		return MAX_POLITNESS_TIME_IN_MS;
        	}
        	return TimeUnit.MILLISECONDS.convert(timeToWait, TimeUnit.SECONDS);
        }
    }


}
