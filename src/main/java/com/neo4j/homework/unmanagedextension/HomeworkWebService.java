package com.neo4j.homework.unmanagedextension;

import java.io.IOException;
import java.lang.Exception;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.neo4j.homework.unmanagedextension.domain.TvShow;
import com.neo4j.homework.unmanagedextension.utils.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.neo4j.graphdb.GraphDatabaseService;

//extension root path
@Path("/neueda")
public class HomeworkWebService
{
    private final DomainService domainService;
    private final ObjectMapper objectMapper;

    public HomeworkWebService(@Context GraphDatabaseService database)
    {
        this.domainService = new DomainService(database);
        this.objectMapper = new ObjectMapper();
    }

    private Response createResponse(final boolean isOkResponse, final int httpCode, final String jsonMessage) {
        return createResponse(isOkResponse, httpCode, jsonMessage, null);
    }

    private Response createResponse(final boolean isOkResponse, final int httpCode, final String jsonMessage,
                                    final ObjectNode messageNode) {
        ObjectNode node;
        if (messageNode != null) {
            node = JsonUtils.createResponseNode(isOkResponse, httpCode, messageNode, objectMapper.createObjectNode());
        } else {
            node = JsonUtils.createResponseNode(isOkResponse, httpCode, jsonMessage, objectMapper.createObjectNode());
        }

        try {
            return Response.ok().entity(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)).build();
        } catch (IOException e1) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Server error: " + e1.getMessage()).build();
        }
    }

    /**
     * Creates user only if there is not mail conflict in the database and JSON parsing is successful (is correct).
     */
    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/user/add")
    public Response addUserToDatabase(final String userJsonData) {
        try {
            JsonNode parentNode = JsonUtils.getAndCheckJsonNode(userJsonData, 3);

            String mail = JsonUtils.getUserMail(parentNode);
            String gender = JsonUtils.validateGender(parentNode);
            String age = JsonUtils.validateAge(parentNode);

            if (domainService.userExists(mail)) {
                String error = "User with mail [%s] (mail must be unique) is already in the database";
                throw new IOException(String.format(error, mail));
            } else {
                domainService.createUser(mail, age, gender);
            }
            return createResponse(true, 200, "User created");
        } catch (Exception e) {
            return createResponse(false, 200, e.getMessage());
        }
    }

    /**
     * Adds TV show to the database only if one with the same does not exist and JSON string is parsed (is correct).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/tvshow/add")
    public Response addTvShowToDatabase(final String tvShowJsonData) {
        try {
            JsonNode parentNode = JsonUtils.getAndCheckJsonNode(tvShowJsonData, 3);
            String title = JsonUtils.getTvShowTitle(parentNode);
            String releaseDate = JsonUtils.validateTvShowDate(parentNode, TvShow.RELEASE_DATE);
            String endDate = JsonUtils.validateTvShowDate(parentNode, TvShow.END_DATE);
            JsonUtils.validateEndDateAfterRelease(releaseDate, endDate);

            if (domainService.tvShowExists(title)) {
                String error = "TV Show with title [%s] (title must be unique) is already in the database";
                throw new IOException(String.format(error, title));
            } else {
                domainService.createTvShow(title, releaseDate, endDate);
            }
            return createResponse(true, 200, "TV show added");
        } catch (Exception e) {
            return createResponse(false, 200, e.getMessage());
        }
    }

    /**
     * Gets TV Shows liked by a user only if JSON is parsed (is correct) and user exists.
     */
    @GET
    @Produces( MediaType.TEXT_PLAIN ) //RESPONSE ENCODING
    @Path("/user/{userMail}/getlikes")
    public Response findUserLikedTvShows( final @PathParam("userMail") String userMail )
    {
        try {
            if(!JsonUtils.validateUserMail(userMail)) {
                throw new IOException("Wrong e-mail: " + userMail);
            }
            List<String> movies = domainService.getUserLikedTvShows(userMail);
            ObjectNode ob = objectMapper.createObjectNode();
            ArrayNode an = objectMapper.createArrayNode();
            for (String tvShow : movies ) {
                an.add(tvShow);
            }
            ob.put("tvshows", an);
            return createResponse(true, 200, null, ob);
        } catch (IOException e) {
            return createResponse(false, 200, e.getMessage());
        }
    }

    /**
     * Adds a TV show like by User to the database only if JSON is parsed (is correct) and TV show exists
     * and User exists.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/user/liketvshow")
    public Response likeTvShowByUser( final String jsonDate )
    {
        try {
            JsonNode parentNode = JsonUtils.getAndCheckJsonNode(jsonDate, 2);
            String mail = JsonUtils.getUserMail(parentNode);
            String title = JsonUtils.getTvShowTitle(parentNode);

            if(domainService.likeTvShowByUser(mail, title)) {
                //added like
                return createResponse(true, 200, "User now likes this TV Show");
            } else {
                //did not add, because already liked
                return createResponse(false, 200, "Already liked");
            }
        } catch (Exception e) {
            return createResponse(false, 200, e.getMessage());
        }
    }

    /**
     * Gets TV Shows aired on a specific date.
     */
    @GET
    @Produces( MediaType.TEXT_PLAIN ) //RESPONSE ENCODING
    @Path("/tvshow/aired/{airDate}")
    public Response findTvShowsByAirDate( final @PathParam("airDate") String airDate )
    {
        try {
            Date airDateObject = JsonUtils.isDateValid(airDate);
            String airDateApiString = JsonUtils.toApiStringFormat(airDateObject);
            List<String> tvShows = domainService.getShowsAiredByDate(airDateApiString);
            ObjectNode ob = objectMapper.createObjectNode();
            ArrayNode an = objectMapper.createArrayNode();
            for (String tvShow : tvShows ) {
                an.add(tvShow);
            }
            ob.put("tvshows", an);
            return createResponse(true, 200, null, ob);
        } catch (Exception e) {
            return createResponse(false, 200, e.getMessage());
        }
    }

    /**
     * Gets TV Shows recommendations for a user.
     */
    @GET
    @Produces( MediaType.TEXT_PLAIN ) //RESPONSE ENCODING
    @Path("/user/{userMail}/recommendations")
    public Response findRecommendedTvShowsForUser(final @PathParam("userMail") String userMail) {
        try {
            if(!JsonUtils.validateUserMail(userMail)) {
                throw new IOException("Wrong e-mail: " + userMail);
            }
            if (!domainService.userExists(userMail)) {
                throw new IOException("No such user in the database with mail: " + userMail);
            }

            Map<String,Long> shows = domainService.getShowRecommendationsForUser(userMail);
            int ageDifference = 0;
            boolean isAgeRecommendation = false;
            while (shows.size() == 0) {
                isAgeRecommendation = true;
                ageDifference = ageDifference + 2;
                if (ageDifference == 50) {
                    break;
                }
                shows = domainService.getShowRecommendationsForUser(userMail, ageDifference);
            }

            ObjectNode ob = objectMapper.createObjectNode();
            ArrayNode an = objectMapper.createArrayNode();
            for (Map.Entry<String, Long> tvShow : shows.entrySet() ) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("show", tvShow.getKey());
                node.put("likes", tvShow.getValue());
                an.add(node);
            }
            if (isAgeRecommendation) {
                ob.put(String.format("tvshow recommendations by age difference [%d]", ageDifference), an);
            } else {
                ob.put("tvshow recommendations by users that liked user's shows", an);
            }
            return createResponse(true, 200, null, ob);
        } catch (Exception e) {
            return createResponse(false, 200, e.getMessage());
        }
    }
}