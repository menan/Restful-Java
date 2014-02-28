package edu.carleton.comp4601.assignment2.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;


public class LuceneManager extends AbstractMongoDBManager {

	private static String INDEX_DIR = "/Users/abdulrahmanalamoudi/Desktop/temp/index";
	
	private static String DEFAULT_DB = "sda";
	private static String DEFAULT_COLLECTION = "index";

	int hitsPerPage = 10;
	

	private FSDirectory dir;
	private StandardAnalyzer analyzer;
	private IndexWriterConfig iwc;
	private IndexWriter writer;
	private static LuceneManager manager;

	
	/**
	 * Shorten constructor for LuceneManager where host and port will be substituted by
	 * their default values specified in class AbstractMongoDB. Parameter <objectClass>
	 * can have the value null in case there is no specific class this collection holds 
	 * into.
	 * Returns a new instance of class LuceneManager.
	 * 
	 * @param db_name
	 * @param collection_name
	 * @param objectClass
	 * @return
	 */
	private LuceneManager(String db_name, String collection_name, @SuppressWarnings("rawtypes") Class objectClass) {
		super(db_name, collection_name, objectClass);
		initLucene();
	}
	
	private LuceneManager(String host, int port, String db_name,
			String collection_name, @SuppressWarnings("rawtypes") Class objectClass) {
		super(host, port, db_name, collection_name, objectClass);
		initLucene();
	}
	
	private void initLucene(){
		analyzer = new StandardAnalyzer(Version.LUCENE_46);
		iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);
	}
	
	
	/**
	 * Returns default instance.
	 * 
	 * @return
	 */
	public static LuceneManager getDefault() {
		
		if (manager == null) {
			manager = new LuceneManager(DEFAULT_DB, DEFAULT_COLLECTION, null);
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
	
	public synchronized void index() {
		IndexWriter writer = null;
		FSDirectory dir = null;

		try {
			dir = FSDirectory.open(new File(INDEX_DIR));
			writer = new IndexWriter(dir, iwc);
			indexDocuments(writer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
				if (dir != null)
					dir.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}
	}


	private void indexDocuments(IndexWriter writer) throws IOException {
		DBCursor cursor = DocumentsManager.getDefault().getCollectionCursor();
		while(cursor.hasNext()){
			BasicDBObject obj = (BasicDBObject) cursor.next();
			edu.carleton.comp4601.assignment2.dao.Document doc = edu.carleton.comp4601.assignment2.dao.Document.getDocumentFrom(obj);
	    	System.out.println("trying to index:" + doc.getUrl());
	    	Document luceneDoc = edu.carleton.comp4601.assignment2.dao.Document.getLuceneDocFromDocument(doc);
			writer.addDocument(luceneDoc);
		}
	}
    
    
//	public ArrayList<edu.carleton.comp4601.assignment2.dao.Document> query(String searchString) {
//		try {
//			IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(INDEX_DIR)));
//			IndexSearcher searcher = new IndexSearcher(reader);
//			QueryParser parser = new QueryParser(Version.LUCENE_46, edu.carleton.comp4601.assignment2.dao.Document.CONTENT, analyzer);
//			Query q = parser.parse(searchString);
//			TopDocs results = searcher.search(q, 10);
//			ScoreDoc[] hits = results.scoreDocs;
//			reader.close();
//			return getDocs(hits);
//		}
//		catch (IOException | ParseException e)
//		{
//			e.printStackTrace();
//		}
//		return
//				null;
//	}
	
	public ArrayList<edu.carleton.comp4601.assignment2.dao.Document> query(String searchString, int n) {
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(INDEX_DIR)));
			IndexSearcher searcher = new IndexSearcher(reader);
			QueryParser parser = new QueryParser(Version.LUCENE_46, edu.carleton.comp4601.assignment2.dao.Document.CONTENT, analyzer);
			Query q = parser.parse(searchString);
			TopDocs results = searcher.search(q, n);
			ScoreDoc[] hits = results.scoreDocs;
			reader.close();
			return getDocs(hits);
		}
		catch (IOException | ParseException e)
		{
			e.printStackTrace();
		}finally{
			if(dir != null)
				dir.close();
		}
		return
				null;
	}


	public ArrayList<edu.carleton.comp4601.assignment2.dao.Document> getDocs(ScoreDoc[] hits) throws IOException {
		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> docs = 
				new ArrayList<edu.carleton.comp4601.assignment2.dao.Document>();
		
		dir = FSDirectory.open(new File (INDEX_DIR));
    	IndexReader reader = DirectoryReader.open(dir);
    	IndexSearcher searcher = new IndexSearcher(reader);
		for (ScoreDoc hit :hits){
			org.apache.lucene.document.Document indexDoc = searcher.doc(hit.doc);
			String id = indexDoc.get(edu.carleton.comp4601.assignment2.dao.Document.DOC_ID);
			if (id != null) {
				edu.carleton.comp4601.assignment2.dao.Document d = DocumentsManager.getDefault().load(Integer.valueOf(id));
				if (d != null)
					docs.add(d);
			}
		}
		return docs;
	}
	



