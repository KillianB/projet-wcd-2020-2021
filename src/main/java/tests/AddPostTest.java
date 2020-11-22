package tests;

import business.PostMessage;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Transaction;
import entities.Post;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;

@WebServlet(
		name = "HelloAppEngine",
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

		addFollowers(toFollow, followers, 1, 10, datastoreService);

		long startTime = System.currentTimeMillis();
		PostMessage.postMessage(new Post(toFollow, "Test", "Test"));
		long endTime = System.currentTimeMillis();

		response.getWriter().println("Temps pour envoyer un message avec 10 followers : " + (endTime - startTime) + " ms");

		addFollowers(toFollow, followers, 11, 100, datastoreService);

		startTime = System.currentTimeMillis();
		PostMessage.postMessage(new Post(toFollow, "Test", "Test"));
		endTime = System.currentTimeMillis();

		response.getWriter().println("Temps pour envoyer un message avec 100 followers : " + (endTime - startTime) + " ms");

		addFollowers(toFollow, followers, 101, 500, datastoreService);

		startTime = System.currentTimeMillis();
		PostMessage.postMessage(new Post(toFollow, "Test", "Test"));
		endTime = System.currentTimeMillis();

		response.getWriter().println("Temps pour envoyer un message avec 500 followers : " + (endTime - startTime) + " ms");
	}

	private void addFollowers(String toFollow, String followersName, int start, int end, DatastoreService datastoreService) {
		Transaction transaction = datastoreService.beginTransaction();

		Entity follow = null;
		HashSet<String> following = null;

		for (int i = start; i <= end; i++) {
			follow = new Entity("Follow", followersName + i);
			following = new HashSet<>();
			following.add(toFollow);
			follow.setProperty("following", following);

			datastoreService.put(follow);
		}
		transaction.commit();
	}
}
