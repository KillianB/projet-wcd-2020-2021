package business;

import com.google.appengine.api.datastore.*;
import entities.Post;
import jakarta.xml.bind.DatatypeConverter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class PostMessage {
	public static Entity postMessage(Post post) {
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
}
