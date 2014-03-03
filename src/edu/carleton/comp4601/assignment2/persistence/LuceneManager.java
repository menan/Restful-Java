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


public class LuceneManager {

	private static String INDEX_DIR = "/Users/abdulrahmanalamoudi/Desktop/temp/index";

	int hitsPerPage = 10;
	

	private FSDirectory dir;
	private StandardAnalyzer analyzer;
	private IndexWriter writer;
	private static LuceneManager manager;
	private String indixing_dir;
	
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
	private LuceneManager() {
		initLucene(INDEX_DIR);
	}
	
	private LuceneManager(String indixing_dir) {
		initLucene(indixing_dir);
	}
	
	private void initLucene(String indixing_dir){
		this.indixing_dir = indixing_dir;
		analyzer = new StandardAnalyzer(Version.LUCENE_46);
	}
	
	
	/**
	 * Returns default instance.
	 * 
	 * @return
	 */
	public static LuceneManager getDefault() {
		
		if (manager == null) {
			manager = new LuceneManager();
		}
		return manager;
	}
	
	public synchronized void index() {
		IndexWriter writer = null;
		FSDirectory dir = null;
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);

		try {
			dir = FSDirectory.open(new File(this.indixing_dir));
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
	    	if(doc != null){
		    	Document luceneDoc = edu.carleton.comp4601.assignment2.dao.Document.getLuceneDocFromDocument(doc);
				writer.addDocument(luceneDoc);
	    	}
		}
	}

	
	public ArrayList<edu.carleton.comp4601.assignment2.dao.Document> query(String searchString, int n) {
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(this.indixing_dir)));
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
		
		dir = FSDirectory.open(new File (this.indixing_dir));
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

	
	public boolean reset() {
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);

		try {
			dir = FSDirectory.open(new File(this.indixing_dir));
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
		manager.reset();

		edu.carleton.comp4601.assignment2.dao.Document eclipse_doc = new edu.carleton.comp4601.assignment2.dao.Document(new Integer (999999));
		eclipse_doc.setScore(1);
		eclipse_doc.setUrl("http://someurl.edu");
		eclipse_doc.setText("Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse "+
				"Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse "+
				"Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse Eclipse ");
		DocumentsManager.getDefault().save(eclipse_doc);
		List<edu.carleton.comp4601.assignment2.dao.Document> searchedDocs = DocumentsManager.convertDBObject(DocumentsManager.getDefault().search("text", "Eclipse", false));
		for(edu.carleton.comp4601.assignment2.dao.Document d : searchedDocs){
			if(d.getId().intValue() == 999999)
				System.out.println("eclipse_doc was added");
		}

		
		manager.index();
		
		String search_query = "Eclipse";
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

		System.out.println("	--- Stored Graphs ---	");
		for(int i = 1; i < 10; i++){	// I know before hand that there are 9 graphs stored already in the DB
			System.out.println("Graph ("+i+"):\n	>"+ GraphManager.getDefault().loadGraph(i));
		}
		
	}
    
}
