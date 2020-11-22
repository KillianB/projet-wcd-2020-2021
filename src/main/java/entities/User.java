package entities;

import com.google.appengine.api.datastore.Entity;

public class User {
	private String email;
	private String name;
	private String urlAvatar;

	public User() {
	}

	public User(String email, String name, String urlAvatar) {
		this.email = email;
		this.name = name;
		this.urlAvatar = urlAvatar;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrlAvatar() {
		return urlAvatar;
	}

	public void setUrlAvatar(String urlAvatar) {
		this.urlAvatar = urlAvatar;
	}

	public static User entityToUser(Entity entity) {
		return new User((String) entity.getProperty("email"), (String) entity.getProperty("name"), (String) entity.getProperty("urlAvatar"));
	}
}
