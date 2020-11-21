package business;


import com.google.api.server.spi.config.*;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import entities.Follow;
import entities.Post;
import entities.Result;
import jakarta.xml.bind.DatatypeConverter;

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
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		//recup postIndex
		Query query = new Query("PostIndex")
				.setFilter(new Query.FilterPredicate("receivers", FilterOperator.EQUAL, user))
				.addSort(Entity.KEY_RESERVED_PROPERTY, SortDirection.DESCENDING)
				.setKeysOnly();

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		PreparedQuery prepquery = DS.prepare(query);
		QueryResultList<Entity> postsI = prepquery.asQueryResultList(fetchOptions);

		ArrayList<Key> keys = new ArrayList<>();
		postsI.forEach(entity -> keys.add(entity.getParent()));

		Map<Key, Entity> msgs = DS.get(keys);

		cursorString = postsI.getCursor().toWebSafeString();

		List<Post> posts = new ArrayList<>();
		msgs.values().forEach(entity -> posts.add(Post.entityToPost(entity)));

	    return CollectionResponse.<Post>builder().setItems(posts).setNextPageToken(cursorString).build();
	}

	@ApiMethod(name = "like", httpMethod = HttpMethod.POST)
	public Result like(@Named("keyString") String key) throws EntityNotFoundException {
		System.out.println();
		return LikeCounter.like(key.replace("%3A", ":"));
	}

	@ApiMethod(name = "getCountLike", httpMethod = HttpMethod.GET)
	public Result getCountLike(@Named("keyString") String key) {
		return LikeCounter.countLike(key.replace("%3A", ":"));
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

	@ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public Result follow(Follow follow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getKindByKey("Follow", KeyFactory.createKey("Follow", follow.getUser()), datastoreService);
		HashSet<String> followers = null;

		if (result == null) result = new Entity("Follow", follow.getUser());

		if (result.getProperty("following") == null) followers = new HashSet<>();
		else followers = new HashSet<>((ArrayList<String>) result.getProperty("following"));

		followers.add(follow.getTarget());
		result.setProperty("following", followers);

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		transaction.commit();

		return new Result(200, "OK");
	}

	@ApiMethod(name = "unfollow", httpMethod = HttpMethod.POST)
	public Result unfollow(Follow follow) throws NotFoundException {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getKindByKey("Follow", KeyFactory.createKey("Follow", follow.getUser()), datastoreService);

		if (result == null || result.getProperty("following") == null) throw new NotFoundException("L'utilisateur ou la personne à unfollow n'existe pas.");

		HashSet<String> followers = (HashSet<String>) result.getProperty("following");

		if (!followers.remove(follow.getTarget())) throw new NotFoundException("L'utilisateur ou la personne à unfollow n'existe pas.");
		result.setProperty("following", followers);

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		transaction.commit();

		return new Result(200, "OK");
	}

	@ApiMethod(name = "followers.list", httpMethod = HttpMethod.GET)
	public Result followersList(@Named("user") String user) throws NotFoundException {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity follow = getKindByKey("Follow", KeyFactory.createKey("Follow", user), datastoreService);

		if (follow == null || follow.getProperty("following") == null) throw new NotFoundException("L'utilisateur n'existe pas ou ne follow personne.");

		return new Result(200, follow.getProperty("following"));
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
}
