package edu.carleton.COMP4601.assignment1.Main;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;



import edu.carleton.COMP4601.assignment1.*;
import edu.carleton.COMP4601.assignment1.persistence.DocumentsManager;

public class Action {
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	String id;
	
	DocumentCollection collection;

	/**
	 * constructor for Action class
	 * @param uriInfo
	 * @param request
	 * @param id
	 * @param coll
	 */
	
	public Action(UriInfo uriInfo, Request request, String id) {
		this.uriInfo = uriInfo;
		this.request = request;
		this.id = id;
	}

	/**
	 * constructor for Action class, collection will be reused from SDA class
	 * @param uriInfo
	 * @param request
	 * @param id
	 * @param coll
	 */
	public Action(UriInfo uriInfo, Request request, String id, DocumentCollection coll) {
		this.uriInfo = uriInfo;
		this.request = request;
		this.id = id;
		this.collection = coll;
	}

	/**
	 * Prints out the XML version of the documet
	 * @return
	 * @throws UnknownHostException
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Document getDocumentXML() throws UnknownHostException {
		Document a = DocumentsManager.getDefault().load(new Integer(id));
		if (a == null) {
			System.out.println("No such Document: " + id);
		}
		else{
			System.out.println("XML - Getting document with id:" + a.getId() + " name:" + a.getName());
		}
		return a;
	}
	/**
	 * prints out the HTML version of each document
	 * @return
	 * @throws UnknownHostException
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getDocumentHTML() throws UnknownHostException {
		if (id == null || id.isEmpty()){
			return "No such Document: " + id;
		}
		Document a;
		try{
			a = DocumentsManager.getDefault().load(new Integer(id));
		}catch(java.lang.NumberFormatException e){
			return "No such Document: " + id;
		}
		if (a == null) {
			return "No such Document: " + id;
		}
		else{
			System.out.println("HTML - Getting document with id:" + a.getId() + " name:" + a.getName());
		}
		
		StringBuilder builder = new StringBuilder();
		// Name
		builder.append("Name: <b>" + a.getName() + "</b>");
		// ID
		builder.append("<br />ID: <b>" + a.getId() + "</b>");
		// Text
		builder.append("<br />Text: <b>" + a.getText() + "</b>");
		// Tags
		if(a.getTags() != null && a.getTags().size() > 0){
			builder.append("<br /><br />Tags: ");
			for(String tag: a.getTags()){
				builder.append("<b>" + tag + "</b>, ");
			}
		}
		// Links
		if(a.getLinks() != null && a.getLinks().size() > 0){
			builder.append("<br /><br />Links:<b><br />");
			for(String link: a.getLinks()){
				builder.append("<a href=\"" + link +"\">" +link  +"</a><br />");
			}
		}
			
		
		
		return builder.toString();
	}
	
	/**
	 * Modifies the current document with the parameters given.
	 * Will return response code 200 if it get modified successfully
	 * will return response code 204 if there is an issue
	 * @param name
	 * @param tags
	 * @param links
	 * @param text
	 * @param servletResponse
	 * @return
	 * @throws NumberFormatException
	 * @throws FileNotFoundException
	 * @throws UnknownHostException
	 * @throws JAXBException
	 */

	@PUT
	public Response putDocument(@FormParam("name") String name,
			@FormParam("tags") String tags,
			@FormParam("links") String links,
			@FormParam("text") String text,
			@Context HttpServletResponse servletResponse) throws NumberFormatException, FileNotFoundException, UnknownHostException, JAXBException {
		
		if(!this.collection.update(new Integer(id).intValue(), name, tags, links, text)){
			return Response.status(HttpServletResponse.SC_NO_CONTENT).build();
		}
		System.out.println("Update was succussful");
		return Response.status(HttpServletResponse.SC_OK).build();
	}

	/**
	 * Deletes a document from the database with the id parameter
	 * @return
	 */
	@DELETE
	public Response deleteDocument() {
		if (DocumentsManager.getDefault().remove(new Integer(id))){
			return Response.status(HttpServletResponse.SC_OK).build();
		}else{
			System.out.println("Action - deleteDocument(): something went wrong deletion didn't go through");
			return Response.status(HttpServletResponse.SC_NO_CONTENT).build();
		}
	}

}
