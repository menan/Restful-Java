COMP4601A2
==========


- When running, directing a browser to http://localhost:8080/COMP4601SDA/rest/sda should display the text "COMP4601 Searchable Document Archive V2: X" in a browser window. Here, X is the name of your group; e.g., "Bob White and Alice Green".

- When started, the SDA uses JmDNS to discover other search web services (SWS) running on the local area network (LAN).

- When started, the SDA registers its search web service using JmDNS on the LAN.
- When terminated, the SDA deregisters its search web service using JmDNS.
- The SearchServiceManager, SDAContextClass and SearchResult classes implement the distributed functionality.
- You must augment your web.xml document to use the <listener-class> as shown or you will have problems with leaking resources in Tomcat.
- You should also provide a general_error.html document that handles HTTP 500 response codes.
- Assuming that you have understood the function of the SearchServiceManager requirements 2,3 and 4 will not require software development by you.
- The SDA stores documents which are both tagged and linked together. Document and DocumentCollection classes are provided and MUST be used. This is required in order to ensure that the search engines interoperate. So, a database document contains:
    - name: This is a user-friendly display string for the document.
    - id: This is a unique number for a document.
    - score: This is the score computed by Lucene.
    - text: This is a string that contains the content of the document. It is not structured in any way (for now).
    - tags: This is an array of strings representing the searchable content for the document. A document must be tagged with at least 1 keyword/phrase.
    links: This is an array of URLs representing links to other documents. You may assume that these links are relative to the URL of the web service itself. A document may have no links.
- You must demonstrate that you can crawl the following URLs:
    - http://www.carleton.ca -- you should only index carleton web pages; i.e., prevent off site page visits. Ensure that you limit the number of pages indexed!
    - http://sikaman.dyndns.org:8888/courses/ -- you should only index pages on this web server; i.e., do not follow links that are not on the server.
    - A URL of your choosing.
- When crawling, a directed graph of the pages visited must be constructed. All graphs should be given a root node, 0, when the crawl begins. Any pages with a null parentURL should be connected to node 0.
- At the end of the crawl, a single graph must be stored as a database document in serialized format. Using the above, the graph should have a path from node 0 to every other node.
- When crawling, you must demonstrate that your crawl is adaptive; i.e., changes the frequency with which it visits pages depending of the time taken to crawl the last several pages.
- When crawling, along with HTML documents, you must index binary data; i.e., images (jpeg, tiff, gif, png), pdf, doc, docx, xls, xlsx, ppt and pptx documents.
- When crawling, image documents referenced within <img> tags with an alt attribute must have those images indexed with a field that includes the tag text.
- When crawling, the docId determined by the crawler must be used as the document id of the content stored in the database.
- You must demonstrate that you can parse the crawled pages using Tika. You must extract all metadata and use the AutoDetectParser to generate content.
- You must index all of the pages stored in your database using Lucene using the StandardAnalyzer.
- Using the graph from (9), compute the Page Rank for all crawled pages.
- Your Lucene documents must contain fields for:
    - URL -- the location of the original document.
    - DocID -- the id of the document stored within your database.
    - Date -- meaning the date and time when the document was crawled.
    - Content -- this is the content returned by the content handler used in the Standard Analyzer. For images, this may not contain much.
    - Metadata fields -- a field should be created for each piece of meta data; e.g., for a file with a MIME type of     image/jpeg the field name would be type and the value would be image/jpeg.
- In the text below, the web service is indicated as: sda/X, where X is the web service. You must provide the following - - RESTful web services:
  - Reset the document archive. sda/reset using GET. This is a testing convenience only. HTML should be returned stating that the reset has occurred (or an error).
  - List the discovered search services. sda/list using GET. This is a testing convenience only. HTML should be returned providing a list of links to the services found (resolved).
  - Show the Page Rank scores for all documents. sda/pagerank using GET. This is designed to test the analysis of the graph obtained from the crawl. A 2 column table of all documents should be returned with the document name and page rank information in the first and second column respectively.
  - Boost document relevance using Page Rank scores. sda/boost using GET. This will re-index the database and apply a boost value to each document that is equal to the Page Rank score of the document.
No boost. sda/noboost using GET. This will re-index the database giving a boost value of 1 to all documents in the database.
  - When creating your own documents, the created documents must be analyzed and indexed using Lucene. All tags must be stored in a separate searchable field. This field is given a default boost value of 2 (you may provide a field to specify this value in a form).
  - When a document is updated, the index must also be updated; i.e., the old Lucene document must be deleted and a new Lucene document inserted.
  - Query for documents with specific terms. sda/query/{terms} using GET.
  - Terms must conform to the format: +term1+term2+ ... meaning: retrieve ranked documents with term1 term2 ... Terms may also include the field names (see notes for format here).
  - Your sda/query/{terms} using GET web service must produce 3 forms of ouput. The method that produces APPLICATION_XML must not invoke SearchServiceManager.query(String tags) as this will generate a mutually recursive search. This form must return a DocumentCollection<Document>. The TEXT_HTML and TEXT_XML forms of the web service must invoke the     SearchServiceManager.query(String tags) method in order to distribute search.
  - The documents found should be returned as a list. You must support XML and HTML representations. The XML must use the representation derived from the Document and DocumentCollection classes. You will need to use the SearchResult class in order to coordinate and aggregate content retrieved during the distributed search.
When searching, all known registered search web services must be queried. You may wait up to 10 seconds for other SWS to return content.
  - Clicking on a queried document displays its contents. You must support XML and HTML representations.
  - If no documents are found, "No documents found." should be displayed. This will be returned in either plain text or HTML representations.
- When the results of a query are displayed, it should be possible to navigate to the linked documents.
