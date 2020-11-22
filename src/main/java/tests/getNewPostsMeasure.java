package tests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.*;
import entities.Follow;
import entities.Post;
import entities.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@WebServlet(
        name = "getTimeLineMeasure",
        urlPatterns = {"/getNewPostMeasure"}
)
public class getNewPostsMeasure extends HttpServlet {
    //method partially copied from Endpoint (impossible to use it directly)
    private List<Post> getTimeline(String user, @Nullable String cursorString, int nbPostRequired) {
        DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
        //recup postIndex
        Query query = new Query("PostIndex")
                .setFilter(new Query.FilterPredicate("receivers", Query.FilterOperator.EQUAL, user))
                .addSort(Entity.KEY_RESERVED_PROPERTY, Query.SortDirection.DESCENDING)
                .setKeysOnly();

        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(nbPostRequired);

        PreparedQuery prepquery = DS.prepare(query);
        QueryResultList<Entity> postsI = prepquery.asQueryResultList(fetchOptions);

        ArrayList<Key> keys = new ArrayList<>();
        postsI.forEach(entity -> keys.add(entity.getParent()));

        Map<Key, Entity> msgs = DS.get(keys);

        List<Post> posts = new ArrayList<>();
        msgs.values().forEach(entity -> posts.add(Post.entityToPost(entity)));

        return posts;
    }

    //create nMessages from user
    private void createMessages(String user, int nMessages) {
        Post message;
        for (int i = 1; i <= nMessages; i++) {
            StringBuilder sender = new StringBuilder(user);
            StringBuilder body = new StringBuilder("Hello, this is the test ");
            body.append(i);
            body.append(".");
            message = new Post(sender.toString(),  body.toString(), null);
            Post.postMessage(message);
        }
    }

    private void deleteMsgTo(String user) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query query = new Query("PostIndex")
                .setFilter(new Query.FilterPredicate("receivers", Query.FilterOperator.EQUAL, user))
                .addSort(Entity.KEY_RESERVED_PROPERTY, Query.SortDirection.DESCENDING)
                .setKeysOnly();

        PreparedQuery preparedQuery = datastoreService.prepare(query);
        List<Entity> postIndex = preparedQuery.asList(FetchOptions.Builder.withDefaults());
        List<Key> postIndexKeys = new ArrayList<>();
        postIndex.forEach(index -> postIndexKeys.add(index.getKey()));

        List<Key> postKeys = new ArrayList<>();
        postIndex.forEach(index -> postKeys.add(index.getParent()));


        List<Key> likeKeys = new ArrayList<>();
        postKeys.forEach(key -> {
            for (int i = 1; i < 10; i++) {
                likeKeys.add(KeyFactory.createKey(key, "LikeCounter", key.getName() + ":like:" + i));
            }
        });
        datastoreService.delete(likeKeys);

        datastoreService.delete(postIndexKeys);
        datastoreService.delete(postKeys);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().println("Tests for getting 10, 100 or 500 posts by an user : \n");

        //users in the test
        String user1 = "Alice";
        String user2 = "Bob";
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        //create follow
        //method copied from Endpoint (impossible to use it directly)
        Entity follow = new Entity("Follow", user2);
        HashSet<String> following = new HashSet<>();
        following.add(user1);
        follow.setProperty("following", following);
        datastoreService.put(follow);
        createMessages(user1, 500);

        long startTime;
        long endTime;
        List<Post> listPost;

        long moyenne10 = 0;
        //create the 10 firsts messages to see if we can get them in a correct time
        for (int i = 0; i < 30; i++){
            //start test
            startTime = System.currentTimeMillis();
            listPost = getTimeline(user2, null, 10);
            endTime = System.currentTimeMillis();
            //end test
            if (listPost.size() != 10) response.getWriter().println("messages missing\n");
            moyenne10 += endTime-startTime;
        }
        moyenne10 = moyenne10/30;
        response.getWriter().println("On 30 tests, getting 10 news posts, the getTimeLine method perform on average " + moyenne10 + " ms.");

        long moyenne100 = 0;
        for (int i = 0; i < 30; i++){
            //start test
            startTime = System.currentTimeMillis();
            listPost = getTimeline(user2, null, 100);
            endTime = System.currentTimeMillis();
            //end test
            moyenne100 += endTime-startTime;
            if (listPost.size() != 100) response.getWriter().println("messages missing\n");
        }
        moyenne100 = moyenne100/30;
        response.getWriter().println("On 30 tests, getting 100 news posts, the getTimeLine method perform on average " + moyenne100 + " ms.");

        long moyenne500 = 0;
        for (int i = 0; i < 30; i++){
            //start test
            startTime = System.currentTimeMillis();
            listPost = getTimeline(user2, null, 500);
            endTime = System.currentTimeMillis();
            //end test
            moyenne500 += endTime-startTime;
            if (listPost.size() != 500) response.getWriter().println("messages missing\n");
        }
        moyenne500 = moyenne500/30;
        response.getWriter().println("On 30 tests, getting 500 news posts, the getTimeLine method perform on average " + moyenne500 + " ms.");

        //delete msg and follow used during the test
        deleteMsgTo(user2);
        datastoreService.delete(follow.getKey());
        response.getWriter().println("tested objects are deleted");
    }
}