//    public boolean addDocument(Document d) {
//		try {
//			System.out.println("adding document to index:" + d.get(DOC_ID));
//	    	writer.addDocument(d);
//	    	writer.close();
//	    	return true;
//		} catch (IOException e) {
//			System.out.println("Error adding document to index:" + d.get(DOC_ID));
//			e.printStackTrace();
//			return false;
//		}
//	}
//
//    
//    public ScoreDoc[] search(String query, int thisHitsPerPage){
//    	Directory dir;
//		try {
//			dir = FSDirectory.open(new File(INDEX_DIR));
//	    	QueryParser parser = new QueryParser(Version.LUCENE_46, CONTENT, analyzer);
//	    	Query q = parser.parse(query);
//	    	IndexReader reader = DirectoryReader.open(dir);
//	    	IndexSearcher searcher = new IndexSearcher(reader);
//	    	TopDocs results = searcher.search(q, thisHitsPerPage);
//	    	System.out.println("Searching for: " + query.toString());
//	    	ScoreDoc[] hits = results.scoreDocs;
//	    	for(int i = 0; i < hits.length; i++){
//	    		int docId = hits[i].doc;
//	    		Document d = searcher.doc(docId);
//	    		System.out.println(d.get("doc_id"));
//	    	}
//			System.out.println(hits.length + " resuts found");
//			return hits;
//		} catch (IOException | ParseException e) {
//			e.printStackTrace();
//			return null;
//		}
//    	 
//    }
    
	public boolean reset() {
		try {
			dir = FSDirectory.open(new File(INDEX_DIR));
			writer = new IndexWriter(dir, iwc);
			writer.deleteAll();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (writer != null)
					writer.close();
				if (dir != null)
					dir.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		LuceneManager manager = LuceneManager.getDefault();
		
//		manager.index();
		
//		manager.reset();
		
		String search_query = "3004";
		int docToLoad = -1;
		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> results =  manager.query(search_query, 20);
		System.out.println("Search Lucene for : "+search_query + " size="+results.size());
		if(results != null){
			for(int i=0; i < results.size(); i++){
				if(i==0)
					docToLoad = results.get(i).getId();
				System.out.println("	> ("+results.get(i).getId()+") "+results.get(i).getUrl()+" [score="+results.get(i).getScore()+"]");
			}
		}

		if(docToLoad > -1){
			edu.carleton.comp4601.assignment2.dao.Document aDoc = DocumentsManager.getDefault().load(docToLoad);
			System.out.println("id :"+aDoc.getId() +" Date :"+aDoc.getCrawledDate());
			System.out.println("URL :"+aDoc.getUrl() +" Parent_URL :"+aDoc.getParent_url());
			System.out.println("metadate :"+aDoc.getMetadata().toString());

//			System.out.println("content :"+aDoc.getText());
		}
		
		List<edu.carleton.comp4601.assignment2.dao.Document> results2 = DocumentsManager.convertDBObject(DocumentsManager.getDefault().search("text", search_query, false));
		System.out.println("Search MongoDB [text] for : "+search_query + " size="+results2.size());

	}
    
}
