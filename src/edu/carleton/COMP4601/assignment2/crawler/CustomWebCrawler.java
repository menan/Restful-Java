package edu.carleton.comp4601.assignment2.crawler;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.BasicDBObject;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;


public class CustomWebCrawler extends WebCrawler {


    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" 
                                                      + "|png|tiff?|mid|mp2|mp3|mp4"
                                                      + "|wav|avi|mov|mpeg|ram|m4v|pdf" 
                                                      + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    
	private static DirectedGraph<URL, DefaultEdge> g = new DefaultDirectedGraph<URL, DefaultEdge>(DefaultEdge.class);
    /**
     * You should implement this function to specify whether
     * the given url should be crawled or not (based on your
     * crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
//    	System.out.println("Not sure if I should visit: " + url.toString());
            String href = url.getURL().toLowerCase();
            return !FILTERS.matcher(href).matches()
            		&& (href.startsWith("http://www.carleton.ca/")
            		|| href.startsWith("http://sikaman.dyndns.org:8888/courses/")
            		|| href.startsWith("http://people.scs.carleton.ca/~jeanpier/"))
            		;
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
            		Document jDoc	=	Jsoup.parse(html.toString());
            		Element artistName = jDoc.select("div#artist_infos > h3").first();
            		Elements genre_from = jDoc.select("div#artist_infos > p > a");

            		Element views = jDoc.select("div#artist_stats_box > p > strong").first();
            		Element plays = jDoc.select("div#artist_stats_box > p > strong").last();
            		
            		Element email = jDoc.select("div#artist_contacts_box > p > script").first();
            		Elements websites = jDoc.select("div#artist_contacts_box > p > a");

            		Element bio = jDoc.select("div#artist_biography_content").first();

            		Element image = jDoc.select("div#artist_image > img").first();
            		Elements comments = jDoc.select("div.user_comment");
            		
            		
            		String imgageURL = "http://www.unsigned.com" + image.attr("src");
            		
            		String emailStr = "";
            		
            		if(email != null){
            			emailStr = sanitizeEmails(email.html());
            		}
            		else{
            			System.out.println("Email is nil tho");
            		}
            		
            		ArrayList<String>  genre = new ArrayList<String>();
            		ArrayList<String>  from = new ArrayList<String>();
            		
            		for	(Element	link	:	genre_from)	{
            			if (link.attr("href").contains("countries")){
            				from.add(link.text().replace(" ", ""));
            			}
            			else{
            				genre.add(link.text());
            			}
        			}

            		ArrayList<String> urls = new ArrayList<String>();
            		for	(Element	w	:	websites)	{
            			urls.add(w.text());
        			}
            		
            		if (artistName != null){

                		String username = page.getWebURL().getPath().substring(1);
                		
                		BasicDBObject doc = new BasicDBObject("docid", page.getWebURL().getDocid());
                		doc.append("username", username);
                		doc.append("genre", genre);
                		doc.append("from", from);
                		doc.append("no_of_views", views.text());
                		doc.append("no_of_plays", plays.text());
                		doc.append("no_of_comments", comments.size());
                		doc.append("email", emailStr);
                		doc.append("websites", urls);
                		doc.append("bio", bio.text());
                		doc.append("image", imgageURL);

                		System.out.println("artist name: " + artistName.text());
                		System.out.println("username: " + username);
                		System.out.println("genre: " + genre.toString());
                		System.out.println("from: " + from.toString());
                		System.out.println("total views: " + views.text());
                		System.out.println("total plays: " + plays.text());
                		System.out.println("Number of Comments: " + comments.size());
                		System.out.println("Email: " + emailStr);
                		System.out.println("Websites: " + urls.toString());
                		System.out.println("Bio: " + bio.text());
                		System.out.println("Image: " + imgageURL);

                		
                		//save the document here
                		System.out.println("=============================");
            		}
            		else{
            			System.out.println("Artist name is nil tho");
            		}

            }
    }
    
    public String sanitizeEmails(String html)
    {
		return html.substring(16,html.length()-3).replace(", ", "@").replace("'", "");
    	
    }
    
    public static DirectedGraph<URL, DefaultEdge> getGraph(){
    	return g;
    }

}
