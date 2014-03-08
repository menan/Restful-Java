package edu.carleton.comp4601.assignment2.persistence;

import java.util.*;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import Jama.Matrix;

import edu.carleton.comp4601.assignment2.dao.Document;
public class HITSManager {


	/** The data structure containing the Web linkage graph */
	private DirectedGraph<Integer, DefaultEdge> graph;

	/** A <code>Map</code> containing the Hub score for each page */
	private Map<Integer, Double> hubScores;
	
	/** A <code>Map</code> containing the Authority score for each page */
	private Map<Integer, Double> authorityScores;

	private ArrayList<Document> results;
	
	public static final int HUBS_AUTHORITIES_THRESHOLD = 1;

	/** 
	 * Constructor for HITS
	 * @param results 
	 * 
	 * @param graph The data structure containing the Web linkage graph
	 */
//	public HITSManager ( ArrayList<Document> results, DirectedGraph<Integer, DefaultEdge> graph ) {
//		this.graph = graph;
//		this.results = results;
//		this.hubScores = new HashMap<Integer, Double>();
//		this.authorityScores = new HashMap<Integer, Double>();
//		int numLinks = results.size();
//		for(int i=0; i<numLinks; i++) {
//			int index = results.get(i).getId();
//			hubScores.put(new Integer(index),new Double(1));
//			authorityScores.put(new Integer(index),new Double(1));
//		}
////		computeHITS();
//	}


//	public void computeHITS() {
//		computeHITS(25);
//	}
	
