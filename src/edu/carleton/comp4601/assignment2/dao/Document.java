package edu.carleton.comp4601.assignment2.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.mongodb.BasicDBObject;

@XmlRootElement
public class Document extends BasicDBObject{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private Integer score;
	private String name;
	private String text;
	private ArrayList<String> tags;
	private ArrayList<String> links;
	private Map<String,Object> metadata;

	public Document() {
		tags = new ArrayList<String>();
		links = new ArrayList<String>();
		metadata = new HashMap<String, Object>();
	}

	public Document(Integer id) {
		this();
		this.id = id;
	}

	@SuppressWarnings("unchecked")
	public Document(Map<?, ?> map) {
		this();
		this.id = (Integer) map.get("id");
		this.score = (Integer) map.get("score");
		this.name = (String) map.get("name");
		this.text = (String) map.get("text");
		this.tags = (ArrayList<String>) map.get("tags");
		this.links = (ArrayList<String>) map.get("links");
		this.metadata = (Map<String, Object>) map.get("metadata");
	}

	public Integer getId() {
		return id;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public Integer getScore() {
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

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

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

	public String getURL(){
		return "/COMP4601SDA/rest/sda/" + id;
	}
	
	public String toHTML(){
		return "<tr><td><a href=\"" + getURL() + "\">" + id + "</a></td><td>" + name + "</td></tr>";
		
	}
}