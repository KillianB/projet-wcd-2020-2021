package tests;

import com.google.appengine.api.datastore.*;
import entities.LikeCounter;
import entities.Post;
import entities.User;
import jakarta.xml.bind.DatatypeConverter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;


@WebServlet(
        name = "likeMesure",
        urlPatterns = {"/likeMesure"}
)
public class MesureLike extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().println("Tests for measuring how many likes we can assure in a sec");


        Key key = createMessages(new User());
        long totalLike = 0;
        List<Thread> thread = new ArrayList<>();
        thread.add(null);
        thread.add(null);
        thread.add(null);
        thread.add(null);
        thread.add(null);
        thread.add(null);
        thread.add(null);
        thread.add(null);
        thread.add(null);
        thread.add(null);
        int success = thread.size();
        long currentlyNbLike = (long)(LikeCounter.countLike(key).getObject());

        while (currentlyNbLike == totalLike+thread.size()) {
            for (int i = 0; i < thread.size(); i++) {
                thread.set(i, new Thread(new Run(key)));
                thread.get(i).start();
            }
            thread.add(new Thread(new Run(key)));

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int j = 0; j < thread.size(); j++) {
                thread.get(j).interrupt();
            }
            totalLike = (long)(LikeCounter.countLike(key).getObject());
        }

        response.getWriter().println("for this test we can assure at least "+ (success-2) + " likes in a second.");

    }

    private Key createMessages(User user) {
         return (Post.postMessage(new Post(user, "",""))).getKey();
    }
    private class Run implements Runnable {
        private Key key;
        public Run(Key key) {
            this.key = key;
        }

        @Override
        public void run() {
            try {
                LikeCounter.like(this.key);

            } catch (EntityNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}