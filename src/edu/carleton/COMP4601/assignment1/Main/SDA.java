package edu.carleton.COMP4601.assignment1.Main;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import edu.carleton.COMP4601.assignment1.*;
import edu.carleton.COMP4601.assignment1.persistence.DocumentsManager;

@Path("/sda")
public class SDA {

	// Allows to insert contextual objects into the class,
	// e.g. ServletContext, Request, Response, UriInfo
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	private String name;
	private DocumentCollection collection;

	/**
	 * constructor for SDA class
	 * defines the home page content
	 */
	public SDA() {
		name = "COMP4601 Searchable Document Archive";
		collection = new DocumentCollection();
	}
	/**
	 * returns the name of the project as a string
	 * @return
	 */
	@GET
	public String printName() {
		return name;
	}
	
	/**
	 * returns xml version of the home page
	 * @return
	 */

	@GET
	@Produces(MediaType.APPLICATION_XML)
	public String sayXML() {
		return "<?xml version=\"1.0\"?>" + "<sda> " + name + " </sda>";
	}

	/**
	 * returns the HTML version of the home page
	 * @return
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtml() {
		return "<html> " + "<title>" + name + "</title>" + "<body><h1>" + name
				+ "</body></h1>" + "</html> ";
	}

	/**
	 * returns out the documents as a list of arrays to be printed out as an XML
	 * @return
	 * @throws UnknownHostException
	 */
	@GET
	@Path("documents")
	@Produces(MediaType.APPLICATION_XML)
	public List<Document> getDocumentsXML() throws UnknownHostException {
		return collection.getDocuments();
	}
	
	/**
	 * displays the documents in a neat format
	 * @return
	 * @throws UnknownHostException
	 */
	
	@GET
	@Path("documents")
	@Produces(MediaType.TEXT_HTML)
	public String getDocumentsHTML() throws UnknownHostException {
		List<Document> resultsDoc = collection.getDocuments();
		if (resultsDoc.size() == 0){
			return "No Documents found :( <br /><a href=\"../../create_document.html\">Click Here</a> to add a document";
		}
		String returnStr = "<h2>Documents</h2>There are " + resultsDoc.size() + " documents<br /><br /><table>";
		
		for(Document d: resultsDoc){
			returnStr += d.toHTML();
		}
		returnStr += "</table><br /><br /><a href=\"../../create_document.html\">New Document</a>";
		
		return returnStr;
	}
	
	/**
	 * search function for sda
	 * tags parameter would be used as a keyword to search
	 * returns a list of documents so it can be displayed as an XML
	 * @param tags
	 * @return
	 * @throws UnknownHostException
	 */

	@GET
	@Path("search/{tags}")
	@Produces(MediaType.APPLICATION_XML)
	public List<Document> searchDocuments(@PathParam("tags") String tags) throws UnknownHostException {
		List<Document> resultsDoc = new ArrayList<Document>();
		resultsDoc = collection.search(tags);
		return resultsDoc;
	}
	
	/**
	 * html version of the search function
	 * if there are no queries or no results found in the database, those errors would be printed out
	 * @param tags
	 * @return
	 * @throws UnknownHostException
	 */
	
	@GET
	@Path("search/{tags}")
	@Produces(MediaType.TEXT_HTML)
	public String searchDocumentsHTML(@PathParam("tags") String tags) throws UnknownHostException {
		if (tags == null || tags.isEmpty()){
			System.out.println("No query entered");
			return "Please enter a query to search";
		}

		List<Document> resultsDoc = collection.search(tags);
		String returnStr = "Your search returned " + resultsDoc.size() + " results<table>";
		
		for(Document d: resultsDoc){
			returnStr += d.toHTML();
		}
		returnStr += "</table>";
		return returnStr;
	}

	
	/**
	 * deletes documents that has posted tags parameters and returns the responses accordingly 
	 * @param tags
	 * @return
	 * @throws UnknownHostException
	 */
	@GET
	@Path("delete/{tags}")
	public Response deleteDocuments(@PathParam("tags") String tags) throws UnknownHostException {
		if(!DocumentsManager.getDefault().deleteAll("tags", tags)){
			return Response.status(HttpServletResponse.SC_NO_CONTENT).build();
		}
		System.out.println("Delete was succussful");
		return Response.status(HttpServletResponse.SC_OK).build();
	}
	
	/**
	 * creates a new document and displays the error messages accordingly
	 * if there is already a document found with the same id, it will tell them to alter it
	 * @param id
	 * @param name
	 * @param tags
	 * @param links
	 * @param text
	 * @param servletResponse
	 * @return
	 * @throws IOException
	 */
	
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String newDocumentHTML(@FormParam("id") String id,
			@FormParam("name") String name,
			@FormParam("tags") String tags,
			@FormParam("links") String links,
			@FormParam("text") String text,
			@Context HttpServletResponse servletResponse) throws IOException {

		if (name == null || name.isEmpty() || id == null || id.isEmpty() || tags == null || tags.isEmpty()){
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return "You have to fill all the fields. Please go <a href=\"javascript:history.back()\">back</a> and fill it.";
		}
		else{

			int newId = new Integer(id).intValue();
			Document doc = DocumentsManager.getDefault().load(newId);
			if (doc == null){
				boolean put = DocumentsManager.getDefault().create(newId,name,tags,links,text);
				if (put){
					servletResponse.setStatus(HttpServletResponse.SC_OK);
					return "Created successfully. <a href=\"/COMP4601SDA/rest/sda/documents\">Click Here</a> to view all documents.";
				}
				else{
					servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
					return "Error creating document.";
				}
			}
			else{

				servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return "Sorry, but there is already a record exists with id " + newId + ". Please go <a href=\"javascript:history.back()\">back</a> and change it.";
			}
		}
			

	}

	/**
	 * XML version of the create document function.
	 * returns the right responses based on the actions accordingly
	 * @param id
	 * @param name
	 * @param tags
	 * @param links
	 * @param text
	 * @param servletResponse
	 * @return
	 * @throws IOException
	 */

	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Document newDocumentXML(@FormParam("id") String id,
			@FormParam("name") String name,
			@FormParam("tags") String tags,
			@FormParam("links") String links,
			@FormParam("text") String text,
			@Context HttpServletResponse servletResponse) throws IOException {

		if (name == null || name.isEmpty() || id == null || id.isEmpty() || tags == null || tags.isEmpty()){
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return null;
		}
		else{

			int newId = new Integer(id).intValue();
			Document doc = DocumentsManager.getDefault().load(newId);
			if (doc == null){
				boolean put = DocumentsManager.getDefault().create(newId,name,tags,links,text);
				if (put){
					servletResponse.setStatus(HttpServletResponse.SC_OK);
					return DocumentsManager.getDefault().load(newId);
				}
				else{
					servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
					return null;
				}
			}
			else{
				servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return null;
			}
		}
		
	}

	/**
	 * initializes a document with the parameter id in the url to the Action class for latter usage
	 * @param id
	 * @return
	 */
	@Path("{doc}")
	public Action getDocument(@PathParam("doc") String id) {
		return new Action(uriInfo, request, id, collection);
	}

}
