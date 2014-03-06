package edu.carleton.comp4601.assignment2.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jgraph.graph.Edge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import Jama.Matrix;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import edu.carleton.comp4601.assignment2.crawler.Controller;
import edu.carleton.comp4601.assignment2.dao.Document;
import edu.carleton.comp4601.assignment2.utility.Marshaller;


public class GraphManager extends AbstractMongoDBManager{

	private static GraphManager manager;
	private static String DEFAULT_DB = "sda";
	private static String DEFAULT_COLLECTION = "graph";
	public static final double TELEPORT_PROBABILITY = 0.1; // It's the Alpha
	public static final double STEADY_STATE_THRESHOLD = 0.02;
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
	
	public boolean save(DirectedGraph<Integer, DefaultEdge> graph, int graph_id) {
		BasicDBObject obj = new BasicDBObject("id", graph_id);
		try {
			obj.put("data", Marshaller.serializeObject(graph));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return save(obj);
	}
	

	@SuppressWarnings("unchecked")	
	public void calculatePageRank(int graph_id){
		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> results = new ArrayList<>();
		DirectedGraph<Integer, DefaultEdge> g = loadGraph(graph_id);
		if(g != null){
//			g.removeVertex(0);
			@SuppressWarnings("rawtypes")
			List<Integer> nodes = new ArrayList(g.vertexSet());
			Collections.sort(nodes);
			System.out.println("Nodes After Sorting:\n"+nodes.toString());
			int n = nodes.size();
			Matrix p = new Matrix(n, n);	// Transition Probability Matrix
											// n number of rows and columns
			// rows
			for(int i =0; i < nodes.size(); i++){
				// columns
				Set<DefaultEdge> filledSpots = g.outgoingEdgesOf(nodes.get(i));
				double value = (filledSpots.size() == 0) ? 0.0 : (1.0/filledSpots.size());
				double probability = value * (1-TELEPORT_PROBABILITY);
				double valueToAdd = TELEPORT_PROBABILITY/n;
				System.out.print("Row ("+i+"): [filledSpots="+filledSpots.size()+", value="+value+", probability="+probability+" valueToAdd="+valueToAdd+"]");
				for(int j = 0; j < nodes.size() ; j++){
					p.set(i, j, valueToAdd);
				}
				
				for(DefaultEdge e : filledSpots){
//					System.out.print(e.toString()+" ");
					int column_num = nodes.indexOf(g.getEdgeTarget(e));
					p.set(i, column_num, p.get(i, column_num) + probability);
				}
				System.out.println();
			}

			System.out.println("Transition Probability Matrix (P):");
			double[][] valsTransposed = p.getArray();
			// now loop through the rows of valsTransposed to print
			for(int i = 0; i < valsTransposed.length; i++) {
			    System.out.printf("%4s", nodes.get(i)+": ");
			    for(int j = 0; j < valsTransposed[i].length; j++) {        
			        System.out.printf(" %.4f", valsTransposed[i][j]);
			    }
			    System.out.println();
			}
			
			Matrix x0 = new Matrix(1, n); 	// initial probability distribution vector 
			x0.set(0, 0, 1.0);				// should look something like {1 0 0 ...}
			Matrix result = getSteadyState(p, x0);
			System.out.print("Final Page Rank: ");
			printMatrix(result);

			DBCursor cursor = DocumentsManager.getDefault().getCollectionCursor();
			while(cursor.hasNext()){
				BasicDBObject obj = (BasicDBObject) cursor.next();
				edu.carleton.comp4601.assignment2.dao.Document doc = edu.carleton.comp4601.assignment2.dao.Document.getDocumentFrom(obj);
		    	System.out.println("trying to pagerank:" + doc.getUrl());
		    	if(doc != null){
					System.out.println("Page: docid="+doc.getId()+" index="+doc.getIndex());
		    		if(nodes.contains(doc.getId())){
		    			int index = nodes.indexOf(doc.getId());
						doc.setScore((float) (doc.getIndex()*result.get(0, index)));
		    		}else{
						doc.setScore(0.0f);
		    		}
					DocumentsManager.getDefault().updateScore(doc.getId(), doc.getScore());
					System.out.println(" score="+doc.getScore());
		    	}
			}
			
			System.out.println("Graph: "+g);
		}
	}
	
	public Matrix getSteadyState(Matrix p, Matrix x_distribution){
		Matrix m = x_distribution.times(p);
		printMatrix(m);
		Matrix subtraction = x_distribution.minus(m);
		double result = subtraction.normF();
		System.out.println(result);
		if( result > STEADY_STATE_THRESHOLD){
			return getSteadyState(p, m);
		}
		return m;
	}
	
	public void printMatrix(Matrix m){
		double[][] vals = m.getArray();
		// now loop through the rows of valsTransposed to print
		for(int i = 0; i < vals.length; i++) {
		    for(int j = 0; j < vals[i].length; j++) {        
		        System.out.printf(" %.5f", vals[i][j]);
		    }
		    System.out.println();
		}

	}
	
	public void noBoost(){
		DocumentsManager.getDefault().updateScore(1);
	}
	
	public static void main(String[] args){
		GraphManager manager = GraphManager.getDefault();
		manager.calculatePageRank(Controller.DEFAULT_CRAWL_GRAPH_ID);
	}

}
