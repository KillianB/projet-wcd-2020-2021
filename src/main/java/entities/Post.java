package entities;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

public class Post {
	private String sender;
	private String body;
	private String url;

	public Post(String sender, String body, String url) {
		this.sender = sender;
		this.body = body;
		this.url = url;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public static Post entityToPost(Entity i) {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();

		Post post = new Post((String)i.getProperty("sender"), (String)i.getProperty("body"), (String)i.getProperty("url"));

		return post;
	}
}