	@SuppressWarnings("unchecked")	
	public void calculateHITS(int graph_id){
		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> results = new ArrayList<>();
		DirectedGraph<Integer, DefaultEdge> g = GraphManager.getDefault().loadGraph(graph_id);
		if(g != null){
//			g.removeVertex(0);
			@SuppressWarnings("rawtypes")
			List<Integer> nodes = new ArrayList(g.vertexSet());
			Collections.sort(nodes);
			System.out.println("Nodes After Sorting:\n"+nodes.toString());
			int n = nodes.size();
			Matrix adjacencyMatrix = new Matrix(n, n);	// Adjacency Matrix (A)
														// n number of rows and columns
			
			// Retrieve rows to assign values to the Adjacency Matrix (A)
			for(int i =0; i < nodes.size(); i++){
				// columns
				Set<DefaultEdge> filledSpots = g.outgoingEdgesOf(nodes.get(i));
				double value = (filledSpots.size() == 0) ? 0.0 : (1.0/filledSpots.size());
				
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
			for(int i=0; i<n; i++){					// should look something like [ 1/n ]
				init_weight.set(i, 0, (double)(1/n));//							  [ 1/n ]
			}										//							  [  .  ]
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

			
//			DBCursor cursor = DocumentsManager.getDefault().getCollectionCursor();
//			while(cursor.hasNext()){
//				BasicDBObject obj = (BasicDBObject) cursor.next();
//				edu.carleton.comp4601.assignment2.dao.Document doc = edu.carleton.comp4601.assignment2.dao.Document.getDocumentFrom(obj);
//		    	System.out.println("trying to pagerank:" + doc.getUrl());
//		    	if(doc != null){
//					System.out.println("Page: docid="+doc.getId()+" index="+doc.getIndex());
//		    		if(nodes.contains(doc.getId())){
//		    			int index = nodes.indexOf(doc.getId());
//						doc.setScore((float) (doc.getIndex()*result.get(0, index)));
//		    		}else{
//						doc.setScore(0.0f);
//		    		}
//					DocumentsManager.getDefault().updateScore(doc.getId(), doc.getScore());
//					System.out.println(" score="+doc.getScore());
//		    	}
//			}
//			
//			System.out.println("Graph: "+g);
		}
	}
	
	public Matrix computeHub(Matrix adjacencyMatrix, Matrix u){
		Matrix new_u = adjacencyMatrix.times(adjacencyMatrix.transpose()).times(u);
		printMatrix(new_u);
		double result = new_u.normF();
		System.out.println("Hub norm="+result);
		if(result < HUBS_AUTHORITIES_THRESHOLD){
			return computeHub(adjacencyMatrix, new_u);
		}
		return new_u.transpose();
	}
	
	public Matrix computeAuthority(Matrix adjacencyMatrix, Matrix v){
		Matrix new_v = adjacencyMatrix.transpose().times(adjacencyMatrix).times(v);
		printMatrix(new_v);
		double result = new_v.normF();
		System.out.println("Authority norm="+result);
		if(result < HUBS_AUTHORITIES_THRESHOLD){
			return computeHub(adjacencyMatrix, new_v);
		}
		return new_v.transpose();
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

	
//	public void calculate(int graph_id){
//		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> results = new ArrayList<>();
//		DirectedGraph<Integer, DefaultEdge> g = loadGraph(graph_id);
//		if(g != null){
////			g.removeVertex(0);
//			@SuppressWarnings("rawtypes")
//			List<Integer> nodes = new ArrayList(g.vertexSet());
//			Collections.sort(nodes);
//			System.out.println("Nodes After Sorting:\n"+nodes.toString());
//			int n = nodes.size();
//			Matrix p = new Matrix(n, n);	// Transition Probability Matrix
//											// n number of rows and columns
//			// rows
//			for(int i =0; i < nodes.size(); i++){
//				// columns
//				Set<DefaultEdge> filledSpots = g.outgoingEdgesOf(nodes.get(i));
//				double value = (filledSpots.size() == 0) ? 0.0 : (1.0/filledSpots.size());
//				double probability = value * (1-TELEPORT_PROBABILITY);
//				double valueToAdd = TELEPORT_PROBABILITY/n;
//				System.out.print("Row ("+i+"): [filledSpots="+filledSpots.size()+", value="+value+", probability="+probability+" valueToAdd="+valueToAdd+"]");
//				for(int j = 0; j < nodes.size() ; j++){
//					p.set(i, j, valueToAdd);
//				}
//				
//				for(DefaultEdge e : filledSpots){
////					System.out.print(e.toString()+" ");
//					int column_num = nodes.indexOf(g.getEdgeTarget(e));
//					p.set(i, column_num, p.get(i, column_num) + probability);
//				}
//				System.out.println();
//			}

	
	
//	public void computeHITS(int numIterations) {
//		boolean change = true;
//		while(numIterations-->0 && change) {
//			change = false;
////			System.out.println("Results size:" + results.size());
//			
//			for (int i = 0; i < results.size(); i++){
//				
//				int index = results.get(i).getId();
////				System.out.println("HITS id : " + index );
//				
//				Set<DefaultEdge> inlinks = graph.incomingEdgesOf(index);
//				Set<DefaultEdge> outlinks = graph.outgoingEdgesOf(index);
////				
//				Iterator<DefaultEdge> inIter = inlinks.iterator();
//				Iterator<DefaultEdge> outIter = outlinks.iterator();
////				
//				double authorityScore = 0;
//				double hubScore = 0;
//
//				while (inIter.hasNext()) {
////					System.out.println("inner id: " + inIter.next());
//					authorityScore += ((Double)(hubScores.get(index))).doubleValue();
//					inIter.next();
//				}
//				while (outIter.hasNext()) {
////					System.out.println("out id: " + outIter.next());
//					hubScore += ((Double)(authorityScores.get((index)))).doubleValue();
//					outIter.next();
//				}
//
//				Double authorityScore2 = (Double)(authorityScores.get(new Integer(index)));
//				Double hubScore2 = (Double)(hubScores.get(new Integer(index)));
//				if(authorityScore2.doubleValue() != authorityScore) {
//					change = true;
//					authorityScores.put(new Integer(index),new Double(authorityScore));
//				}
//				
//				if(hubScore2.doubleValue() != hubScore) {
//					change = true;
//					hubScores.put(new Integer(index),new Double(hubScore));
//				}
//			}
//		}
//	}
	

	
	public void printScores(){
		System.out.println("Calculated Using HITS Algorithm: ");

		for (int i = 0; i < results.size(); i++){
			int index = results.get(i).getId();
			
			System.out.println("DocID:" + results.get(i).getUrl() + " Authority Score:" + authorityScores.get(index) + " Hub Score: " + hubScores.get(index));
		
		}
	}
}
