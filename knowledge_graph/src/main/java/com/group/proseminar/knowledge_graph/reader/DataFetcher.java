package com.group.proseminar.knowledge_graph.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An object from this class is created by {@link Controller}.
 * It connects to dbpedia and fetches all articles that are classified under the given ontology.
 * For each fetched article an {@link ArticleHolder} is generated, registered in the pool at {@link Controller}, and started to run.
 * 
 * @author Sibar Soumi
 *
 */

class DataFetcher extends Thread {
	private String url;
	private Controller controller;
	private Semaphore pool_limiter ;
	
	DataFetcher(String ontology, Controller controller, Semaphore pool_limiter) {
		this.controller = controller;
		this.pool_limiter=pool_limiter;
		url = "http://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=SELECT+*+%7B%3Fs+a+dbo%3A"
				+ ontology
				+ ".+%3Fs+dbo%3Aabstract+%3Fabs.+FILTER%28lang%28%3Fabs%29%3D%22en%22%29%7D&format=application%2Fsparql-results%2Bjson&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on&run=+Run+Query+";
	}

	@Override
	public void run() {
		JSONObject json;
		try {
			
			json = readJsonFromUrl(url);		
			for (Object i : json.getJSONObject("results").getJSONArray("bindings")) {
				String uri = ((JSONObject) i).getJSONObject("s").getString("value");
				String textField = ((JSONObject) i).getJSONObject("abs").getString("value");
				try {
					pool_limiter.acquire();
					ArticleHolder ah = new ArticleHolder(textField, uri, controller);
					ah.start();
					controller.addHolderToThePool(ah);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

}
