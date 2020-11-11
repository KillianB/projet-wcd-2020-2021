package business;


import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.api.datastore.*;
import entities.Post;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Api(name = "tinyApi",
		version = "v1",
		audiences = "1048463456874-56t3t922794hiac34phbfntkdqmt0lhl.apps.googleusercontent.com",
		clientIds = "1048463456874-56t3t922794hiac34phbfntkdqmt0lhl.apps.googleusercontent.com",
		namespace =
		@ApiNamespace(
				ownerDomain = "https://tinyinsta-295118.ew.r.appspot.com",
				ownerName = "https://tinyinsta-295118.ew.r.appspot.com",
				packagePath = "")
)

public class Endpoint {
	@ApiMethod(name = "timeline", httpMethod = HttpMethod.GET)
	public List<Post> getTimeline() {
		List<Post> posts = new ArrayList<>();

		return posts;
	}

	@ApiMethod(name = "postMessage", httpMethod = HttpMethod.POST)
	public Entity postMessage(Post post) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		StringBuilder postKey =
				new StringBuilder()
						.append(post.getSender())
						.append(":")
						.append(Long.MAX_VALUE-(new Date()).getTime());
		StringBuilder postIndexKey =
				new StringBuilder()
						.append(postKey)
						.append(":")
						.append(Arrays.toString(messageDigest.digest((new Date()).toString().getBytes())));

		Entity newPost = new Entity("Post", postKey.toString());
		Entity newPostIndex = new Entity("PostIndex", postIndexKey.toString());

		newPost.setProperty("sender", post.getSender());
		newPost.setProperty("body", post.getBody());
		newPost.setProperty("url", post.getUrl());

		Query query = new Query("Follow")
				.setFilter(new Query.FilterPredicate("__key__", Query.FilterOperator.EQUAL, post.getSender()));

		PreparedQuery preparedQuery = datastoreService.prepare(query);
		Entity result = preparedQuery.asSingleEntity();

		HashSet<String> followers = new HashSet<>(((HashSet<String>) result.getProperty("followers")));

		newPostIndex.setProperty("receivers", followers);

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(newPost);
		datastoreService.put(newPostIndex);
		transaction.commit();

		return newPost;
	}

	@ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public void follow(@Named("user") String user, @Named("toFollow") String toFollow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getFollowersByUser(user);
		HashSet<String> followers = (HashSet<String>) result.getProperty("followers");

		result.setProperty("followers", followers.add(toFollow));

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		transaction.commit();
	}

	@ApiMethod(name = "unfollow", httpMethod = HttpMethod.POST)
	public void unfollow(@Named("user") String user, @Named("toFollow") String toUnfollow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getFollowersByUser(user);
		HashSet<String> followers = (HashSet<String>) result.getProperty("followers");

		result.setProperty("followers", followers.remove(toUnfollow));

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		transaction.commit();
	}

	private Entity getFollowersByUser(String user) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("Follow")
				.setFilter(new Query.FilterPredicate("__key__", Query.FilterOperator.EQUAL, user));
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		return preparedQuery.asSingleEntity();
	}
}
