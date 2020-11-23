package business;


import com.google.api.server.spi.config.*;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import entities.*;

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
	public CollectionResponse<Post> getTimeline(@Named("user") String user, @Nullable @Named("cursorString") String cursorString)
		throws EntityNotFoundException {
		user = user.replace("%3A", ":");

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
		for (Entity entity : msgs.values()) {
			posts.add(Post.fetchUserAndFormat(entity));
		}

	    return CollectionResponse.<Post>builder().setItems(posts).setNextPageToken(cursorString).build();
	}

	@ApiMethod(name = "like", httpMethod = HttpMethod.POST)
	public Result like(@Named("keyString") String key) throws EntityNotFoundException {
		return LikeCounter.like(KeyFactory.createKey("Post", key.replace("%3A", ":")));
	}

	@ApiMethod(name = "getCountLike", httpMethod = HttpMethod.GET)
	public Result getCountLike(Key key) {
		return LikeCounter.countLike(key);
	}

	@ApiMethod(name = "postMessage", httpMethod = HttpMethod.POST)
	public Entity postMessage(Post post) {
		return Post.postMessage(post);
	}

	@ApiMethod(name = "updateUser", httpMethod = HttpMethod.POST)
	public Entity updateUser(User user) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity userDB;

		try {
			userDB = datastoreService.get(KeyFactory.createKey("User", user.getEmail()));
		} catch (EntityNotFoundException e) {
			userDB = new Entity("User", user.getEmail());
		}

		userDB.setProperty("name", user.getName());
		userDB.setProperty("urlAvatar", user.getUrlAvatar());

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(userDB);
		transaction.commit();

		return userDB;
	}

	@ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public Result follow(Follow follow) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getKindByKey("Follow", KeyFactory.createKey(KeyFactory.createKey("User", follow.getUser().getEmail()), "Follow", follow.getUser().getEmail() + ":follow"), datastoreService);
		HashSet<String> followers = null;

		if (result == null) result = new Entity("Follow", follow.getUser().getEmail() + ":follow", KeyFactory.createKey("User", follow.getUser().getEmail()));

		if (result.getProperty("following") == null) followers = new HashSet<>();
		else followers = new HashSet<>((ArrayList<String>) result.getProperty("following"));

		if (followers.size() + 1 > 20000) throw new ArrayStoreException("On ne peut pas follow plus de 20000 personnes.");

		followers.add(follow.getTarget().getEmail());
		result.setProperty("following", followers);

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		transaction.commit();

		return new Result(200, "OK");
	}

	@ApiMethod(name = "unfollow", httpMethod = HttpMethod.POST)
	public Result unfollow(Follow follow) throws NotFoundException {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity result = getKindByKey("Follow", KeyFactory.createKey(KeyFactory.createKey("User", follow.getUser().getEmail()), "Follow", follow.getUser().getEmail() + ":follow"), datastoreService);

		if (result == null || result.getProperty("following") == null) throw new NotFoundException("L'utilisateur ou la personne à unfollow n'existe pas.");

		HashSet<String> followers = new HashSet<>((List<String>) result.getProperty("following"));

		if (!followers.remove(follow.getTarget().getEmail())) throw new NotFoundException("L'utilisateur ou la personne à unfollow n'existe pas.");
		result.setProperty("following", followers);

		Transaction transaction = datastoreService.beginTransaction();
		datastoreService.put(result);
		transaction.commit();

		return new Result(200, "OK");
	}

	@ApiMethod(name = "followed.list", httpMethod = HttpMethod.GET)
	public CollectionResponse<User> followedList(@Named("user") String user, @Nullable @Named("next") String cursorString) throws NotFoundException, EntityNotFoundException {
		user = user.replace("%3A", ":");

		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Entity follow = getKindByKey("Follow", KeyFactory.createKey(KeyFactory.createKey("User", user), "Follow", user + ":follow"), datastoreService);

		if (follow == null) throw new NotFoundException("L'utilisateur n'existe pas ou ne follow personne.");
		if (follow.getProperty("following") == null) return CollectionResponse.<User>builder().setItems(new ArrayList<>()).setNextPageToken(null).build();

		List<String> followers = (List<String>) follow.getProperty("following");

		int currentCursor = 0;
		int nextCursor = 0;

		if (cursorString != null) currentCursor = Integer.parseInt(cursorString);

		nextCursor = currentCursor + 10;

		if (nextCursor >= followers.size()) {
			nextCursor = followers.size();
			followers = followers.subList(currentCursor, nextCursor);
		}
		else followers = followers.subList(currentCursor, nextCursor);

		List<User> users = new ArrayList<>();
		for (String follower : followers) {
			Entity u = datastoreService.get(KeyFactory.createKey("User", follower));
			users.add(User.entityToUser(u));
		}

		String toSend = nextCursor == followers.size() ? null : String.valueOf(nextCursor);
		return CollectionResponse.<User>builder().setItems(users).setNextPageToken(toSend).build();
	}

	@ApiMethod(name = "followers.list", httpMethod = HttpMethod.GET)
	public CollectionResponse<User> followersList(@Named("user") String user, @Nullable @Named("next") String cursorString) throws EntityNotFoundException {
		user = user.replace("%3A", ":");

		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("Follow")
				.setFilter(new Query.FilterPredicate("following", Query.FilterOperator.EQUAL, user))
				.setKeysOnly();
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);

		if (cursorString != null) fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));

		QueryResultList<Entity> resultList = preparedQuery.asQueryResultList(fetchOptions);

		ArrayList<Key> userKeys = new ArrayList<>();
		resultList.forEach(entity -> userKeys.add(entity.getParent()));

		ArrayList<User> users = new ArrayList<>();
		for (Key userKey : userKeys) {
			Entity u = datastoreService.get(userKey);
			users.add(User.entityToUser(u));
		}

		cursorString = resultList.getCursor().toWebSafeString();

		return CollectionResponse.<User>builder().setItems(users).setNextPageToken(cursorString).build();
	}

	private Entity getKindByKey(String kind, Key key, DatastoreService datastoreService) {
		Query query = new Query(kind)
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, key));
		PreparedQuery preparedQuery = datastoreService.prepare(query);

		return preparedQuery.asSingleEntity();
	}
}
