package com.neo4j.homework.unmanagedextension;

import com.neo4j.homework.unmanagedextension.domain.TvShow;
import com.neo4j.homework.unmanagedextension.domain.User;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Domain service. Has access to graph database.
 */
public class DomainService {
    /**
     * Object for accessing graph database and execute commands.
     */
    private final GraphDatabaseService database;

    public DomainService(final GraphDatabaseService database) {
        this.database = database;
    }

    boolean createUser(final String mail, final String age, final String gender) {
        try ( Transaction tx = database.beginTx() ) {
            Node newUser = database.createNode(User.USER_LABEL);
            newUser.setProperty(User.MAIL, mail);
            newUser.setProperty(User.AGE, age);
            newUser.setProperty(User.GENDER, gender);
            tx.success();
            return true;
        }
    }

    boolean userExists(final String mail) {
        try ( Transaction tx = database.beginTx() ) {
            ResourceIterator<Node> nodes = database.findNodes(User.USER_LABEL, User.MAIL, mail);
            boolean userExists = nodes.hasNext();
            nodes.close();
            tx.success();
            return userExists;
        }
    }

    boolean createTvShow(final String title, final String releaseDate, final String endDate) {
        try ( Transaction tx = database.beginTx() ) {
            Node newTvShow = database.createNode(TvShow.TV_SHOW_LABEL);
            newTvShow.setProperty(TvShow.TITLE, title);
            newTvShow.setProperty(TvShow.RELEASE_DATE, releaseDate);
            if (endDate != null) {
                newTvShow.setProperty(TvShow.END_DATE, endDate);
            }
            tx.success();
            return true;
        }
    }

    boolean tvShowExists(final String title) {
        try ( Transaction tx = database.beginTx() ) {
            ResourceIterator<Node> nodes = database.findNodes(TvShow.TV_SHOW_LABEL, TvShow.TITLE, title);
            boolean tvShowExists = nodes.hasNext();
            nodes.close();
            tx.success();
            return tvShowExists;
        }
    }

    boolean likeTvShowByUser(final String mail,final  String title) throws IOException {
        try ( Transaction tx = database.beginTx() ) {
            ResourceIterator<Node> showNodes = database.findNodes(TvShow.TV_SHOW_LABEL, TvShow.TITLE, title);
            TvShow tvShow;
            if (showNodes.hasNext()) {
                tvShow = new TvShow(showNodes.next());
            } else {
                showNodes.close();
                tx.success();
                throw new IOException("No such movie in the database with title: " + title);
            }
            showNodes.close();

            ResourceIterator<Node> userNodes = database.findNodes(User.USER_LABEL, User.MAIL, mail);

            User user;
            if (userNodes.hasNext()) {
                user = new User(userNodes.next());
            } else {
                userNodes.close();
                tx.success();
                throw new IOException("No such user in the database with mail: " + mail);
            }
            userNodes.close();
            boolean addedLike = user.addTvShowLike(tvShow);
            tx.success();
            return addedLike;
        }
    }

    List<String> getUserLikedTvShows(final String userMail) throws IOException {
        try ( Transaction tx = database.beginTx() ) {
            ResourceIterator<Node> userNodes = database.findNodes(User.USER_LABEL, User.MAIL, userMail);

            User user;
            if (userNodes.hasNext()) {
                user = new User(userNodes.next());
            } else {
                userNodes.close();
                tx.success();
                throw new IOException("No such user in the database with mail: " + userMail);
            }
            userNodes.close();
            tx.success();
            return user.getLikedTvShows();
        }
    }

    List<String> getShowsAiredByDate(final String airedDate) {
        try ( Transaction tx = database.beginTx() ) {
            ResourceIterator<Node> showNodes = database.findNodes(TvShow.TV_SHOW_LABEL, TvShow.RELEASE_DATE, airedDate);
            List<String> showList = new ArrayList<>();
            while (showNodes.hasNext()) {
                showList.add(new TvShow(showNodes.next()).getName());
            }
            showNodes.close();
            tx.success();
            return showList;
        }
    }

    Map<String,Long> getShowRecommendationsForUser(final String mail) {
        String query = getRecommendationsCipherQuery(mail, 10);
        try ( Transaction tx = database.beginTx() ) {
            Result result = database.execute(query);

            Map<String,Long> map = new HashMap<>();
            while ( result.hasNext() )
            {
                Long likes = -1L;
                String title = "";
                Map<String,Object> row = result.next();
                for ( Entry<String,Object> column : row.entrySet() ) {
                    if (column.getKey().equals("showlikes")) {
                        likes = (Long) column.getValue();
                    } else {
                        title = (String) column.getValue();
                    }
                }
                if (likes == -1L || title.equals("")) {
                    continue;
                }
                map.put(title, likes);
            }
            tx.success();
            return map;
        }
    }

    Map<String,Long> getShowRecommendationsForUser(final String mail, final int ageDifference) {
        String query = getRecommendationCipherQueryForAge(mail, 10, ageDifference);
        try ( Transaction tx = database.beginTx() ) {
            Result result = database.execute(query);

            Map<String,Long> map = new HashMap<>();
            while ( result.hasNext() )
            {
                Long likes = -1L;
                String title = "";
                Map<String,Object> row = result.next();
                for ( Entry<String,Object> column : row.entrySet() ) {
                    if (column.getKey().equals("showlikes")) {
                        likes = (Long) column.getValue();
                    } else {
                        title = (String) column.getValue();
                    }
                }
                if (likes == -1L || title.equals("")) {
                    continue;
                }
                map.put(title, likes);
            }
            tx.success();
            return map;
        }
    }

    private String getRecommendationsCipherQuery(final String mail, final int limit) {
        return String.format("MATCH (user:PERSON {mail:\"%s\"})-[:LIKES]->(tvshows:TV_SHOW)<-[likes:LIKES]-(otherusers:PERSON),\n" +
                            "(otherusers)-[:LIKES]->(othershows)\n" +
                            "WHERE NOT (user)-[:LIKES]->(othershows)\n" +
                            "RETURN DISTINCT othershows.title, count(distinct likes) as showlikes\n" +
                            "ORDER BY showlikes DESC\n" +
                            "LIMIT %d", mail, limit);
    }

    private String getRecommendationCipherQueryForAge(final String mail, final int limit, final int ageDifference) {
        return String.format("MATCH (mainuser:PERSON {mail:\"%s\"}), (user:PERSON)-[likes:LIKES]->(tvshows:TV_SHOW)\n" +
                            "WHERE NOT (mainuser)-[:LIKES]->(tvshows)\n" +
                            "AND (TOINT(user.age) >=  TOINT(mainuser.age) - %d)\n" +
                            "AND (TOINT(user.age) <=  TOINT(mainuser.age) + %d)\n" +
                            "RETURN DISTINCT tvshows.title, count(distinct likes) as showlikes\n" +
                            "ORDER BY showlikes DESC\n" +
                            "LIMIT %d", mail, ageDifference, ageDifference, limit);
    }

}
