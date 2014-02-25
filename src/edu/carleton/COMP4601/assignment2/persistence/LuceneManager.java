package edu.carleton.comp4601.assignment2.persistence;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

//import edu.carleton.comp4601.assignment2.dao.Document;

public class LuceneManager {


    String luceneStorageFolder = "/Volumes/My Passport/School/workspace/data/lucene/root";
	int hitsPerPage = 10;
	String field = "contents";
	StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);

	private static LuceneManager manager;

	private IndexWriter writer = null;
	
	
	private LuceneManager() {
		super();

		try {
	    	Directory index = FSDirectory.open(new File(luceneStorageFolder));
	    	IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			writer = new IndexWriter(index, config);
		} catch (IOException e) {
				e.printStackTrace();
		}
	}

	
	public static LuceneManager getDefault() {
		if (manager == null) {
			manager = new LuceneManager();
		}
		return manager;
	}
	

    public boolean indexDocument(String url, int i, Date date, String content, String metadata){
		Document doc = new Document();
		doc.add(new TextField("url", url, Field.Store.YES));
		doc.add(new IntField("docid", i, Field.Store.YES));
		doc.add(new StringField("date", date.toString(), Field.Store.YES));
		doc.add(new StringField("content", content, Field.Store.YES));
		doc.add(new StringField("metadata", metadata, Field.Store.YES));
		return addDocument(doc);
	}

    public boolean addDocument(Document d) {
		try {
	    	writer.addDocument(d);
	    	writer.close();
	    	return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
    
    
    
    public ScoreDoc[] search(String query, int thisHitsPerPage){
    	Directory dir;
		try {
			dir = FSDirectory.open(new File(luceneStorageFolder));
	    	QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
	    	Query q = parser.parse(query);
	    	IndexReader reader = DirectoryReader.open(dir);
	    	IndexSearcher searcher = new IndexSearcher(reader);
	    	TopDocs results = searcher.search(q, 5 * thisHitsPerPage);
	    	System.out.println("Searching for: " + query.toString());
	    	ScoreDoc[] hits = results.scoreDocs;
	    	for(int i = 0; i < hits.length; i++){
	    		int docId = hits[i].doc;
	    		Document d = searcher.doc(docId);
	    		System.out.println(d.get("filename"));
	    	}
			System.out.println(hits.length + " resuts found");
			return hits;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return null;
		}
    	 
    }



}
