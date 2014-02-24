package edu.carleton.COMP4601.assignment1.persistence;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import com.mongodb.DBObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

import edu.carleton.COMP4601.assignment1.Document;


public class DocumentsManager extends AbstractMongoDBManager {

	private static DocumentsManager manager;
	private static String DEFAULT_DB = "sda";
	private static String DEFAULT_COLLECTION = "documents";
	
	
	/**
	 * Shorten constructor for DocumentsManager where host and port will be substituted by
	 * their default values specified in class AbstractMongoDB. Parameter <objectClass>
	 * can have the value null in case there is no specific class this collection holds 
	 * into.
	 * Returns a new instance of class DocumentsManager.
	 * 
	 * @param db_name
	 * @param collection_name
	 * @param objectClass
	 * @return
	 */
	private DocumentsManager(String db_name, String collection_name, @SuppressWarnings("rawtypes") Class objectClass) {
		super(db_name, collection_name, objectClass);
	}
	
	private DocumentsManager(String host, int port, String db_name,
			String collection_name, @SuppressWarnings("rawtypes") Class objectClass) {
		super(host, port, db_name, collection_name, objectClass);
	}


	/**
	 * Returns default instance.
	 * 
	 * @return
	 */
	public static DocumentsManager getDefault() {
		
		if (manager == null) {
			manager = new DocumentsManager(DEFAULT_DB, DEFAULT_COLLECTION, null);
		}
		return manager;
	}

	
	@Override
	public boolean setupTable() {
		if(collection != null && this.drop()){
			collection = db.getCollection(this.collection_name);
			return (collection != null);
		}
		return false;
	}

	@Override
	public String getClassName() {
		if(objecClass == null)
			return "";
		return this.objecClass.getSimpleName();
	}

	@Override
	public String getCollectionName() {
		if(collection == null)
			return "";
		return collection.getName();
	}
	
	public static List<Document> convertDBObject(List<DBObject> list){
		List<Document> docs = new ArrayList<Document>(); 
		for(DBObject obj: list){
			docs.add(new Document(obj.toMap()));
		}
		return docs;
	}

	/**
	 * Find all documents in the database that has the given field.
	 * Returns a List<DBObject> if documents were found.
	 * 
	 * @param field
	 * @param new_value
	 * @param searchQuery
	 * @return List<DBObject>
	 */

	public boolean create(int id, String name, String tags, String links, String text) {
		Document a = new Document(id);
		a.setName(name);
		a.setText(text);
		a.setTags(new ArrayList<String>(Arrays.asList(tags.split(":"))));
		a.setLinks(new ArrayList<String>(Arrays.asList(links.split(" "))));
		
		return save(a);
	}

	
	public boolean remove(Document doc) {
		return remove(doc.getId());
	}
	
	public boolean remove(Integer doc_id) {
		if (this.load(doc_id) == null)
			return false;
		else
			return this.delete("id", doc_id);
	}
	public Document load(int id){
		BasicDBObject query = new BasicDBObject("id", id);
		DBCursor cursor = collection.find(query);
		if (cursor.count() > 0){
			BasicDBObject obj = (BasicDBObject) cursor.next();
			Document doc = new Document(obj.toMap());
			return doc;
		}
		else{
			return null;
		}

	}

	public boolean save(Document a){
		return this.add(a).getLastError().ok();
	}
	
	public synchronized WriteResult add(Document a){

		BasicDBObject doc = new BasicDBObject("id", a.getId());
		doc.put("name", a.getName());
		doc.put("text", a.getText());
		doc.put("tags", a.getTags());
		doc.put("links", a.getLinks());
		
		if(collection != null){
			return collection.insert(doc, WriteConcern.SAFE);
		}
		else
			return null;
	}
}
