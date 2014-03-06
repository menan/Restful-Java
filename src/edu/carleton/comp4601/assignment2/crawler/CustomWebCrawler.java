package edu.carleton.comp4601.assignment2.crawler;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.mongodb.BasicDBObject;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;
import edu.carleton.comp4601.assignment2.persistence.GraphManager;

public class CustomWebCrawler extends WebCrawler {


    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|java|jar" 
                                                      + "|mid|mp2|mp3|mp4"
                                                      + "|wav|avi|mov|mpeg|ram|m4v" 
                                                      + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    private final static Pattern FILTERS_TIKA = Pattern.compile(".*(\\.(jpeg|tiff|gif|png|pdf|doc|docx|xls|xlsx|ppt|pptx))$");
	private static DirectedGraph<Integer, DefaultEdge> g;
	public static long MIN_POLITNESS_TIME_IN_MS = 200;
	public static long MAX_POLITNESS_TIME_IN_MS = 75000; // 1.25 min
	public static long MAX_TIME_TO_WAIT_IN_SEC = 30; // 1 min
	private Map<String, Duration > durationsToVisitDomains;
//	private static int CRAWLER_ID = 1;
	private int crawler_id;
	
	public static void setupNewGraph(){
		g = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
	}
	
	/**
	 * Before starting crawling, the onStart() method is called.
	 * You may initialize some local variables if you like. 
	 */
	@Override
	public void onStart() {
//		crawler_id = CRAWLER_ID++;
		durationsToVisitDomains = new ConcurrentHashMap< String, Duration >();
		if(g==null)
			setupNewGraph();
	}
	
	/**
	 * Before ending crawling, the onBeforeExit() method is called.
	 * You may initialize some local variables if you like. 
	 */
    @Override
    public void onBeforeExit(){
//    	storeGraph();
    	calculatePageRank();
    }
	
