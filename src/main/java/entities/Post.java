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
	private User sender;
	private String body;
	private String url;
	private long like;

	public Post() {
	}

	public Post(User sender, String body, String url) {
		this.sender = sender;
		this.body = body;
		this.url = url;
		this.like = 0;
	}

	private Post(User sender, String body, String url, long like) {
		this.sender = sender;
		this.body = body;
		this.url = url;
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
						.append(post.getSender().getEmail());
		StringBuilder postIndexKey =
				new StringBuilder()
						.append(postKey)
						.append(":")
						.append(DatatypeConverter.printHexBinary(messageDigest.digest((new Date()).toString().getBytes())).toUpperCase());

		Entity newPost = new Entity("Post", postKey.toString());
		Entity newPostIndex = new Entity("PostIndex", postIndexKey.toString(), newPost.getKey());

		newPost.setProperty("sender", post.getSender().getEmail());
		newPost.setProperty("body", post.getBody());
		newPost.setProperty("url", post.getUrl());

		Query query = new Query("Follow")
				.setFilter(new Query.FilterPredicate("following", Query.FilterOperator.EQUAL, post.getSender().getEmail()));

		PreparedQuery preparedQuery = datastoreService.prepare(query);
		List<Entity> result = preparedQuery.asList(FetchOptions.Builder.withDefaults());

		List<String> keys = new ArrayList<>();
		result.forEach(entity -> keys.add(entity.getParent().getName()));

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

	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
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

	public long getLike() {
		return this.like;
	}

	private static Post entityToPost(Entity entity) {
		return new Post(new User((String) entity.getProperty("sender"), "", ""), (String) entity.getProperty("body"), (String) entity.getProperty("url"), (long) LikeCounter.countLike(entity.getKey()).getObject());
	}

	public static Post fetchUserAndFormat(Entity entity) throws EntityNotFoundException {
		Post post = entityToPost(entity);

		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		Entity user = datastoreService.get(KeyFactory.createKey("User", post.getSender().getEmail()));

		post.setSender(User.entityToUser(user));

		return post;
	}
}
