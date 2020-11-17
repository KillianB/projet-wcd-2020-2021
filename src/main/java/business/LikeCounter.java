package business;

import java.util.List;
import java.util.Random;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

import entities.Result;

import com.google.appengine.api.datastore.Query.FilterOperator;

class LikeCounter {
	private static Random rand = new Random();
	public static Entity generateLike(String keyReservedProperty,int i) {
		Entity like = new Entity("likeCounter", keyReservedProperty+"like"+i);
		like.setProperty("like", (int)0);
		like.setProperty("message", keyReservedProperty);
		
		return like;
	}
	
	
	public static Result countLike(String keyReservedProperty) {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("likeCounter")
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN_OR_EQUAL, keyReservedProperty + "like0"))
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.LESS_THAN, keyReservedProperty + "like10"));
		
		PreparedQuery pq = DS.prepare(query);
		List<Entity> result = pq.asList(FetchOptions.Builder.withDefaults());
		Integer counter = 0; 
		for (Entity i : result) {
			counter += (int) i.getProperty("like");
		}
		return new Result(200, counter);
	}
	
	public static Result like(String keyReservedProperty) {
		DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
		boolean done = false;
		Transaction transaction = DS.beginTransaction();
		Query query = new Query("LikeCounter")
				.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.EQUAL, keyReservedProperty+"like"+rand.nextInt(10)));
		PreparedQuery pq = DS.prepare(query);
		List<Entity> likeList = pq.asList(FetchOptions.Builder.withDefaults());
		Entity like = likeList.get(0);
		int nb = (int)like.getProperty("like");
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
