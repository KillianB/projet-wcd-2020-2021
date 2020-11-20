package business;


import com.google.api.server.spi.config.*;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import entities.Post;
import entities.Result;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Api(name = "tinyApi",
		version = "v1",
		audiences = "1048463456874-56t3t922794hiac34phbfntkdqmt0lhl.apps.googleusercontent.com",
		clientIds = "1048463456874-56t3t922794hiac34phbfntkdqmt0lhl.apps.googleusercontent.com",
		namespace =
		@ApiNamespace(
				ownerDomain = "tinyinsta-295118.ew.r.appspot.com",
				ownerName = "tinyinsta-295118.ew.r.appspot.com",
				packagePath = "")
)

public class Endpoint {
	@ApiMethod(name = "timeline", httpMethod = HttpMethod.GET)
	public CollectionResponse<Post> getTimeline(@Named("user") String user, @Nullable @Named("cursorString") String cursorString) {
		List<Post> posts = new ArrayList<>();
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		//recup postIndex
		Query query = new Query("PostIndex")
				.setFilter(new Query.FilterPredicate("receivers", FilterOperator.EQUAL, user))
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

	    return CollectionResponse.<Post>builder().setItems(posts).setNextPageToken(cursorString).build();
	}

	@ApiMethod(name = "like", httpMethod = HttpMethod.POST)
	public Result like(@Named("keyReservedProperty") String keyReservedProperty) {
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
						.append(Long.MAX_VALUE - (new Date()).getTime())
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

		Transaction transaction = datastoreService.beginTransaction(TransactionOptions.Builder.withXG(true));

		datastoreService.put(newPost);
		datastoreService.put(newPostIndex);

		for (int i = 0; i < 10; i++) {
			datastoreService.put(LikeCounter.generateLike(Entity.KEY_RESERVED_PROPERTY, i));
		}

		transaction.commit();

		return newPost;
	}

	@ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public Result follow(@Named("user") String user, @Named("toFollow") String toFollow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getKindByKey("Follow", KeyFactory.createKey("Follow", user), datastoreService);
		HashSet<String> followers = (HashSet<String>) result.getProperty("following");

		result.setProperty("followers", followers.add(toFollow));

		List<Entity> messages = getAllPostIndexOfUser(toFollow, datastoreService);

		messages.forEach(message -> message.setProperty("receivers", ((HashSet<String>) message.getProperty("receivers")).add(user)));

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		datastoreService.put(messages);
		transaction.commit();

		return new Result(200, "OK");
	}

	@ApiMethod(name = "unfollow", httpMethod = HttpMethod.POST)
	public Result unfollow(@Named("user") String user, @Named("toUnfollow") String toUnfollow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getKindByKey("Follow", KeyFactory.createKey("Follow", user), datastoreService);
		HashSet<String> followers = (HashSet<String>) result.getProperty("following");

		result.setProperty("followers", followers.remove(toUnfollow));

		List<Entity> messages = getAllPostIndexOfUser(toUnfollow, datastoreService);

		messages.forEach(message -> message.setProperty("receivers", ((HashSet<String>) message.getProperty("receivers")).remove(user)));

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		datastoreService.put(messages);
		transaction.commit();

		return new Result(200, "OK");
	}

	@ApiMethod(name = "followers.list", httpMethod = HttpMethod.GET)
	public Result followersList(@Named("user") String user) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		return new Result(200, getKindByKey("Follow", KeyFactory.createKey("Follow", user), datastoreService).getProperty("following"));
	}

	@ApiMethod(name = "followed.list", httpMethod = HttpMethod.GET)
	public CollectionResponse<Entity> followedList(@Named("user") String user, @Nullable @Named("next") String cursorString) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("Follow")
				.setFilter(new Query.FilterPredicate("following", Query.FilterOperator.EQUAL, user))
				.setKeysOnly();
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);

		if (cursorString != null) fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));

		QueryResultList<Entity> resultList = preparedQuery.asQueryResultList(fetchOptions);
		cursorString = resultList.getCursor().toWebSafeString();

		return CollectionResponse.<Entity>builder().setItems(resultList).setNextPageToken(cursorString).build();
	}

	private Entity getKindByKey(String kind, Key key, DatastoreService datastoreService) {
		Query query = new Query(kind)
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, key));
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		return preparedQuery.asSingleEntity();
	}

	private List<Entity> getAllPostIndexOfUser(String user, DatastoreService datastoreService) {
		Query query = new Query("PostIndex")
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, user));
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		return preparedQuery.asList(FetchOptions.Builder.withDefaults());
	}
}
