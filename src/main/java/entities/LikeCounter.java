package entities;

import com.google.appengine.api.datastore.*;

import java.util.Random;

public class LikeCounter {
	private static Random rand = new Random();

	public static Entity generateLike(String key, int i) {
		Entity like = new Entity("LikeCounter", key + ":like:" + i, KeyFactory.createKey("Post", key));
		like.setProperty("like", 0);
		like.setProperty("message", key);

		return like;
	}


	public static Result countLike(Key key) {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		//Query query = new Query("LikeCounter")
		//		.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN_OR_EQUAL, KeyFactory.createKey(key, "LikeCounter", key.getName() + ":like:0")))
		//		.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.LESS_THAN, KeyFactory.createKey(key, "LikeCounter", key.getName() + ":like:10")));

		//PreparedQuery pq = DS.prepare(query);
		Entity oneCounter;
		long counter = 0;
		for (int i = 0; i < 10; i++) {
			try {
				oneCounter = DS.get(KeyFactory.createKey(key, "LikeCounter", key.getName() + ":like:" + i));
				counter += (long) oneCounter.getProperty("like");
			} catch (EntityNotFoundException e) {
				e.printStackTrace();
			}

		}
		return new Result(200, counter);
	}

	public static Result like(Key key) throws EntityNotFoundException {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		boolean done = false;
		Transaction transaction = DS.beginTransaction();
		Entity like = DS.get(KeyFactory.createKey(key, "LikeCounter", key.getName() + ":like:" + rand.nextInt(10)));
		long nb = (long) like.getProperty("like");
		like.setProperty("like", nb + 1);
		DS.put(like);
		done = true;
		transaction.commit();
		if (done) {
			return new Result(200, "OK");
		} else {
			return new Result(500, "like non comptabilisé");
		}

	}
}
