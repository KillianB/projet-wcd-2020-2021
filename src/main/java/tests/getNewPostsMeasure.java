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
        name = "HelloAppEngine",
        urlPatterns = {"/getNewPostMeasure"}
)
public class getNewPostsMeasure extends HttpServlet {
    //method copied from Endpoint (impossible to use it directly)
    private CollectionResponse<Post> getTimeline(String user, @Nullable String cursorString) {
        DatastoreService DS = DatastoreServiceFactory.getDatastoreService();
        //recup postIndex
        Query query = new Query("PostIndex")
                .setFilter(new Query.FilterPredicate("receivers", Query.FilterOperator.EQUAL, user))
                .addSort(Entity.KEY_RESERVED_PROPERTY, Query.SortDirection.DESCENDING)
                .setKeysOnly();

        FetchOptions fetchOptions = FetchOptions.Builder.withDefaults();

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

    //method copied from Endpoint (impossible to use it directly)
    private Entity getKindByKey(String kind, Key key, DatastoreService datastoreService) {
        Query query = new Query(kind)
                .setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, key));
        PreparedQuery preparedQuery = datastoreService.prepare(query);

        return preparedQuery.asSingleEntity();
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

        datastoreService.delete(postIndexKeys);
        datastoreService.delete(postKeys);
    }
    //method copied from Endpoint (impossible to use it directly)
    public Result unfollow(Follow follow) throws NotFoundException {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Entity result = getKindByKey("Follow", KeyFactory.createKey("Follow", follow.getUser()), datastoreService);

        if (result == null || result.getProperty("following") == null) throw new NotFoundException("L'utilisateur ou la personne à unfollow n'existe pas.");

        HashSet<String> followers = (HashSet<String>) result.getProperty("following");

        if (!followers.remove(follow.getTarget())) throw new NotFoundException("L'utilisateur ou la personne à unfollow n'existe pas.");
        result.setProperty("following", followers);

        Transaction transaction = datastoreService.beginTransaction();
        datastoreService.put(result);
        transaction.commit();

        return new Result(200, "OK");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().println("Tests for getting 10, 100 or 500 posts by an user : <br/>");

        //users in the test
        String user1 = "Alice";
        String user2 = "Bob";
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        //create follow
        //method copied from Endpoint (impossible to use it directly)
        Follow follow = new Follow(user2, user1);
        Entity result = getKindByKey("Follow", KeyFactory.createKey("Follow", follow.getUser()), datastoreService);
        HashSet<String> followers = null;

        if (result == null) result = new Entity("Follow", follow.getUser());

        if (result.getProperty("following") == null) followers = new HashSet<>();
        else followers = new HashSet<>((ArrayList<String>) result.getProperty("following"));

        followers.add(follow.getTarget());
        result.setProperty("following", followers);

        Transaction transaction = datastoreService.beginTransaction();
        datastoreService.put(result);
        transaction.commit();
        long startTime;
        long endTime;
        List<Long> resultOfTests10 = new ArrayList<Long>();

        //create the 10 firsts messages to see if we can get them in a correct time
        for (int i = 0; i < 30; i++){
            createMessages(user1, 10);
            //start test
            startTime = System.currentTimeMillis();
            getTimeline(user2, null);
            endTime = System.currentTimeMillis();
            //end test
            resultOfTests10.add(endTime-startTime);
        }
        long moyenne10 = 0;
        for (int i = 0; i < 30; i++) {
            moyenne10 += resultOfTests10.get(i);
        }
        moyenne10 = moyenne10/30;
        response.getWriter().println("On 30 tests, getting 10 news posts, the getTimeLine method perform on average " + moyenne10 + " ms.");

        List<Long> resultOfTests100 = new ArrayList<Long>();
        for (int i = 0; i < 30; i++){
            createMessages(user1, 100);
            //start test
            startTime = System.currentTimeMillis();
            getTimeline(user2, null);
            endTime = System.currentTimeMillis();
            //end test
            resultOfTests100.add(endTime-startTime);
        }
        long moyenne100 = 0;
        for (int i = 0; i < 30; i++) {
            moyenne100 += resultOfTests10.get(i);
        }
        moyenne100 = moyenne100/30;
        response.getWriter().println("On 30 tests, getting 100 news posts, the getTimeLine method perform on average " + moyenne100 + " ms.");

        List<Long> resultOfTests500 = new ArrayList<Long>();
        for (int i = 0; i < 30; i++){
            createMessages(user1, 500);
            //start test
            startTime = System.currentTimeMillis();
            getTimeline(user2, null);
            endTime = System.currentTimeMillis();
            //end test
            resultOfTests500.add(endTime-startTime);
        }
        long moyenne500 = 0;
        for (int i = 0; i < 30; i++) {
            moyenne500 += resultOfTests10.get(i);
        }
        moyenne500 = moyenne500/30;
        response.getWriter().println("On 30 tests, getting 500 news posts, the getTimeLine method perform on average " + moyenne500 + " ms.");

        //delete msg and follow used during the test
        try {
            deleteMsgTo(user2);
            unfollow(follow);
            response.getWriter().println("tested objects are deleted");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }
}
