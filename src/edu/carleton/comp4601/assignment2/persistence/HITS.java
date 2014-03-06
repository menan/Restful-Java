package edu.carleton.comp4601.assignment2.persistence;

import java.util.*;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.carleton.comp4601.assignment2.dao.Document;
public class HITS {


	/** The data structure containing the Web linkage graph */
	private DirectedGraph<Integer, DefaultEdge> graph;

	/** A <code>Map</code> containing the Hub score for each page */
	private Map<Integer, Double> hubScores;
	
	/** A <code>Map</code> containing the Authority score for each page */
	private Map<Integer, Double> authorityScores;

	private ArrayList<Document> results;
	
	/** 
	 * Constructor for HITS
	 * @param results 
	 * 
	 * @param graph The data structure containing the Web linkage graph
	 */
	public HITS ( ArrayList<Document> results, DirectedGraph<Integer, DefaultEdge> graph ) {
		this.graph = graph;
		this.results = results;
		this.hubScores = new HashMap<Integer, Double>();
		this.authorityScores = new HashMap<Integer, Double>();
		int numLinks = results.size();
		for(int i=0; i<numLinks; i++) {
			int index = results.get(i).getId();
			hubScores.put(new Integer(index),new Double(1));
			authorityScores.put(new Integer(index),new Double(1));
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
//			System.out.println("Results size:" + results.size());
			
			for (int i = 0; i < results.size(); i++){
				
				int index = results.get(i).getId();
//				System.out.println("HITS id : " + index );
				
				Set<DefaultEdge> inlinks = graph.incomingEdgesOf(index);
				Set<DefaultEdge> outlinks = graph.outgoingEdgesOf(index);
//				
				Iterator<DefaultEdge> inIter = inlinks.iterator();
				Iterator<DefaultEdge> outIter = outlinks.iterator();
//				
				double authorityScore = 0;
				double hubScore = 0;

				while (inIter.hasNext()) {
//					System.out.println("inner id: " + inIter.next());
					authorityScore += ((Double)(hubScores.get(index))).doubleValue();
					inIter.next();
				}
				while (outIter.hasNext()) {
//					System.out.println("out id: " + outIter.next());
					hubScore += ((Double)(authorityScores.get((index)))).doubleValue();
					outIter.next();
				}

				Double authorityScore2 = (Double)(authorityScores.get(new Integer(index)));
				Double hubScore2 = (Double)(hubScores.get(new Integer(index)));
				if(authorityScore2.doubleValue() != authorityScore) {
					change = true;
					authorityScores.put(new Integer(index),new Double(authorityScore));
				}
				
				if(hubScore2.doubleValue() != hubScore) {
					change = true;
					hubScores.put(new Integer(index),new Double(hubScore));
				}
			}
		}
	}
	

	
	public void printScores(){
		System.out.println("Calculated Using HITS Algorithm: ");

		for (int i = 0; i < results.size(); i++){
			int index = results.get(i).getId();
			
			System.out.println("DocID:" + results.get(i).getUrl() + " Authority Score:" + authorityScores.get(index) + " Hub Score: " + hubScores.get(index));
		
		}
	}
}
