package tests;

import com.google.appengine.api.datastore.*;
import entities.Post;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@WebServlet(
		name = "TestPost",
		urlPatterns = {"/addposttest"}
)
public class AddPostTest extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");

		response.getWriter().println("Tests pour la création d'un post lorsque l'utilisateur à 10, 100 ou 500 followers");

		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		String toFollow = "ToFollowTest";
		String followers = "TestUser";
		long startTime = 0;
		long endTime = 0;
		long totalTime = 0;

		addFollowers(toFollow, followers, 1, 10, datastoreService);

		for (int i = 0; i < 30; i++) {
			startTime = System.currentTimeMillis();
			Post.postMessage(new Post(toFollow, "Test", "Test"));
			endTime = System.currentTimeMillis();
			totalTime += endTime - startTime;
		}

		response.getWriter().println("Temps pour envoyer un message avec 10 followers : " + totalTime/30 + " ms");

		addFollowers(toFollow, followers, 11, 100, datastoreService);

		totalTime = 0;
		for (int i = 0; i < 30; i++) {
			startTime = System.currentTimeMillis();
			Post.postMessage(new Post(toFollow, "Test", "Test"));
			endTime = System.currentTimeMillis();
			totalTime += endTime - startTime;
		}

		response.getWriter().println("Temps pour envoyer un message avec 100 followers : " + totalTime/30 + " ms");

		addFollowers(toFollow, followers, 101, 500, datastoreService);

		totalTime = 0;
		for (int i = 0; i < 30; i++) {
			startTime = System.currentTimeMillis();
			Post.postMessage(new Post(toFollow, "Test", "Test"));
			endTime = System.currentTimeMillis();
			totalTime += endTime - startTime;
		}

		response.getWriter().println("Temps pour envoyer un message avec 500 followers : " + totalTime/30 + " ms");

		try {
			TimeUnit.SECONDS.sleep(1);
			removeAllEntities();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void addFollowers(String toFollow, String followersName, int start, int end, DatastoreService datastoreService) {
		Entity follow = null;
		HashSet<String> following = null;

		for (int i = start; i <= end; i++) {
			follow = new Entity("Follow", followersName + i);
			following = new HashSet<>();
			following.add(toFollow);
			follow.setProperty("following", following);

			datastoreService.put(follow);
		}
	}

	private void removeAllEntities() {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("PostIndex")
				.setFilter(new Query.FilterPredicate("receivers", Query.FilterOperator.EQUAL, "TestUser1"))
				.addSort(Entity.KEY_RESERVED_PROPERTY, Query.SortDirection.DESCENDING)
				.setKeysOnly();

		PreparedQuery preparedQuery = datastoreService.prepare(query);
		List<Entity> postIndex = preparedQuery.asList(FetchOptions.Builder.withDefaults());
		List<Key> postIndexKeys = new ArrayList<>();
		postIndex.forEach(index -> postIndexKeys.add(index.getKey()));

		List<Key> postKeys = new ArrayList<>();
		postIndex.forEach(index -> postKeys.add(index.getParent()));

		datastoreService.delete(postIndexKeys);
		datastoreService.delete(postKeys);

		List<Key> followKeys = new ArrayList<>();
		for (int i = 1; i <= 500; i++) {
			followKeys.add(KeyFactory.createKey("Follow", "TestUser" + i));
		}

		datastoreService.delete(followKeys);

		List<Key> likeCountersKey = new ArrayList<>();
		postKeys.forEach(key -> {
			for (int i = 0; i < 10; i++) {
				likeCountersKey.add(KeyFactory.createKey(key, "LikeCounter", key.getName() + ":like:" + i));
			}
		});

		datastoreService.delete(likeCountersKey);
	}
}
