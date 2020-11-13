package entities;

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
}
