package business;


import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.repackaged.com.google.datastore.v1.client.DatastoreException;

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
	public List<Post> getTimeline(User user, String cursorString) {
		List<Post> posts = new ArrayList<>();
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		//recup postIndex
		Query query = new Query("PostIndex")
				.setFilter(new Query.FilterPredicate("receivers", FilterOperator.EQUAL, user.getId()))
				.addSort(Entity.KEY_RESERVED_PROPERTY, SortDirection.DESCENDING);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);
		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}
		PreparedQuery prepquery = DS.prepare(query);
		QueryResultList<Entity> postsI = prepquery.asQueryResultList(fetchOptions);

		//recup les parents des PostIndex (donc les posts d'origine)
		for (Entity i : postsI) {
			query = new Query("Post")
					.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.EQUAL, i.getParent()));
			PreparedQuery pq = DS.prepare(query);
			List<Entity> message = pq.asList(FetchOptions.Builder.withDefaults());
			//add chacun des posts à la timeline
			posts.add(Post.entityToPost(i));
		}
	    cursorString = postsI.getCursor().toWebSafeString();

		return posts;
	}
	
	@ApiMethod(name="like", httpMethod = HttpMethod.POST)
	public boolean like(String keyReservedProperty) {
		return LikeCounter.like(keyReservedProperty);
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
						.append(Long.MAX_VALUE-(new Date()).getTime())
						.append(":")
						.append(post.getSender());
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
				.setFilter(new Query.FilterPredicate("following", Query.FilterOperator.EQUAL, post.getSender()));

		PreparedQuery preparedQuery = datastoreService.prepare(query);
		List<Entity> result = preparedQuery.asList(FetchOptions.Builder.withDefaults());

		List<String> keys = new ArrayList<>();
		result.forEach(entity -> keys.add(entity.getParent().toString()));

		HashSet<String> followers = new HashSet<>(keys);

		newPostIndex.setProperty("receivers", followers);

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(newPost);
		datastoreService.put(newPostIndex);
		for (int i = 0; i < 10; i++) {
			datastoreService.put(LikeCounter.generateLike(newPost.KEY_RESERVED_PROPERTY, i));
		}
		
		transaction.commit();

		return newPost;
	}

	@ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public void follow(@Named("user") String user, @Named("toFollow") String toFollow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getFollowersByUser(user, datastoreService);
		HashSet<String> followers = (HashSet<String>) result.getProperty("following");

		result.setProperty("followers", followers.add(toFollow));

		List<Entity> messages = getAllPostIndexOfUser(toFollow, datastoreService);

		messages.forEach(message -> message.setProperty("receivers", ((HashSet<String>)message.getProperty("receivers")).add(user)));

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		datastoreService.put(messages);
		transaction.commit();
	}

	@ApiMethod(name = "unfollow", httpMethod = HttpMethod.POST)
	public void unfollow(@Named("user") String user, @Named("toFollow") String toUnfollow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getFollowersByUser(user, datastoreService);
		HashSet<String> followers = (HashSet<String>) result.getProperty("following");

		result.setProperty("followers", followers.remove(toUnfollow));

		List<Entity> messages = getAllPostIndexOfUser(toUnfollow, datastoreService);

		messages.forEach(message -> message.setProperty("receivers", ((HashSet<String>)message.getProperty("receivers")).remove(user)));

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		datastoreService.put(messages);
		transaction.commit();
	}

	private Entity getFollowersByUser(String user, DatastoreService datastoreService) {
		Query query = new Query("Follow")
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, user));
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		return preparedQuery.asSingleEntity();
	}

	private List<Entity> getAllPostIndexOfUser(@Named("user") String user, DatastoreService datastoreService) {
		Query query = new Query("PostIndex")
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, user));
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		return preparedQuery.asList(FetchOptions.Builder.withDefaults());
	}
}
