package com.neo4j.homework.unmanagedextension.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Database domain node for User.
 */
public class User {
    /** User mail property value. */
    public static final String MAIL = "mail";
    /** User age property value. */
    public static final String AGE = "age";
    /** User gender property value. */
    public static final String GENDER = "gender";
    /** User's min age. */
    public static final int MIN_AGE = 1;
    /** User's max age. */
    public static final int MAX_AGE = 100;
    /** User's node label. */
    public static final Label USER_LABEL = Label.PERSON;
    /** User's underlying database node. */
    private final Node underlyingNode;

    public User(Node userNode)
    {
        this.underlyingNode = userNode;
    }

    protected Node getUnderlyingNode()
    {
        return underlyingNode;
    }

    public String getMail()
    {
        return (String)underlyingNode.getProperty(MAIL);
    }

    public String getAge() {
        return (String)underlyingNode.getProperty(AGE);
    }

    public String getGender() {
        return (String)underlyingNode.getProperty(GENDER);
    }

    /**
     * Adds a TV show like relationship (if there isn't one already).
     */
    public boolean addTvShowLike(TvShow tvShow) {
        Relationship tvShowRelationShip = getTvShowRelationship(tvShow);
        if ( tvShowRelationShip == null )
        {
            //user starts to like tv-show
            underlyingNode.createRelationshipTo( tvShow.getUnderlyingNode(), RelationType.LIKES );
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets relationship to the TV Show. Returns null if user has no relationships(LIKES).
     */
    private Relationship getTvShowRelationship(TvShow tvShow) {
        Node tvShowNode = tvShow.getUnderlyingNode();
        for ( Relationship rel : underlyingNode.getRelationships( RelationType.LIKES ) )
        {
            if ( rel.getOtherNode( underlyingNode ).equals( tvShowNode ) )
            {
                //user already likes this tv show
                return rel;
            }
        }
        //user has no relations with this tv show
        return null;
    }

    /**
     * Gets a list of liked TV shows by user.
     */
    public List<String> getLikedTvShows() {
        List<String> movies = new ArrayList<>();
        for ( Relationship rel : underlyingNode.getRelationships( RelationType.LIKES, Direction.OUTGOING ) )
        {
            movies.add(new TvShow(rel.getOtherNode(underlyingNode)).getName());
        }
        return movies;
    }

    @Override
    public int hashCode()
    {
        return underlyingNode.hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        return o instanceof User &&
                underlyingNode.equals(((User) o).getUnderlyingNode());
    }

    @Override
    public String toString() {
        return "User[mail: " + getMail() + ", age: " + getAge() + ", gender: " + getGender() + "]";
    }
}
