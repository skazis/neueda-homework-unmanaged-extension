package com.neo4j.homework.unmanagedextension.domain;

import org.neo4j.graphdb.Node;

/**
 * Database domain node for TV show.
 */
public class TvShow {
    /** TV show title property value. */
    public static final String TITLE = "title";
    /** TV show air date property value. */
    public static final String RELEASE_DATE = "releaseDate";
    /** TV show end date property value. */
    public static final String END_DATE = "endDate";
    /** TV show title length. */
    public static final int TITLE_LENGTH = 50;
    /** TV show date format. */
    public final static String DATE_FORMAT = "dd-MM-yyyy";
    /** TV show node label. */
    public static final Label TV_SHOW_LABEL = Label.TV_SHOW;
    /** TV shows's underlying database node. */
    private final Node underlyingNode;

    public TvShow(Node tvShowNode) {
        this.underlyingNode = tvShowNode;
    }

    protected Node getUnderlyingNode() {
        return underlyingNode;
    }

    public String getName() {
        return (String)underlyingNode.getProperty(TITLE);
    }

    public String getReleaseDate() {
        return (String)underlyingNode.getProperty(RELEASE_DATE);
    }

    public String getEndDate() {
        return (String)underlyingNode.getProperty(END_DATE);
    }

    @Override
    public int hashCode() {
        return underlyingNode.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        return o instanceof TvShow &&
                underlyingNode.equals(((TvShow) o).getUnderlyingNode());
    }

    @Override
    public String toString() {
        return "TvShow[name: " + getName() + ", release date: " + getReleaseDate()
                + ", end date: " + getEndDate() + "]";
    }
}
