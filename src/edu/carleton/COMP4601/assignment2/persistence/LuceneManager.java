package edu.carleton.comp4601.assignment2.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
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

import edu.carleton.comp4601.assignment2.crawler.Controller;



//import edu.carleton.comp4601.assignment2.dao.Document;

public class LuceneManager {

	public static String INDEX_DIR = "/Users/abdulrahmanalamoudi/Desktop/temp/index";
	
	int hitsPerPage = 10;
	
	private final static String PATH = "path";
	private final static String DOC_ID = "doc_id";
	private final static String MODIFIED = "modified";
	private final static String CONTENTS = "contents";

	private FSDirectory dir;
	private StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
	private IndexSearcher searcher;
	private IndexWriter writer = null;

	private static LuceneManager manager;

	public static LuceneManager getDefault() {
		if (manager == null) {
			manager = new LuceneManager();
		}
		return manager;
	}
	
	private LuceneManager() {
		super();

		try {
    		File docDir = new File (Controller.CRAWL_DIR);
			dir = FSDirectory.open(new File (INDEX_DIR));
    		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
    		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);
    		writer = new IndexWriter(dir, iwc);
    		indexDocuments(writer, docDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try{
				if(writer != null){
					writer.close();
				}
				if(dir != null)
					dir.close();
			}catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	private void indexDocuments(IndexWriter writer, File file) {
		if(file.canRead()){
			if(file.isDirectory()){
				String[] files = file.list();
				if(files != null){
					for (String name : files){
						indexDocuments(writer, new File (file, name));
					}
				}
			}
			else {
				FileInputStream fis;
				try{
					fis = new FileInputStream(file);
					indexAFile(file, fis);
					fis.close();
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		}
	}


	private void indexAFile(File file, FileInputStream fis) throws IOException{
		org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
		Field pathField	= new StringField(PATH, file.getPath(), Field.Store.YES);
		doc.add(pathField);
		try
		{	
			int docId = Integer.valueOf(file.getName().replaceFirst("[.][^.]+$", ""));
			doc.add(new IntField(DOC_ID, docId, Field.Store.YES));
		} catch (NumberFormatException e)
		{
		}
		doc.add(new LongField(MODIFIED, file.lastModified(), Field.Store.NO));
		doc.add(new TextField(CONTENTS, new BufferedReader( new InputStreamReader(fis, "UTF-8"))));
		writer.addDocument(doc);
	}
    
	public ArrayList<edu.carleton.comp4601.assignment2.dao.Document>	query(String searchString) {
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(INDEX_DIR)));
			searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			QueryParser parser = new QueryParser(Version.LUCENE_46, CONTENTS, analyzer);
			Query q = parser.parse(searchString);
			TopDocs results = searcher.search(q, 1000);
			ScoreDoc[] hits = results.scoreDocs;
			reader.close();
			return getDocs(hits);
		}
		catch (IOException | ParseException e)
		{
			e.printStackTrace();
		}
		return
		null;
	}


	private ArrayList<edu.carleton.comp4601.assignment2.dao.Document> getDocs(ScoreDoc[] hits) throws IOException {
		ArrayList<edu.carleton.comp4601.assignment2.dao.Document> docs = 
				new ArrayList<edu.carleton.comp4601.assignment2.dao.Document>();
		
		for (ScoreDoc hit :hits){
			org.apache.lucene.document.Document indexDoc = searcher.doc(hit.doc);
			String id = indexDoc.get(DOC_ID);
			if (id != null) {
				edu.carleton.comp4601.assignment2.dao.Document d = DocumentsManager.getDefault().load(Integer.valueOf(id));
				if (d != null)
					docs.add(d);
			}
		}
		return docs;
	}
	

    public boolean indexDocument(String url, int i, Date date, String content, String metadata){
		Document doc = new Document();
		doc.add(new TextField("url", url, Field.Store.YES));
		doc.add(new IntField(DOC_ID, i, Field.Store.YES));
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
			dir = FSDirectory.open(new File(INDEX_DIR));
	    	QueryParser parser = new QueryParser(Version.LUCENE_46, CONTENTS, analyzer);
	    	Query q = parser.parse(query);
	    	IndexReader reader = DirectoryReader.open(dir);
	    	searcher = new IndexSearcher(reader);
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
