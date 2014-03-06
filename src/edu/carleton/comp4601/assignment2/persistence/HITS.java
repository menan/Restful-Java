package edu.carleton.comp4601.assignment2.persistence;

import java.util.*;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
public class HITS {


	/** The data structure containing the Web linkage graph */
	private DirectedGraph<Integer, DefaultEdge> graph;

	/** A <code>Map</code> containing the Hub score for each page */
	private Map<Integer, Double> hubScores;
	
	/** A <code>Map</code> containing the Authority score for each page */
	private Map<Integer, Double> authorityScores;
	
	/** 
	 * Constructor for HITS
	 * 
	 * @param graph The data structure containing the Web linkage graph
	 */
	public HITS ( DirectedGraph<Integer, DefaultEdge> graph ) {
		this.graph = graph;
		this.hubScores = new HashMap<Integer, Double>();
		this.authorityScores = new HashMap<Integer, Double>();
		int numLinks = graph.vertexSet().size();
		for(int i=0; i<numLinks; i++) { 
			hubScores.put(new Integer(i),new Double(1));
			authorityScores.put(new Integer(i),new Double(1));
		}
		computeHITS();
	}


	public void computeHITS() {
		computeHITS(25);
	}

	public void computeHITS(int numIterations) {
		boolean change = true;
		while(numIterations-->0 && change) {
			change = false;
			System.out.println("Graph edge size:" + graph.edgeSet().size());
			for (int i = 0; i < graph.edgeSet().size(); i++){
//				Set<DefaultEdge> inlinks = graph.incomingEdgesOf(new Integer(i));
//				Set<DefaultEdge> outlinks = graph.outgoingEdgesOf(new Integer(i));
//				
//				Iterator<DefaultEdge> inIter = inlinks.iterator();
//				Iterator<DefaultEdge> outIter = outlinks.iterator();
//				
//				double authorityScore = 0;
//				double hubScore = 0;
				System.out.println("HITS id : " + i );

//				while (inIter.hasNext()) {
////					System.out.println("inner id: " + inIter.next());
//					authorityScore += ((Double)(hubScores.get(i))).doubleValue();
//				}
//				while (outIter.hasNext()) {
////					System.out.println("out id: " + outIter.next());
//					hubScore += ((Double)(authorityScores.get((i)))).doubleValue();
//				}

//				Double authorityScore2 = (Double)(authorityScores.get(new Integer(i)));
//				Double hubScore2 = (Double)(hubScores.get(new Integer(i)));
//				if(authorityScore2.doubleValue() != authorityScore) {
//					change = true;
//					authorityScores.put(new Integer(i),new Double(authorityScore));
//				}
//				
//				if(hubScore2.doubleValue() != hubScore) {
//					change = true;
//					hubScores.put(new Integer(i),new Double(hubScore));
//				}
			}
		}
	}
	
	public void printScores(){
		System.out.println("HITS: ");

		for (int i = 0; i < graph.vertexSet().size(); i++){
			
		
		}
	}
	

}
