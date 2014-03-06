package edu.carleton.comp4601.assignment2.dao;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import com.mongodb.BasicDBObject;

@XmlRootElement
public class Document extends BasicDBObject{
	
	public final static String URL = "url";
	public final static String DOC_ID = "docid";
	public final static String DATE = "date"; //meaning the date and time when the document was crawled.
	public final static String CONTENT = "text";
	public final static String METADATA = "metadata";

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private double score;
	private String name;
	private String url;
	private String parent_url;
	private String text;
	private Date date; // meaning the date and time when the document was crawled
	private ArrayList<String> tags;
	private ArrayList<String> links;
	private Map<String,Object> metadata;
	private ArrayList<String> images;

	public Document() {
		tags = new ArrayList<String>();
		links = new ArrayList<String>();
		metadata = new HashMap<String, Object>();
		score = 0.0f;
	}

	public Document(Integer id) {
		this();
		this.id = id;
		score = 0.0f;
	}

	@SuppressWarnings("unchecked")
	public Document(Map<?, ?> map) {
		this();
		this.id = (Integer) map.get("id");
		this.score = ((Double) map.get("score")).doubleValue();
		this.name = (String) map.get("name");
		this.text = (String) map.get("text");
		this.tags = (ArrayList<String>) map.get("tags");
		this.links = (ArrayList<String>) map.get("links");
		this.metadata = (Map<String, Object>) map.get("metadata");
		this.images = (ArrayList<String>) map.get("images");
		this.date = (Date) map.get("date");
		this.setUrl((String) map.get("url"));
		this.setParent_url((String) map.get("parent_url"));
	}
	
	@SuppressWarnings("unchecked")
	public Document(BasicDBObject obj) {
		this();
		this.id = obj.getInt("id");
		this.score =  obj.getDouble("score");
		this.name = (String) obj.get("name");
		this.text = (String) obj.get("text");
		this.tags = (ArrayList<String>) obj.get("tags");
		this.links = (ArrayList<String>) obj.get("links");
		this.metadata = (Map<String, Object>) obj.get("metadata");
		this.images = (ArrayList<String>) obj.get("images");
		this.date = (Date) obj.get("date");
		this.setUrl((String) obj.get("url"));
		this.setParent_url((String) obj.get("parent_url"));
	}

	
	public Integer getId() {
		return id;
	}

	public void setScore(Float score2) {
		this.score = score2;
	}

	public double getScore() {
		return score;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getParent_url() {
		return parent_url;
	}

	public void setParent_url(String parent_url) {
		this.parent_url = parent_url;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	// To get the attribute ("alt") of an image
	public ArrayList<String> getTags() {
		return tags;
	}

	public void setTags(ArrayList<String> tags) {
		this.tags = tags;
	}

	public ArrayList<String> getLinks() {
		return links;
	}

	public void setLinks(ArrayList<String> links) {
		this.links = links;
	}
	
	public void setImages(ArrayList<String> images) {
		this.images = images;
	}

	// To get the attribute ("src") of an image
	public ArrayList<String> getImages() {
		return images;
	}

	public Date getCrawledDate() {
		return date;
	}

	public void setCrawledDate(Date crawledDate) {
		this.date = crawledDate;
	}

	public Map<String,Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String,Object> metadata) {
		this.metadata = metadata;
	}

	public void addTag(String tag) {
		tags.add(tag);
	}

	public void removeTag(String tag) {
		tags.remove(tag);
	}

	public void addLink(String link) {
		links.add(link);
	}

	public void removeLink(String link) {
		links.remove(link);
	}

	public String getSDALink(){
		return "/COMP4601SDA/rest/sda/" + id;
	}
	
	public String toHTML(){
		return "<tr><td><a href=\"" + getSDALink() + "\">" + id + "</a></td><td>" + name + "</td></tr>";
		
	}
	
	public static Document getDocumentFrom(BasicDBObject obj){
		return new Document(obj);
	}
	
	public static BasicDBObject getBasicDBObjectFromDocument(Document doc){
		BasicDBObject obj = new BasicDBObject("id", doc.getId());
		obj.put("name", doc.getName());
		obj.put("url", doc.getUrl());
		obj.put("text", doc.getText());
		obj.put("tags", doc.getTags());
		obj.put("links", doc.getLinks());
		obj.put("date", doc.getCrawledDate());
		obj.put("score", doc.getScore());
		obj.put("metadata", doc.getMetadata());
		obj.put("parent_url", doc.getParent_url());
		
//		System.out.println("getBasicDBObjectFromDocument Testing Resutl Before Returning it");
//		System.out.println("Passed Doc: "+doc.toString());		
//		for(String k : obj.keySet()){
//			System.out.println("	> "+k+" : "+obj.get(k));
//		}
//
		return obj;
	}

	public static org.apache.lucene.document.Document getLuceneDocFromDocument(Document doc) throws UnsupportedEncodingException{
		org.apache.lucene.document.Document lucene_doc = new org.apache.lucene.document.Document();
		if(doc.getUrl() != null)
			lucene_doc.add(new StringField(URL, doc.getUrl(), Field.Store.YES));
		if(doc.getId() != null)
			lucene_doc.add(new IntField(DOC_ID, doc.getId(), Field.Store.YES));
		if(doc.getCrawledDate() != null)
			lucene_doc.add(new LongField(DATE, doc.getCrawledDate().getTime(), Field.Store.NO));
		String content = doc.getImages() != null ? doc.getText()+ "\n"+ doc.getTags().toString() : doc.getText();
//		lucene_doc.add(new TextField(CONTENT, content, Field.Store.YES));
		lucene_doc.add(new	TextField(CONTENT,	
				new	BufferedReader(new InputStreamReader(new ByteArrayInputStream(doc.getText().getBytes()), "UTF-8"))));
		if(doc.getMetadata() != null)
			lucene_doc.add(new TextField(METADATA, doc.getMetadata().toString(), Field.Store.NO));
		return lucene_doc;
	}
	
	public String toString(){
		return new String ("ID:"+getId()+", Name:"+name+", URL:"+getUrl()+", Date:"+getCrawledDate());
	}

	
}