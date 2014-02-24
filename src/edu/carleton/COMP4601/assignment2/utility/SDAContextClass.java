package edu.carleton.comp4601.assignment2.utility;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class SDAContextClass implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			SearchServiceManager.getInstance().stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void contextInitialized(ServletContextEvent arg0) {
		SearchServiceManager.getInstance();
	}

}
