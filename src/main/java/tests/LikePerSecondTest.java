package tests;

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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		User user = new User("t@t.com", "t", "t");
		Entity userE = new Entity("User", user.getEmail());
		userE.setProperty("name", user.getName());
		userE.setProperty("urlAvatar", user.getUrlAvatar());

		datastoreService.put(userE);

		Entity post = Post.postMessage(new Post(user, "", ""));

		List<Runnable> runnables = createRunnables(10, post.getKey());

		for (int i = 0; i < 30; i++) {
			try {
				TimeUnit.SECONDS.sleep(1);
				executeRunnables(runnables);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		response.getWriter().println("10 likes par seconde pendant 30 secondes : " + LikeCounter.countLike(post.getKey()).getObject() + "/300.");

		runnables.addAll(createRunnables(10, post.getKey()));

		for (int i = 0; i < 30; i++) {
			try {
				TimeUnit.SECONDS.sleep(1);
				executeRunnables(runnables);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		response.getWriter().println("20 likes par seconde pendant 30 secondes : " + ((long) LikeCounter.countLike(post.getKey()).getObject() - 300) + "/600.");

		runnables.addAll(createRunnables(20, post.getKey()));

		for (int i = 0; i < 30; i++) {
			try {
				TimeUnit.SECONDS.sleep(1);
				executeRunnables(runnables);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		response.getWriter().println("40 likes par seconde pendant 30 secondes : " + ((long) LikeCounter.countLike(post.getKey()).getObject() - 900) + "/1200.");

		runnables.addAll(createRunnables(60, post.getKey()));

		for (int i = 0; i < 30; i++) {
			try {
				TimeUnit.SECONDS.sleep(1);
				executeRunnables(runnables);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		response.getWriter().println("100 likes par seconde pendant 30 secondes : " + ((long) LikeCounter.countLike(post.getKey()).getObject() - 2100) + "/3000.");

		try {
			TimeUnit.SECONDS.sleep(1);
			removeAllEntities();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private List<Runnable> createRunnables(int nb, Key postKey) {
		List<Runnable> tasks = new ArrayList<>();

		for (int i = 0; i < nb; i++) {
			tasks.add(() -> {
				try {
					LikeCounter.like(postKey);
				} catch (EntityNotFoundException e) {
					e.printStackTrace();
				}
			});
		}

		return tasks;
	}

	private void executeRunnables(List<Runnable> runnables) {
		runnables.forEach(Runnable::run);
	}

	private void removeAllEntities() {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("PostIndex")
				.setKeysOnly();

		PreparedQuery preparedQuery = datastoreService.prepare(query);
		Entity entity = preparedQuery.asSingleEntity();
		Key postKey = entity.getParent();

		datastoreService.delete(postKey);
		datastoreService.delete(entity.getKey());

		List<Key> likeCountersKey = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			likeCountersKey.add(KeyFactory.createKey(postKey, "LikeCounter", postKey.getName() + ":like:" + i));
		}

		datastoreService.delete(likeCountersKey);
		datastoreService.delete(KeyFactory.createKey("User", "t@t.com"));
	}
}
