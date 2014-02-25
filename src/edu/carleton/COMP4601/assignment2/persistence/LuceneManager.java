package edu.carleton.comp4601.assignment2.persistence;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
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


    String crawlStorageFolder = "/Volumes/My Passport/School/workspace/data/crawler/root";
	int hitsPerPage = 10;
	String field = "contents";
	StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
	
    public void indexToLucene(String field, String value){
    	Directory index = new RAMDirectory();

    	IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);

    	IndexWriter w;
		try {
			w = new IndexWriter(index, config);
	    	addDoc(w, "Lucene in Action", "193398817");
	    	addDoc(w, "Lucene for Dummies", "55320055Z");
	    	addDoc(w, "Managing Gigabytes", "55063554A");
	    	addDoc(w, "The Art of Computer Science", "9900333X");
	    	w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	
    }
    private static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
    	org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
    	  doc.add(new TextField("title", title, Field.Store.YES));
    	  doc.add(new StringField("isbn", isbn, Field.Store.YES));
    	  w.addDocument(doc);
	}
    
    public void search(String query) throws IOException, ParseException{
    	Directory dir = FSDirectory.open(new File(crawlStorageFolder));
    	QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
    	Query q = parser.parse(query);

    	IndexReader reader = DirectoryReader.open(dir);

    	IndexSearcher searcher = new IndexSearcher(reader);
    	
    	TopDocs results = searcher.search(q, 5 * hitsPerPage);
    	System.out.println("Searching for: " + query.toString());
    	
    	ScoreDoc[] hits = results.scoreDocs;
    	
    	for(int i = 0; i < hits.length; i++){
    		int docId = hits[i].doc;
    		Document d = searcher.doc(docId);
    		System.out.println(d.get("filename"));
    	}

		System.out.println(hits.length + " resuts found");
		
    	 
    }

}
