package edu.carleton.comp4601.assignment2.utility;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.ws.rs.core.MediaType;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import edu.carleton.comp4601.assignment2.dao.DocumentCollection;
import edu.carleton.comp4601.assignment2.resources.SearchableDocumentArchive;

/* Assumption is that this works as a singleton.
 * We register the service when we create it.
 * Relies on certain strings being defined.
 * Expectation is that only 1 search service will run on 
 * each machine.
 */

public class SearchServiceManager implements Runnable, ServiceListener {

	private static SearchServiceManager instance;
	private static String COMP4601SDA = "COMP4601SDA";
	private static int PORT = 8080;
	private JmDNS jmdns;
	private Thread thread;
	private static String SEARCH = "_sda._tcp.local.";
	private ConcurrentHashMap<String, ServiceInfo> services;
	private Logger logger;

	public SearchServiceManager() {
		logger = Logger.getGlobal();
		services = new ConcurrentHashMap<String, ServiceInfo>();
	}

	public void init() throws IOException {
		InetAddress addr = InetAddress.getLocalHost();
		String host = addr.getHostName();
		jmdns = JmDNS.create(addr, host);
		jmdns.addServiceListener(SEARCH, this);
		ServiceInfo[] infos = jmdns.list(SEARCH);
		System.out.println("LIST: " + SEARCH);
		for (ServiceInfo s : infos) {
			System.out.println(s);
		}
		
		// Add this hook in order to ensure that
		// jmDNS stops properly
		thread = new Thread(this);
		Runtime.getRuntime().addShutdownHook(thread);
	}

	public void run() {
		try {
			stop();
		} catch (IOException e) {
		}
	}
	
	public void stop() throws IOException {
		logger.log(Level.INFO, "Stopping distributed search services...");
		jmdns.unregisterAllServices();
		jmdns.close();
	}

	public void register(int port, String name)
			throws IOException {
		ServiceInfo s = ServiceInfo.create(SEARCH, name+hashCode(), port, "");
		jmdns.registerService(s);
	}

	public void unregister(int port, String name) {
		ServiceInfo s = ServiceInfo.create(SEARCH, name+hashCode(), port, "");
		jmdns.unregisterService(s);
	}

	public static SearchServiceManager getInstance() {
		if (instance == null) {
			instance = new SearchServiceManager();
			try {
				instance.init();
				/*
				 * Uses locally defined values
				 * You may wish to change this!
				 */
				instance.register(PORT, COMP4601SDA);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	// This is the distributed interface
	// Send off the query to all
	public SearchResult search(String tags) {
		SearchResult sr = new SearchResult(services.values().size());
		for (ServiceInfo s : services.values()) {
			new AsyncSearch(sr, s, tags, false).start();
		}
		return sr;
	}
	
	// This is the distributed interface
	// Send off the query to all
	public SearchResult query(String tags) {
		String a = "";
		try {
			a = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			a = "unknown";
		}
		SearchResult sr = new SearchResult(services.values().size());
		for (ServiceInfo s : services.values()) {
			if (!a.equals(s.getHostAddresses()[0])) {
				new AsyncSearch(sr, s, tags, true).start();
			}
		}
		return sr;
	}
	
	

	private String getService(ServiceInfo s) {
		return "http://" + s.getHostAddresses()[0] + ":" + s.getPort() + "/"
				+ COMP4601SDA + "/";
	}

	// ServiceListener Interface
	// Assumes that only one service per machine
	public void serviceAdded(ServiceEvent s) {
		// services.put(getService(s.getInfo()), s.getInfo());
		 System.out.println("Added: " + s.getName());
	}

	public void serviceRemoved(ServiceEvent s) {
		services.remove(getService(s.getInfo()));
		 System.out.println("Removed: " + s.getName());
	}

	public void serviceResolved(ServiceEvent s) {
		services.put(getService(s.getInfo()), s.getInfo());
		System.out.println("Resolved: " + s.getName());
	}
	
	public Collection<ServiceInfo> list() {
		return services.values();
	}

	// This distributes the search.
	// We don't wait for each task to finish
	private class AsyncSearch extends Thread {
		SearchResult sr;
		ServiceInfo s;
		String tags;
		boolean isQuery;

		AsyncSearch(SearchResult sr, ServiceInfo s, String tags, boolean isQuery) {
			super();
			this.sr = sr;
			this.s = s;
			this.tags = tags;
			this.isQuery = isQuery;
		}

		public void run() {
			// We handle every exception here in order to 
			// ensure that we decrement the latch whatever
			// happens to the distributed search.
			try {
				logger.log(Level.INFO, "Searching: "+getService(s));
				Client client = Client.create(new DefaultClientConfig());
				WebResource service = client.resource(getService(s));
				String serviceType;
				if (isQuery) 
					serviceType = SearchableDocumentArchive.QUERY;
				else
					serviceType = SearchableDocumentArchive.SEARCH;
				
				ClientResponse r = service.path(SearchableDocumentArchive.REST)
						.path(SearchableDocumentArchive.SDA)
						.path(serviceType).path(tags)
						.accept(MediaType.APPLICATION_XML)
						.get(ClientResponse.class);
				// Check to make sure that we got a reasonable response
				if (r.getStatus() < 204) {
					sr.addAll(r.getEntity(DocumentCollection.class)
							.getDocuments());
				}
			} catch (Exception e) {
				System.out.println("Error in search: " + e);
			} finally {
				sr.countDown();
			}
		}
	}
}