    /**
     * This function is called once the header of a page is fetched.
     * It can be overwritten by sub-classes to perform custom logic
     * for different status codes. For example, 404 pages can be logged, etc.
     */
    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
    	String url = webUrl.getURL();
    	startingVisit(url);
//    	int max = myController.getConfig().getMaxPagesToFetch();
//    	if(max>0)
//    		myController.getConfig().setMaxPagesToFetch(max-1);

    }

    
    /**
     * You should implement this function to specify whether
     * the given url should be crawled or not (based on your
     * crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
        boolean answer = !FILTERS.matcher(href).matches()
				&& (href.contains("carleton.ca/")
						|| href.startsWith("http://sikaman.dyndns.org:8888/courses/") || href
							.startsWith("http://people.scs.carleton.ca/~jeanpier/"));
//        System.out.println("Should visit: " + href+" ("+answer+")");
		return answer;
    }

    /**
     * This function is called when a page is fetched and ready 
     * to be processed by your program.
     */
	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();
		String visited_url = page.getWebURL().getURL();
		Integer docid = new Integer(page.getWebURL().getDocid());
		String parent_url = page.getWebURL().getParentUrl();
		String page_type = page.getContentType();
		BasicDBObject doc = null;
		System.out.println("URL: " + url + "\n(Page-Type:" + page_type + ")");

		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();
			org.jsoup.nodes.Document jsoup_doc = Jsoup.parse(html);

			// ----- MongoDB -----
			Elements jsoup_links = jsoup_doc.getElementsByTag("a");
			Elements paragraphs = jsoup_doc.getElementsByTag("p");
			Elements hTags = jsoup_doc.select("h1, h2, h3, h4");
			Elements jsoup_text = new Elements(paragraphs);
			jsoup_text.addAll(hTags);

			// Add page to db
			doc = new BasicDBObject();
			doc.append("id", docid);
			doc.append("url", visited_url);
			doc.append("parent_url", parent_url);
			doc.append("date", new Date());
			// text
			StringBuilder text_builder = new StringBuilder();
			for (Element e : jsoup_text) {
				text_builder.append(e.text() + "\n");
			}
			// System.out.println("jsoup_text size:"+jsoup_text.size());
			doc.append("text", text_builder.toString());
			// images
			String image_selector = "img[src~=(?i)\\.(png|jpe?g|gif)]";
			// Elements jsoup_images = jsoup_doc.getElementsByTag("img");
			Elements jsoup_images = jsoup_doc.select(image_selector);
			List<String> images = new ArrayList<>();
			List<String> tags = new ArrayList<>();
			for (Element e : jsoup_images) {
				String src = e.attr("src");
				String alt = e.attr("alt");
				images.add(src);
				tags.add(alt);
				// System.out.println("image:"+e.toString());
				// System.out.println("	> src:"+src);
				// System.out.println("	> alt:"+alt);

			}
			// System.out.println("jsoup_images size:" + jsoup_images.size()
			// + ", images size:" + images.size());
			doc.append("images", images);
			doc.append("tags", tags);
			// links
			List<String> links = new ArrayList<>();
			for (Element e : jsoup_links) {
				// System.out.println("link:"+e.toString());
				String link = e.attr("abs:href");

				if (link != null && !link.isEmpty()) {
					links.add(link);
				}
			}
			// System.out.println("jsoup_links size:" + jsoup_links.size()
			// + ", links size:" + links.size());
			doc.append("links", links);

		}
		else if (FILTERS_TIKA.matcher(url).matches()) {
			// Add page to db
			doc = new BasicDBObject();
			docid = myController.getDocIdServer().getNewDocID(url);
			doc.append("id", docid);
			doc.append("url", visited_url);
			doc.append("parent_url", parent_url);
			doc.append("date", new Date());
			System.out.println("Inside the ELSE and the assigned docid = "+docid +" (URL:"+url+" | PARENT_URL:+"+parent_url+")");

  			parseDoc(url, doc);
		}


		DocumentsManager.getDefault().add(doc);

		boolean graphed = graph(docid, page.getWebURL().getParentDocid());

		if (graphed)
			System.out.println("Just graphed too.");
		else
			System.out.println("There was an error graphing the document");

		endingVisit(url);

		// ... to be implemented
		System.out
				.println("_____________=========-------==========-------========__________");
	}
    
    public void parseDoc(String url, BasicDBObject doc){

    	Tika tika = new Tika();
		java.io.InputStream input = null;
		try {
			URL net_url = new URL(url);
			Metadata metadata = new Metadata();
			input = TikaInputStream.get(net_url, metadata);
			org.xml.sax.ContentHandler textHandler = new BodyContentHandler(-1);
			ParseContext context = new ParseContext();
			Parser parser = new AutoDetectParser();
			String type = tika.detect(net_url);
			
			System.out.println("MimeType type =" + type.toString());

			Map<String,Object> mongoDB_metadata = new HashMap<String, Object>();
			
			parser.parse(input, textHandler, metadata, context); // parse the stream
		    for (int i = 0; i < metadata.names().length; i++) {
		        String item = metadata.names()[i];
		        System.out.println(" > "+item + " -- " + metadata.get(item));
		        if(metadata.isMultiValued(item))
		        	mongoDB_metadata.put(item, metadata.getValues(item));
		        else
		        	mongoDB_metadata.put(item, metadata.get(item));
		        	
		    }

		    doc.append("metadata", mongoDB_metadata);
		    doc.append("text", textHandler.toString());
		    
        	System.out.println("Gonna parse url: " + url);
        	
	        System.out.println("Type:" + metadata.get(HttpHeaders.CONTENT_TYPE));
	        System.out.println("Title:" + metadata.get("title"));
	       // System.out.println("Text:" + text);
	        System.out.println("docid:" + doc.get("id"));
	        System.out.println("Metadata:" + metadata.toString());
		    
		} catch (IOException | org.xml.sax.SAXException | TikaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // close the stream
		}
    }

    public void calculatePageRank(){
//		DirectedGraph<Integer, DefaultEdge> newG = GraphManager.getDefault().loadGraph();
		
		for(int v: g.vertexSet()){
			System.out.println("vertex: " + v + "("+"), rank:" + pageRank(v));
			boolean updated = DocumentsManager.getDefault().updateScore(v, pageRank(v));
			if(updated)
				System.out.println("updated the score on the database as well");
			else
				System.out.println("Error updating the score to the db");
				
			
			
		}
    }
    
    public static boolean storeGraph(int crawler_id){
    	GraphManager.getDefault().save(g, crawler_id);
		System.out.println("The finished version of the graph:");
		System.out.println(g);
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
		return g.inDegreeOf(docID);
    }

    public boolean graph(int currentDocID, int prentDocID){
    	if (prentDocID == -1){
    		prentDocID = 0;
    	}

		g.addVertex(prentDocID);
		g.addVertex(currentDocID);
		
		g.addEdge(prentDocID, currentDocID);
		
		return true;
    }
    
    
//    public static void searchFor(String query){
//    	List<Document> result = LuceneManager.getDefault().query(query, 10);
//	    System.out.println("Searchfor : "+query + " size="+result.size()+" "+result.toString());
//    }

	
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
