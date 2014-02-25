package edu.carleton.comp4601.assignment2.resources;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.carleton.comp4601.assignment2.crawler.Controller;
import edu.carleton.comp4601.assignment2.dao.Document;
import edu.carleton.comp4601.assignment2.dao.DocumentCollection;
import edu.carleton.comp4601.assignment2.persistence.DocumentsManager;
import edu.carleton.comp4601.assignment2.persistence.LuceneManager;

public class SearchableDocumentArchive {

	private static SearchableDocumentArchive manager = null;
	
	public static String REST = "rest";
	public static String QUERY = "Query";
	public static String SEARCH = "Search";
	public static String SDA = "SDA";

	private DocumentCollection collection = new DocumentCollection();
	

	public static SearchableDocumentArchive getDefault() {
		if (manager == null) {
			manager = new SearchableDocumentArchive();
		}
		return manager;
	}
	
	private SearchableDocumentArchive() {
		super();

	}
	


    public void calculatePageRank(){
    	for(Document d: collection.getDocuments()){
    		System.out.println("gonna calculate page rank for" + d.get("id"));
    	}
    }


}
