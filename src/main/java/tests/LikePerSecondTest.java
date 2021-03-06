package tests;

import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.*;
import entities.LikeCounter;
import entities.Post;
import entities.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@WebServlet(
		name = "LikePerSecond",
		urlPatterns = {"/likepersecond"}
)
public class LikePerSecondTest extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");

		response.getWriter().println("Tests pour le nombre de like par seconde.");

		int nbLike = Integer.parseInt((String) request.getParameter("nb"));
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		User user = new User("t@t.com", "t", "t");
		User dummy = new User("dummy@t.com", "d", "d");

		Entity userE = new Entity("User", user.getEmail());
		userE.setProperty("name", user.getName());
		userE.setProperty("urlAvatar", user.getUrlAvatar());
		Entity dummyE = new Entity("User", dummy.getEmail());
		dummyE.setProperty("name", dummy.getName());
		dummyE.setProperty("urlAvatar", dummy.getUrlAvatar());

		Entity follow = new Entity("Follow", dummy.getEmail() + ":follow", dummyE.getKey());
		HashSet<String> following = new HashSet<>();
		following.add(user.getEmail());
		follow.setProperty("following", following);

		datastoreService.put(userE);
		datastoreService.put(dummyE);
		datastoreService.put(follow);

		Entity post = Post.postMessage(new Post(user, "", ""));

		List<Runnable> runnables = createRunnables(nbLike, post.getKey(), datastoreService);

		long total = 0;

		for (int i = 0; i < 30; i++) {
			total += executeRunnables(runnables);
		}

		response.getWriter().println(nbLike + " likes par seconde (" + total/30 + "ms en moyenne pour " + nbLike + ") pendant sur 30 essais : " + LikeCounter.countLike(post.getKey()).getObject() + "/" + (30 * nbLike) + ".");

		try {
			TimeUnit.SECONDS.sleep(1);
			removeAllEntities();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private List<Runnable> createRunnables(int nb, Key postKey, DatastoreService datastoreService) {
		List<Runnable> tasks = new ArrayList<>();

		for (int i = 0; i < nb; i++) {
			tasks.add(() -> {
				try {
					Transaction transaction = datastoreService.beginTransaction();

					Entity like = datastoreService.get(KeyFactory.createKey(postKey, "LikeCounter", postKey.getName() + ":like:" + (new Random()).nextInt(10)));

					long n = (long) like.getProperty("like");
					like.setProperty("like", n + 1);
					datastoreService.put(like);

					transaction.commit();
				} catch (EntityNotFoundException e) {
					e.printStackTrace();
				}
			});
		}

		return tasks;
	}

	private long executeRunnables(List<Runnable> runnables) {
		List<Thread> threads = new ArrayList<>();

		long start = System.currentTimeMillis();
		runnables.forEach(runnable -> {
			Thread thread = ThreadManager.createThreadForCurrentRequest(runnable);
			thread.start();
			threads.add(thread);
		});

		threads.forEach(thread -> {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		long end = System.currentTimeMillis();
		return end-start;
	}

	private void removeAllEntities() {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("PostIndex")
				.setFilter(new Query.FilterPredicate("receivers", Query.FilterOperator.EQUAL, "dummy@t.com"))
				.setKeysOnly();

		PreparedQuery preparedQuery = datastoreService.prepare(query);
		Entity entity = preparedQuery.asSingleEntity();
		Key postKey = entity.getParent();

		datastoreService.delete(entity.getKey());
		datastoreService.delete(postKey);

		List<Key> likeCountersKey = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			likeCountersKey.add(KeyFactory.createKey(postKey, "LikeCounter", postKey.getName() + ":like:" + i));
		}

		datastoreService.delete(likeCountersKey);
		datastoreService.delete(KeyFactory.createKey("User", "t@t.com"));
		datastoreService.delete(KeyFactory.createKey("User", "dummy@t.com"));
		datastoreService.delete(KeyFactory.createKey(KeyFactory.createKey("User", "dummy@t.com"), "Follow", "dummy@t.com:follow"));
	}
}
