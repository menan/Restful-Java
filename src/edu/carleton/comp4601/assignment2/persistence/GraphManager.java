package edu.carleton.comp4601.assignment2.persistence;

import java.io.IOException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import edu.carleton.comp4601.assignment2.utility.Marshaller;


public class GraphManager extends AbstractMongoDBManager{

	private static GraphManager manager;
	private static String DEFAULT_DB = "sda";
	private static String DEFAULT_COLLECTION = "graph";
	
	
	/**
	 * Shorten constructor for GraphManager where host and port will be substituted by
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
	private GraphManager(String db_name, String collection_name, @SuppressWarnings("rawtypes") Class objectClass) {
		super(db_name, collection_name, objectClass);
	}
	
	private GraphManager(String host, int port, String db_name,
			String collection_name, @SuppressWarnings("rawtypes") Class objectClass) {
		super(host, port, db_name, collection_name, objectClass);
	}


	/**
	 * Returns default instance.
	 * 
	 * @return
	 */
	public static GraphManager getDefault() {
		
		if (manager == null) {
			manager = new GraphManager(DEFAULT_DB, DEFAULT_COLLECTION, null);
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
	

	public DirectedGraph<Integer, DefaultEdge> loadGraph(){
		return loadGraph(0);
	}
	
	@SuppressWarnings("unchecked")
	public DirectedGraph<Integer, DefaultEdge> loadGraph(int graph_id){
		try {
			return (DirectedGraph<Integer, DefaultEdge>) Marshaller.deserializeObject(load(graph_id));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	
	
	public byte[] load(){
		BasicDBObject query = new BasicDBObject("id", 0);
		DBCursor cursor = collection.find(query);
		if (cursor.count() > 0){
			BasicDBObject obj = (BasicDBObject) cursor.next();
			byte[] data = (byte[]) obj.get("data");
			return data;
		}
		else{
			return null;
		}

	}

	public byte[] load(int crawler_id){
		BasicDBObject query = new BasicDBObject("id", crawler_id);
		DBCursor cursor = collection.find(query);
		if (cursor.count() > 0){
			BasicDBObject obj = (BasicDBObject) cursor.next();
			byte[] data = (byte[]) obj.get("data");
			return data;
		}
		else{
			return null;
		}

	}

	
	public boolean save(BasicDBObject a){
		return this.add(a).getLastError().ok();
	}

	public boolean save(byte[] data) {
		return save(data, 0);
	}
	
	public boolean save(byte[] data, int crawler_id) {
		BasicDBObject graph = new BasicDBObject("id", crawler_id);
		graph.put("data", data);
		return save(graph);
	}

	
	public boolean save(DirectedGraph<Integer, DefaultEdge> graph) {
		return save(graph, 0);
	}
	
	public boolean save(DirectedGraph<Integer, DefaultEdge> graph, int crawler_id) {
		BasicDBObject obj = new BasicDBObject("id", crawler_id);
		try {
			obj.put("data", Marshaller.serializeObject(graph));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return save(obj);
	}
	
	public void computeHITS(int id){
		
	}

}
