package edu.carleton.comp4601.assignment2.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jgraph.graph.Edge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
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
	public static final double TELEPORT_PROBABILITY = 0.5; // It's the Alpha
	public static final double STEADY_STATE_THRESHOLD = 0.01;
	public static final double HUBS_AUTHORITIES_THRESHOLD = 0.01;

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
//		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> results = new ArrayList<>();
		DirectedGraph<Integer, DefaultEdge> g = loadGraph(graph_id);
//		DirectedGraph<Integer, DefaultEdge> g = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
//		g.addVertex(1);
//		g.addVertex(2);
//		g.addVertex(3);
//		g.addEdge(1, 2);
//		g.addEdge(2, 1);
//		g.addEdge(2, 3);
//		g.addEdge(3, 2);
		
		if(g != null){
			g.removeVertex(0);
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
			
//			double[][] test ={	{0.02, 0.02, 0.88, 0.02, 0.02, 0.02, 0.02},
//					{0.02, 0.45, 0.45, 0.02, 0.02, 0.02, 0.02},
//					{0.31, 0.02, 0.31, 0.31, 0.02, 0.02, 0.02},
//					{0.02, 0.02, 0.02, 0.45, 0.45, 0.02, 0.02},
//					{0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.88},
//					{0.02, 0.02, 0.02, 0.02, 0.02, 0.45, 0.45},
//					{0.02, 0.02, 0.02, 0.31, 0.31, 0.02, 0.31}};
//
//			Matrix p = new Matrix(test);
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
		Matrix mP = x_distribution.times(p);
		mP = mP.times(1.0/mP.normInf());
		printMatrix(mP);
		Matrix subtraction = x_distribution.minus(mP);
		double result = subtraction.normF();
		System.out.println("err:"+result);
		if( result > STEADY_STATE_THRESHOLD){
			return getSteadyState(p, mP);
		}
		return mP;
	}
	
	public void calculateHITS(int graph_id){
//		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> results = new ArrayList<>();
		DirectedGraph<Integer, DefaultEdge> g = GraphManager.getDefault().loadGraph(graph_id);
		if(g != null){
			System.out.println("Graph: "+g);
			g.removeVertex(0);
			@SuppressWarnings({ "rawtypes", "unchecked" })
			List<Integer> nodes = new ArrayList(g.vertexSet());
			Collections.sort(nodes);
			System.out.println("Nodes After Sorting:\n"+nodes.toString());
			int n = nodes.size();
			Matrix adjacencyMatrix = new Matrix(n, n);	// Adjacency Matrix (A)
														// n number of rows and columns
//			double[][] test ={	{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0},
//					{0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0},
//					{1.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0},
//					{0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0},
//					{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0},
//					{0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0},
//					{0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 1.0}};
//
//			Matrix adjacencyMatrix = new Matrix(test);

			
			// Retrieve rows to assign values to the Adjacency Matrix (A)
			for(int i =0; i < nodes.size(); i++){
				// columns
				Set<DefaultEdge> filledSpots = g.outgoingEdgesOf(nodes.get(i));
				double value = (filledSpots.size() == 0) ? 0.0 : (1.0);
				
				System.out.print("Row ("+i+"): [filledSpots="+filledSpots.size()+", value="+value+"]");
				
				for(DefaultEdge e : filledSpots){
//					System.out.print(e.toString()+" ");
					int column_num = nodes.indexOf(g.getEdgeTarget(e));
					adjacencyMatrix.set(i, column_num, value);
				}
				System.out.println();
			}

			System.out.println("Adjacency Matrix (A):");
			double[][] valsTransposed = adjacencyMatrix.getArray();
			// now loop through the rows of valsTransposed to print
			for(int i = 0; i < valsTransposed.length; i++) {
			    System.out.printf("%4s", nodes.get(i)+": ");
			    for(int j = 0; j < valsTransposed[i].length; j++) {        
			        System.out.printf(" %.4f", valsTransposed[i][j]);
			    }
			    System.out.println();
			}
			
			Matrix init_weight = new Matrix(n, 1); 	// initial weight vector 
			for(int i=0; i<n; i++)					// should look something like [ 1/n ]
				init_weight.set(i, 0, 1.0/n);			//							  [ 1/n ]
													//							  [  .  ]
													//							  [  .  ]
													//							  [ 1/n ]

			//////////////////////////////////////////////////////////////////////////////////////////
			//																						//
			// FOR MORE INFO VISIT:																	//
			// http://www.math.cornell.edu/~mec/Winter2009/RalucaRemus/Lecture4/lecture4.html		//
			//																						//
			//////////////////////////////////////////////////////////////////////////////////////////
			
			
			// [1] First compute (v) the Authority weight vector
			// v = A(transpose) x init_weight
			Matrix v = adjacencyMatrix.transpose().times(init_weight);
			
			// [2] Then compute updated Hub weight (u)
			Matrix u = adjacencyMatrix.times(v);
			
			
			Matrix authority = computeAuthority(adjacencyMatrix, v);
			
			Matrix hub = computeHub(adjacencyMatrix, u);

			System.out.print("Final Authority: ");
			printMatrix(authority);

			System.out.print("Final Hub: ");
			printMatrix(hub);

			
			DBCursor cursor = DocumentsManager.getDefault().getCollectionCursor();
			while(cursor.hasNext()){
				BasicDBObject obj = (BasicDBObject) cursor.next();
				edu.carleton.comp4601.assignment2.dao.Document doc = edu.carleton.comp4601.assignment2.dao.Document.getDocumentFrom(obj);
		    	System.out.println("trying to pagerank:" + doc.getUrl());
		    	if(doc != null){
					System.out.println("Page: docid="+doc.getId()+" index="+doc.getIndex());
		    		if(nodes.contains(doc.getId())){
		    			int index = nodes.indexOf(doc.getId());
						doc.setAuthority((float) (authority.get(0, index)));
						doc.setHub((float) (hub.get(0, index)));
		    		}
					DocumentsManager.getDefault().updateScore(doc.getId(), doc.getScore());
					System.out.println(" authority="+doc.getAuthority()+" hub="+doc.getHub());
		    	}
			}
			
			System.out.println("Graph: "+g);
		}
	}
	
	public Matrix computeHub(Matrix adjacencyMatrix, Matrix u){
		Matrix mHub = adjacencyMatrix.times(adjacencyMatrix.transpose()).times(u);
		mHub = mHub.times(1.0/mHub.norm1());
		printMatrix(mHub);
		Matrix subtraction = u.minus(mHub);
		double result = subtraction.normF();
		System.out.println("Hub norm="+result);
		if(result > HUBS_AUTHORITIES_THRESHOLD){
			return computeHub(adjacencyMatrix, mHub);
		}
		return mHub.transpose();
	}
	
	public Matrix computeAuthority(Matrix adjacencyMatrix, Matrix v){
		Matrix mAuthority = adjacencyMatrix.transpose().times(adjacencyMatrix).times(v);
		mAuthority = mAuthority.times(1.0/mAuthority.norm1());
		printMatrix(mAuthority);
		Matrix subtraction = v.minus(mAuthority);
		double result = subtraction.normF();
		System.out.println("Authority norm="+result);
		if(result > HUBS_AUTHORITIES_THRESHOLD){
			return computeAuthority(adjacencyMatrix, mAuthority);
		}
		return mAuthority.transpose();
	}

	
	public void printMatrix(Matrix m){
		double[][] vals = m.getArray();
		// now loop through the rows of valsTransposed to print
		for(int i = 0; i < vals.length; i++) {
		    for(int j = 0; j < vals[i].length; j++) {        
		        System.out.printf(" %.2f", vals[i][j]);
		    }
		    System.out.println();
		}
	}
	
	public void noBoost(){
		DocumentsManager.getDefault().updateAllScores(1);
	}
	
	public static void main(String[] args){
		GraphManager manager = GraphManager.getDefault();
		manager.calculatePageRank(Controller.DEFAULT_CRAWL_GRAPH_ID);
		manager.calculateHITS(Controller.DEFAULT_CRAWL_GRAPH_ID);
	}

}
