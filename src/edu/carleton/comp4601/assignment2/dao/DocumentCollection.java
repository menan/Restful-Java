package edu.carleton.comp4601.assignment2.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import edu.carleton.comp4601.assignment2.dao.Document;
import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;
import edu.carleton.comp4601.assignment2.persistence.LuceneManager;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DocumentCollection {
	@XmlElement(name="documents")
	private List<Document> documents;
	
	public DocumentCollection(){
		DocumentsManager manager = DocumentsManager.getDefault();
		documents = DocumentsManager.convertDBObject(manager.findAll("id"));
		setDocuments(documents);
	}
	
	/**
	 * Search for tags that are passed as a String in a format like:
	 * tag1:tag2:tag3:.... Each document in the database will be checked if has
	 * ALL the passed tags. Lacking one of the passed tags will, a document will
	 * be not be included in the returned result.
	 * 
	 * @param tags_string
	 * @return
	 */
	public List<Document> search(String tags_string){

		List<Document> 	results = LuceneManager.getDefault().query(tags_string, 100);

		if (results != null && results.size() > 0){
		}
		else{
			System.out.println("no results returned");
		}
		
//		List<String> 	tags = new ArrayList<String>(Arrays.asList(tags_string.split(":")));
//		List<DBObject> 	resultsObj = DocumentsManager.getDefault().search("tags", tags);
//		List<Document> 	results = new ArrayList<Document>();
//		if (resultsObj != null && resultsObj.size() > 0){
//			results = DocumentsManager.convertDBObject(resultsObj);
//		}
//		else{
//			System.out.println("no results returned");
//		}
		return results;
		
	}
	
	public List<Document> scoreSort(List<Document> docs){
		if(docs != null && docs.size() > 0){
			Set<Document> docSet = new TreeSet<Document>(new docidComparator()); // to eliminate duplicates
			docSet.addAll(docs);
			List<Document> result= new ArrayList<Document>(docSet);
			Collections.sort(result, new ScoreComparator()); // to sort docs based on score

			return result;
		}
		return docs;

	}
	
	public List<Document> authoritySort(List<Document> docs){
		if(docs != null && docs.size() > 0){
			Collections.sort(docs, new AuthorityComparator()); // to sort docs based on score
			Set<Document> docSet = new TreeSet<Document>(new docidComparator()); // to eliminate duplicates
			docSet.addAll(docs);
			return new ArrayList<Document>(docSet);
		}
		return docs;
	}

	/**
	 * Creates a new instance object of the Document class, added to the MongoDB then
	 * return that new object to caller.
	 * @param id
	 * @param name
	 * @param tags
	 * @param links
	 * @param text
	 * @return
	 */
	public Document create(int id, String name, String tags, String links, String text) {
		Document a = new Document(id);
		a.setName(name);
		a.setText(tags);
		System.out.println("Creating document with name:" + name);
		try {
			DocumentsManager.getDefault().create(id,name,tags,links,text);
			documents.add(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return a;
	}

	/**
	 * This method is used to update an existing field in the database. After finding
	 * the document with the passed <id>, this method basically checks for all each 
	 * parameter separately and checks if it has a value; then take that value and 
	 * set the specified field to that value.
	 * 
	 * @param id
	 * @param name
	 * @param tags
	 * @param links
	 * @param text
	 * @return boolean
	 */
	
	public boolean update(int id, String name, String tags, String links, String text) {
		BasicDBObject searchQuery = new BasicDBObject().append("id", id);
		boolean result = true;
		if(name!=null)
			result = result && DocumentsManager.getDefault().update("name", name, searchQuery);
		if(tags!=null)
			result = result && DocumentsManager.getDefault().update("tags", new ArrayList<String>(Arrays.asList(tags.split(":"))), searchQuery);
		if(links!=null)
			result = result && DocumentsManager.getDefault().update("links", new ArrayList<String>(Arrays.asList(links.split(" "))), searchQuery);
		if(text!=null)
			result = result && DocumentsManager.getDefault().update("text", text, searchQuery);
		
			
		return result;
	}
	
	/**
	 * Returns the size of the "documents" collection.
	 * @return
	 */	
	public int size() {
		return documents.size();
	}

	/**
	 * Return a list of current documents inside of the "documents" collection in MongoDB.
	 * @return
	 */
	public List<Document> getDocuments() {
		setDocuments(DocumentsManager.convertDBObject(DocumentsManager.getDefault().findAll("id")));
		return documents;
	}

	/**
	 * Set the list of documents to what's passed.
	 * @param documents
	 */
	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}
	
	public boolean reset(){
		return DocumentsManager.getDefault().deleteAll(); // && LuceneManager.getDefault().reset();
	}
	
	private class ScoreComparator implements Comparator<Document>{
	    @Override
	    public int compare(Document d1, Document d2) {
	    	if(d1 != null && d2 != null)
	    		return Double.compare(d2.getScore(),d1.getScore());
	    	else
	    		return 0;
	    }
	}
	
	private class AuthorityComparator implements Comparator<Document>{
	    @Override
	    public int compare(Document d1, Document d2) {
	    	if(d1 != null && d2 != null)
	    		return Double.compare(d2.getAuthority(),d1.getAuthority());
	    	else
	    		return 0;
	    }
	}
	
	private class docidComparator implements Comparator<Document>{
	    @Override
	    public int compare(Document d1, Document d2) {
	    	if(d1 != null && d2 != null)
	    		return Double.compare(d2.getId(),d1.getId());
	    	else
	    		return 0;
	    }
	}
	
	
	
}