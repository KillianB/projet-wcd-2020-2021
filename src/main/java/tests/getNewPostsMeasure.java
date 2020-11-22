package tests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.*;
import entities.Post;
import entities.User;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@WebServlet(
        name = "getTimeLineMeasure",
        urlPatterns = {"/getNewPostMeasure"}
)
public class getNewPostsMeasure extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().println("Tests for getting 10, 100 or 500 posts by an user : <br/>");

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        User bob = new User("Test", "", "");

        Entity follow = new Entity("Follow", "Alice");
        HashSet<String> following = new HashSet<>();
        following.add("Bob");
        follow.setProperty("following", following);

        datastoreService.put(follow);

        long startTime;
        long endTime;
        long result = 0;

        createMessages(bob, 10);

        //create the 10 firsts messages to see if we can get them in a correct time
        for (int i = 0; i < 30; i++){
            //start test
            startTime = System.currentTimeMillis();
            getTimeline("Alice", null);
            endTime = System.currentTimeMillis();
            //end test
            result += endTime-startTime;
        }

        response.getWriter().println("On 30 tests, getting 10 news posts, the getTimeLine method perform on average " + result/30 + " ms.");

        result = 0;
        createMessages(bob, 90);

        for (int i = 0; i < 30; i++){
            //start test
            startTime = System.currentTimeMillis();
            getTimeline("Alice", null);
            endTime = System.currentTimeMillis();
            //end test
            result += endTime-startTime;
        }

        response.getWriter().println("On 30 tests, getting 100 news posts, the getTimeLine method perform on average " + result/30 + " ms.");

        result = 0;
        createMessages(bob, 400);

        for (int i = 0; i < 30; i++){
            //start test
            startTime = System.currentTimeMillis();
            getTimeline("Alice", null);
            endTime = System.currentTimeMillis();
            //end test
            result += endTime-startTime;
        }

        response.getWriter().println("On 30 tests, getting 500 news posts, the getTimeLine method perform on average " + result/30 + " ms.");

        try {
            TimeUnit.SECONDS.sleep(1);
            removeAllEntities();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //method copied from Endpoint (impossible to use it directly)
    private CollectionResponse<Post> getTimeline(String user, @Nullable String cursorString) {
        DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
        //recup postIndex
        Query query = new Query("PostIndex")
                .setFilter(new Query.FilterPredicate("receivers", Query.FilterOperator.EQUAL, user))
                .addSort(Entity.KEY_RESERVED_PROPERTY, Query.SortDirection.DESCENDING)
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
        msgs.values().forEach(entity -> posts.add(Post.entityToPost(entity)));

        return CollectionResponse.<Post>builder().setItems(posts).setNextPageToken(cursorString).build();
    }

    //create nMessages from user
    private void createMessages(User user, int nMessages) {
        for (int i = 0; i < nMessages; i++) {
            Post.postMessage(new Post(user,  "", ""));
        }
    }

    private void removeAllEntities() {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query query = new Query("PostIndex")
                .setFilter(new Query.FilterPredicate("receivers", Query.FilterOperator.EQUAL, "Alice"))
                .addSort(Entity.KEY_RESERVED_PROPERTY, Query.SortDirection.DESCENDING)
                .setKeysOnly();

        PreparedQuery preparedQuery = datastoreService.prepare(query);
        List<Entity> postIndex = preparedQuery.asList(FetchOptions.Builder.withDefaults());
        List<Key> postIndexKeys = new ArrayList<>();
        postIndex.forEach(index -> postIndexKeys.add(index.getKey()));

        List<Key> postKeys = new ArrayList<>();
        postIndex.forEach(index -> postKeys.add(index.getParent()));

        List<Key> likeCountersKey = new ArrayList<>();
        postKeys.forEach(key -> {
            for (int i = 0; i < 10; i++) {
                likeCountersKey.add(KeyFactory.createKey(key, "LikeCounter", key.getName() + ":like:" + i));
            }
        });

        datastoreService.delete(postIndexKeys);
        datastoreService.delete(postKeys);
        datastoreService.delete(KeyFactory.createKey("Follow", "Alice"));
        datastoreService.delete(likeCountersKey);
    }
}
