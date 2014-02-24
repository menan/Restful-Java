package edu.carleton.COMP4601.assignment1.persistence;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public abstract class AbstractMongoDBManager {
	
	/**
	 * This class is meant to be a generic type of manager for a MongoDB database.
	 * Implemented functionalities are commonly used, ones like search, add, update, etc.
	 * User wishes to create a specified manager to store a one type of a class; could
	 * subclass this abstract class and pass in the <Class> wants to store in MongoDB.
	 * It also make it easier for user to connect to a MongoDB.
	 */
	
	private static String DEFAULT_HOST = "localhost";
	private static int DEFAULT_PORT = 27017;
	
	
	protected DB db;
	protected MongoClient mongoClient;
	protected DBCollection collection;
	@SuppressWarnings("rawtypes")
	protected Class objecClass;
	protected String collection_name;

	
	abstract boolean setupTable();
	public abstract String getClassName();
	public abstract String getCollectionName();


	
	protected AbstractMongoDBManager(String db_name, String collection_name, @SuppressWarnings("rawtypes") Class objecClass){
		this(DEFAULT_HOST, DEFAULT_PORT, db_name, collection_name, objecClass);
	}
	
	protected AbstractMongoDBManager(String host, int port, String db_name,
			String collection_name, @SuppressWarnings("rawtypes") Class objecClass){
		try {
			mongoClient = new MongoClient( host , port );
			db = mongoClient.getDB(db_name);
	        collection = db.getCollection(collection_name);
	        this.objecClass = objecClass;
	        if(this.objecClass != null)
	        	collection.setObjectClass(objecClass);
	        this.collection_name = collection_name;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	/**
	 * Adds given object to the database.
	 * Returns WriteResult.
	 * 
	 * @param BasicDBObject
	 * @return
	 */
	public synchronized WriteResult add(BasicDBObject doc){
		if(collection != null)
			return collection.insert(doc, WriteConcern.SAFE);
		else
			return null;
	}

	
	/**
	 * Updates given object by replacing old value (using $set) with new_value.
	 * Returns true if update is successful, otherwise returns false.
	 * 
	 * @param field
	 * @param new_value
	 * @param searchQuery
	 * @return
	 */
	public synchronized boolean update(String field, Object new_value, BasicDBObject searchQuery){
				
		if(collection != null && field != null && new_value != null && searchQuery != null){
			BasicDBObject newDocument = new BasicDBObject();
			// To update a particular value only, uses $set update modifier.
			newDocument.append("$set", new BasicDBObject().append(field.toLowerCase(), new_value));
			 
			WriteResult wr = collection.update(searchQuery, newDocument);
			
			if(!wr.getLastError().ok())
				return false;
			else
				return true;
		}
		else
			return false;

	}
	
	/**
	 * Updates all objects in the database that match the given criteria.
	 * Returns true if update is successful, otherwise returns false.
	 * 
	 * @param field
	 * @param new_value
	 * @param searchQuery
	 * @return
	 */
	public synchronized boolean updateAll(String field, Object new_value, BasicDBObject searchQuery){
				
		if(collection != null && field != null && new_value != null && searchQuery != null){
			BasicDBObject newDocument = new BasicDBObject();
			// To update a particular value only, uses $set update modifier.
			newDocument.append("$set", new BasicDBObject().append(field.toLowerCase(), new_value));
			 
			WriteResult wr = collection.update(searchQuery, newDocument,false,true);
			
			if(!wr.getLastError().ok())
				return false;
			else
				return true;
		}
		else
			return false;

	}
	
	/**
	 * Find a document in the database that has the given field.
	 * Returns a BasicDBObject if such a document was found.
	 * 
	 * @param field
	 * @return BasicDBObject
	 */
	public synchronized DBObject find(String field){
		if(collection != null){
		    DBObject query = new BasicDBObject(field.toLowerCase(), new BasicDBObject("$exists", true));
			return collection.find(query).next();
		}
		return null;
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
	public synchronized List<DBObject> findAll(String field){
		List<DBObject> ans = null;
	    DBObject query = new BasicDBObject(field.toLowerCase(), new BasicDBObject("$exists", true));
	    if(collection != null){
	    	ans = new ArrayList<DBObject>();
		    DBCursor cursor = collection.find(query);
		    return cursor.toArray();
	    }
	    return ans;
	}
	
	/**
	 * Find all documents in the database that has the given field where n represents the limit 
	 *  number of returned elements and index is the number of discard ones at the beginning of 
	 *  of the cursor.
	 * Returns a List<DBObject> if documents were found.
	 * 
	 * @param field
	 * @param n
	 * @param index
	 * @return List<DBObject>
	 */
	public synchronized List<DBObject> findNDocsStatringAtIndex(String field, int n, int index) throws 
	IllegalStateException, MongoException{
	    DBObject query = new BasicDBObject(field.toLowerCase(), new BasicDBObject("$exists", true));
	    if(collection != null && n > 0){
		    DBCursor cursor = collection.find(query).limit(n).skip(index);
		    return cursor.toArray();
	    }
	    return null;
	}
	
	
	/**
	 * Deletes a document that has the same value of the passed <field>.
	 * Returns true if successful, otherwise returns false.
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public synchronized boolean delete(String field, Object value){

		if(collection != null){
			WriteResult wr = collection.remove(new BasicDBObject().append( field , value));
			if(!wr.getLastError().ok()){
				System.out.println("AbstractMongoDBManager - deleting document failed");
				return false;
			}
			return true;
		}
		return false;
	}

	
	/**
	 * Deletes all documents that have a value of the <field> equals
	 * to one of the listed values in the given <list>.
	 * Returns true if successful, otherwise returns false.
	 * 
	 * @param field
	 * @param list
	 * @return
	 */
	public synchronized boolean delete(String field,List<Object> list){
		BasicDBObject deleteQuery = new BasicDBObject();
		deleteQuery.put(field.toLowerCase(), new BasicDBObject("$in", list));

		if(collection != null){
			DBCursor cursor = collection.find(deleteQuery);
			while (cursor.hasNext()) {
			    DBObject item = cursor.next();
			    collection.remove(item);
			}	
		}
		return false;
	}

	
	/**
	 * Deletes all documents that have a <field> of the specified <value>.
	 * Returns true if successful, otherwise returns false.
	 * 
	 * @param field
	 * @return
	 */
	public synchronized boolean deleteAll(String field, String value){
		String expression = ".*((?i)"+value+").*";

	    DBObject deleteQuery = new BasicDBObject(field.toLowerCase(), new BasicDBObject("$regex", expression));

		return deleteAll(deleteQuery);
	}
	
	
	/**
	 * Deletes all documents that meet the <deleteQuery> criteria.
	 * Returns true if successful, otherwise returns false.
	 * 
	 * @param deleteQuery
	 * @return
	 */
	public synchronized boolean deleteAll(DBObject deleteQuery){
		if(collection != null){
			DBCursor cursor = collection.find(deleteQuery);
			boolean returnVal = cursor.hasNext();
			while (cursor.hasNext()) {
			    DBObject item = cursor.next();
			    System.out.println("item:" + item.get("name"));
			    returnVal = returnVal && collection.remove(item).getLastError().ok();
			}
			return returnVal;
		}
		return false;
	}
	

	/**
	 * Pass an empty BasicDBObject deletes all documents in the database.
	 * 
	 * @return
	 */
	public synchronized boolean deleteAll(){
		if(collection != null)
			return collection.remove(new BasicDBObject()).getLastError().ok();
		return false;
	}
	
	/**
	 * It deletes the entire documents and drop the collection.
	 * 
	 * @return
	 */
	public synchronized boolean drop(){
		if(collection != null){
			collection.drop();
			return true;
		}
		return false;
	}

	/**
	 * Find all documents in the database that contains given string in a certain <field> where 
	 * n represents the limit number of returned elements and index is the number of discard 
	 * ones at the beginning of the cursor. User passes a true value for <isExactMatch> if 
	 * looking for an exact match of the passed string; otherwise pass in false.
	 * Returns a List<DBObject> if documents were found.
	 * 
	 * @param field
	 * @param stringToSearch
	 * @param isExactMatch
	 * @param n
	 * @param index
	 * @return List<DBObject>
	 */
	public synchronized List<DBObject> search(String field, String stringToSearch, boolean isExactMatch, int n, int index) throws 
	IllegalStateException, MongoException{
		String expression;
	    if(isExactMatch)
	    	expression = ".*"+stringToSearch+".*";
	    else
	    	expression = ".*((?i)"+stringToSearch+").*";

	    BasicDBObject query  = new BasicDBObject("$regex", expression);
	    if(collection != null && n > 0){
		    DBCursor cursor = collection.find(query).limit(n).skip(index);
		    return cursor.toArray();
	    }
	    return null;
	}
	
	/**
	 * Find all documents in the database that contains given string in a certain <field>.
	 * User passes a true value for isExactMatch if looking for an exact match of the passed string.
	 * Returns a List<DBObject> if documents were found.
	 * 
	 * @param field
	 * @param stringToFind
	 * @param isExactMatch
	 * @return List<DBObject>
	 */
	public synchronized List<DBObject> search(String field, String stringToFind, boolean isExactMatch){
		String expression;
	    if(isExactMatch)
	    	expression = ".*"+stringToFind+".*";
	    else
	    	expression = ".*((?i)"+stringToFind+").*";

	    DBObject query = new BasicDBObject(field.toLowerCase(), new BasicDBObject("$regex", expression));

	    if(collection != null){
		    DBCursor cursor = collection.find(query);
		    return cursor.toArray();
	    }
	    return null;
	}
	
	/**
	 * Find all documents in the database that contains the given <objToFind> in a certain <field>.
	 * NOTE: AND operator is used in case a List<Object> was passed.
	 * Returns a List<DBObject> if documents were found.
	 * 
	 * @param field
	 * @param objToFind
	 * @return List<DBObject>
	 */
	public synchronized List<DBObject> search(String field, Object objToFind){
		BasicDBObject query;

		if(objToFind instanceof List){
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) objToFind;
			ArrayList<BasicDBObject> x = new ArrayList<BasicDBObject>();
			for(Object o : list){
				x.add(new BasicDBObject(field.toLowerCase(), o));
			}
			query = new BasicDBObject("$and",x);
		}
		else
			query = new BasicDBObject(field.toLowerCase(), new BasicDBObject("$in", objToFind));


	    
	    if(collection != null){
		    DBCursor cursor = collection.find(query);
		    return cursor.toArray();
	    }
	    return null;
	}

	/**
	 * Checks wither a document exist or not.
	 * Returns true if a document was found, otherwise it returns false.
	 * 
	 * @param field
	 * @param stringToFind
	 * @return boolean
	 */
//	public synchronized boolean exists(String field, Object objToFind){
//		BasicDBObject deleteQuery = new BasicDBObject(field.toLowerCase(), objToFind));
//
//		if(collection != null){
//			DBCursor cursor = collection.find(deleteQuery);
//			return cursor.hasNext();
//		}
//		return false;
//
//	}
	
}

