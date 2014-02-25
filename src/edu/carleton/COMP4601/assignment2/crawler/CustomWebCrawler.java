package edu.carleton.comp4601.assignment2.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import edu.carleton.comp4601.assignment2.dao.Document;
import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;

public class CustomWebCrawler extends WebCrawler {


    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|java|jar" 
                                                      + "|mid|mp2|mp3|mp4"
                                                      + "|wav|avi|mov|mpeg|ram|m4v|pdf" 
                                                      + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    
    private final static Pattern FILTERS_TIKA = Pattern.compile(".*(\\.(jpeg|tiff|gif|png|pdf|doc|docx|xls|xlsx|ppt|pptx))$");
    
	private static DirectedGraph<URL, DefaultEdge> g = new DefaultDirectedGraph<URL, DefaultEdge>(DefaultEdge.class);
    /**
     * You should implement this function to specify whether
     * the given url should be crawled or not (based on your
     * crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
        if (FILTERS_TIKA.matcher(href).matches()){
			parseDoc(url);
        	return false;
        }
        else{

            return !FILTERS.matcher(href).matches()
            		&& (href.contains("carleton.ca/")
            		|| href.startsWith("http://sikaman.dyndns.org:8888/courses/")
            		|| href.startsWith("http://people.scs.carleton.ca/~jeanpier/"));
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
            			System.out.println("Document created successfully");
            		else
            			System.out.println("There was an error creating the document");
            		
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
    			System.out.println("Parsed document created successfully");
    		else
    			System.out.println("There was an error creating the Parsed document");
    		
            //... to be implemented
			
		}
		catch (IOException | SAXException | TikaException e) {
			e.printStackTrace();
		}
		System.out.println("_____________=========-------==========-------========__________");
        
    }
    
    
    public void indexToLucene(Document d, String url){
    	StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
    	Directory index = new RAMDirectory();

    	IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);

    	IndexWriter w;
		try {
			w = new IndexWriter(index, config);
	    	addDoc(w, "Lucene in Action", "193398817");
	    	addDoc(w, "Lucene for Dummies", "55320055Z");
	    	addDoc(w, "Managing Gigabytes", "55063554A");
	    	addDoc(w, "The Art of Computer Science", "9900333X");
	    	w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	
    }
    private static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
    	org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
    	  doc.add(new TextField("title", title, Field.Store.YES));
    	  doc.add(new StringField("isbn", isbn, Field.Store.YES));
    	  w.addDocument(doc);
	}
    public static DirectedGraph<URL, DefaultEdge> getGraph(){
    	return g;
    }

}
