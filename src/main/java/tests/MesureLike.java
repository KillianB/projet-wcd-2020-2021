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


@WebServlet(
        name = "likeMesure",
        urlPatterns = {"/likeMesure"}
)
public class MesureLike extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Key key = createMessages(new User());
        long totalLike = 0;
        List<Thread> thread = new ArrayList<>();
        long currentlyNbLike = (long)(LikeCounter.countLike(key).getObject());
        while (currentlyNbLike == totalLike) {
            for (int i = 0; i < thread.size(); i++) {
                thread.set(i, new Thread(new Run(key)));
            }
            totalLike = (long)(LikeCounter.countLike(key).getObject());

        }
                new Thread(new Run(key));


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