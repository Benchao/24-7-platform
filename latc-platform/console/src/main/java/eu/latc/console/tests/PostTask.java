package eu.latc.console.tests;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.PostMethod;

import eu.latc.console.resources.APIKey;

public class PostTask {
	// Where the application is deployed
	// final static String HOST = "http://latc-console.few.vu.nl/";
	final static String HOST = "http://127.0.0.1:58080/LATC-console";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Prepare the message
		// "specification" is mandatory
		// "title" is mandatory
		// "author" is optional
		// "description" is optional
		NameValuePair[] request = { new NameValuePair("specification", "<xml></xml>"),
				new NameValuePair("title", "test"), new NameValuePair("api_key", APIKey.KEY),
				new NameValuePair("author", "Admin"), new NameValuePair("description", "A test"), };

		// Diplay the result
		System.out.println("Message to send -> " + request.toString());

		// Prepare the query
		String URI = HOST + "/api/tasks";
		System.out.println(URI);
		PostMethod post = new PostMethod();
		post.setURI(new URI(URI, false));
		post.setRequestBody(request);

		// Issue the POST
		HttpClient clientService = new HttpClient();
		int status = clientService.executeMethod(post);

		// Check response code
		if (status != HttpStatus.SC_CREATED) {
			throw new Exception("Received error status " + status);
		}
		System.out.println("response -> " + post.getResponseBodyAsString());
	}
}
