package business;

import java.util.List;
import java.util.Random;

import com.google.appengine.api.datastore.*;

import entities.Result;

import com.google.appengine.api.datastore.Query.FilterOperator;

class LikeCounter {
	private static Random rand = new Random();
	public static Entity generateLike(String key,int i) {
		Entity like = new Entity("LikeCounter", key + ":like:" + i, KeyFactory.createKey("Post", key));
		like.setProperty("like", 0);
		like.setProperty("message", key);

		return like;
	}


	public static Result countLike(String key) {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("LikeCounter")
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN_OR_EQUAL, KeyFactory.createKey(KeyFactory.createKey("Post", key), "LikeCounter", key + ":like:0")))
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.LESS_THAN, KeyFactory.createKey(KeyFactory.createKey("Post", key), "LikeCounter", key + ":like:10")));

		PreparedQuery pq = DS.prepare(query);
		List<Entity> result = pq.asList(FetchOptions.Builder.withDefaults());
		long counter = 0;
		for (Entity i : result) {
			counter += (long) i.getProperty("like");
		}
		return new Result(200, counter);
	}

	public static Result like(String key) throws EntityNotFoundException {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		boolean done = false;
		Transaction transaction = DS.beginTransaction();
		Entity like = DS.get(KeyFactory.createKey("LikeCounter", key + ":like:" + + rand.nextInt(10)));
		long nb = (long)like.getProperty("like");
		like.setProperty("like", nb + 1);
		DS.put(like);
		done = true;
		transaction.commit();
		if (done) {
			return new Result(200,"OK");
		} else {
			return new Result(500, "like non comptabilisé");
		}

	}
}
