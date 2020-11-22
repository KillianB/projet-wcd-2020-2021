package entities;

import com.google.appengine.api.datastore.*;
import jakarta.xml.bind.DatatypeConverter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class Post {
	private String sender;
	private String body;
	private String url;
	private Key key;
	private long like;

	public Post() {
	}

	public Post(String sender, String body, String url) {
		this.sender = sender;
		this.body = body;
		this.url = url;
		this.key = null;
		this.like = 0;
	}

	private Post(String sender, String body, String url, Key key, long like) {
		this.sender = sender;
		this.body = body;
		this.url = url;
		this.key = key;
		this.like = like;
	}

	public static Entity postMessage(Post post) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		StringBuilder postKey =
				new StringBuilder()
						.append(Long.MAX_VALUE - (new Date()).getTime())
						.append(":")
						.append(post.getSender());
		StringBuilder postIndexKey =
				new StringBuilder()
						.append(postKey)
						.append(":")
						.append(DatatypeConverter.printHexBinary(messageDigest.digest((new Date()).toString().getBytes())).toUpperCase());

		Entity newPost = new Entity("Post", postKey.toString());
		Entity newPostIndex = new Entity("PostIndex", postIndexKey.toString(), newPost.getKey());

		newPost.setProperty("sender", post.getSender());
		newPost.setProperty("body", post.getBody());
		newPost.setProperty("url", post.getUrl());

		Query query = new Query("Follow")
				.setFilter(new Query.FilterPredicate("following", Query.FilterOperator.EQUAL, post.getSender()));

		PreparedQuery preparedQuery = datastoreService.prepare(query);
		List<Entity> result = preparedQuery.asList(FetchOptions.Builder.withDefaults());

		List<String> keys = new ArrayList<>();
		result.forEach(entity -> keys.add(entity.getKey().getName()));

		HashSet<String> followers = new HashSet<>(keys);

		newPostIndex.setProperty("receivers", followers);

		Transaction transaction = datastoreService.beginTransaction(TransactionOptions.Builder.withXG(true));

		datastoreService.put(newPost);
		datastoreService.put(newPostIndex);

		for (int i = 0; i < 10; i++) {
			datastoreService.put(LikeCounter.generateLike(postKey.toString(), i));
		}

		transaction.commit();

		return newPost;
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

	public Key getKey() {
		return this.key;
	}

	public long getLike() {
		return this.like;
	}
	public static Post entityToPost(Entity i) {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();

		return new Post((String)i.getProperty("sender"), (String)i.getProperty("body"), (String)i.getProperty("url"), i.getKey(), (long)LikeCounter.countLike(i.getKey().toString()).getObject());
	}
}
