package entities;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

public class Post {
	private String sender;
	private String body;
	private String url;
	private Key key;

	public Post() {
	}

	public Post(String sender, String body, String url) {
		this.sender = sender;
		this.body = body;
		this.url = url;
		this.key = null;
	}

	private Post(String sender, String body, String url, Key key) {
		this.sender = sender;
		this.body = body;
		this.url = url;
		this.key = key;
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

		return new Post((String)i.getProperty("sender"), (String)i.getProperty("body"), (String)i.getProperty("url"), i.getKey());
	}
}
