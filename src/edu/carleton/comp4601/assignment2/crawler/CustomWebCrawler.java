package edu.carleton.comp4601.assignment2.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

import com.mongodb.DBObject;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import Jama.Matrix;


import edu.carleton.comp4601.assignment2.dao.Document;
import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;
import edu.carleton.comp4601.assignment2.persistence.LuceneManager;

public class CustomWebCrawler extends WebCrawler {


    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|java|jar" 
                                                      + "|mid|mp2|mp3|mp4"
                                                      + "|wav|avi|mov|mpeg|ram|m4v|pdf" 
                                                      + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    
    private final static Pattern FILTERS_TIKA = Pattern.compile(".*(\\.(jpeg|tiff|gif|png|pdf|doc|docx|xls|xlsx|ppt|pptx))$");
    
	public static UndirectedGraph<String, DefaultEdge> graph;
	private static DirectedGraph<URL, DefaultEdge> g = new DefaultDirectedGraph<URL, DefaultEdge>(DefaultEdge.class);

    /**
     * You should implement this function to specify whether
     * the given url should be crawled or not (based on your
     * crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
//        System.out.println("Not sure if I should visit " + href);
            if (!FILTERS.matcher(href).matches() && (href.contains("carleton.ca/") ||
            		href.startsWith("http://sikaman.dyndns.org:8888/courses/") ||
        			href.startsWith("http://people.scs.carleton.ca/~jeanpier/"))){

              if (FILTERS_TIKA.matcher(href).matches()){
      			parseDoc(url);
              	return false;
              }
              else{
                  return true;
              }
            }
            else{
            	return false;
            }
            
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


//            		boolean graphed = graph(Integer.toString(page.getWebURL().getDocid()), page.getWebURL().getParentUrl());
            		
					try {
						graph(new URL(url),new URL(page.getWebURL().getParentUrl()));
            			System.out.println("Just graphed too.");
					} catch (MalformedURLException e) {
            			System.out.println("There was an error graphing the document: " + e.getMessage());
//						e.printStackTrace();
					}
            		
//            		if(graphed)
//            			System.out.println("Just graphed too.");
//            		else
//            			System.out.println("There was an error graphing the document");

//            		boolean indexed = LuceneManager.getDefault().indexDocument(url, page.getWebURL().getDocid(), new Date(), text, "text/html");

//            		if(indexed)
//            			System.out.println("Document indexed to the lucene successfully");
//            		else
//            			System.out.println("There was an error indexing the document");
            		
                    //... to be implemented
            		System.out.println("_____________=========-------==========-------========__________");
            }
    }
    
    public void parseDoc(WebURL weburl){
		URL url;
		InputStream input;
		try {
			url = new URL(weburl.getURL());
			input = TikaInputStream.get(url);

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
	        System.out.println("Text:" + text);
	        System.out.println("Metadata:" + metadata.toString());
	        
	        String title = "";
	        if(metadata.get("title") != null)
	        	title = metadata.get("title");
        	else
        		title = weburl.getURL();

    		Document d = new Document(weburl.getDocid());
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
		System.out.println("_____________=========-------==========-------========__________");
        
    }

	public static Graph<String, DefaultEdge> getInstanceOfGraph(){
		if(graph == null){
			graph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		}
		return graph;
	}


    public boolean graph(String docid, String parent_url){
    	
    	boolean returnValue = false;
		// ----- Graph -----
		// - Add vertex
		if (!getInstanceOfGraph().containsVertex(docid)) {
			getInstanceOfGraph().addVertex(docid);
			returnValue = true;
		}
		// - Add edge
		if (parent_url != null) {
			List<DBObject> seachResult = DocumentsManager.getDefault().search("url",parent_url, true);
			if (seachResult != null && seachResult.size() > 0) {
				getInstanceOfGraph().addEdge(seachResult.get(0).get("id").toString(),docid);
				returnValue = true;
			}
		}
		
		return returnValue;
    }
    
    
    public static UndirectedGraph<String, DefaultEdge> getGraph(){
    	return graph;
    }
    
    public static boolean storeGraph(){
    	//to be implemented 
    	System.out.println("Graph:");
    	System.out.println(g.toString());
    	return true;
    }
    

    public boolean graph(URL curent_url, URL parent_url){

        // add the vertices
		g.addVertex(curent_url);
		g.addVertex(parent_url);
		// add edges to create linking structure
		g.addEdge(curent_url, parent_url);
		
		g.
		
		return true;

    }
    public static void searchFor(String query){
    	LuceneManager.getDefault().search(query, 2);
    }

}
