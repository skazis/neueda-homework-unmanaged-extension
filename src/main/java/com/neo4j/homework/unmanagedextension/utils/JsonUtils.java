package com.neo4j.homework.unmanagedextension.utils;

import com.neo4j.homework.unmanagedextension.domain.Gender;
import com.neo4j.homework.unmanagedextension.domain.TvShow;
import com.neo4j.homework.unmanagedextension.domain.User;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing and validating JSON data and objects, also for any other data.
 */
public class JsonUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Private constructor. Only static methods for this class.
     */
    private JsonUtils() {}

    /**
     * Validates JSON node and returns user email if it is valid.
     */
    public static String getUserMail(final JsonNode node) throws IOException {
        String email = validateJsonNode(node, User.MAIL).asText();
        if (!validateUserMail(email)) {
            throw new IOException(String.format("Wrong e-mail: %s", email));
        }
        return email;
    }

    /**
     * Looks for JSON node within parent node. Throws exception if it is missing or it is a container.
     */
    public static JsonNode validateJsonNode(final JsonNode parentNode, final String nodeName) throws IOException {
        JsonNode subNode = parentNode.get(nodeName);

        if (subNode == null) {
            throw new IOException(String.format("JSON node [%s] is missing", nodeName));
        } else if (subNode.isContainerNode()) {
            throw new IOException(String.format("JSON node [%s] cannot be container", nodeName));
        }

        return subNode;
    }

    /**
     * Validates user's email. alphanumeric{n}@alphanumeric{n}.alpha{2,n}
     */
    public static boolean validateUserMail(final String email) throws IOException {
        Pattern p = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
        Matcher m = p.matcher(email);

        return m.matches();
    }

    /**
     * Validates JSON node and gender. Gender can be male, m, female or f.
     */
    public static String validateGender(final JsonNode node) throws IOException {
        String gender = validateJsonNode(node, User.GENDER).asText();
        return Gender.getLongForm(gender);
    }

    /**
     * Validates JSON node and user's age. Must be between 1 and 100.
     */
    public static String validateAge(final JsonNode node) throws IOException {
        String age = validateJsonNode(node, User.AGE).asText();
        validateUserAge(age);
        return age;
    }

    /**
     * Validates user's age. Must be between 1 and 100.
     */
    public static void validateUserAge(final String age) throws IOException {
        if (age == null) {
            throw new IOException("User's age is not provided.");
        } else {
            try {
                final int userAge = Integer.parseInt(age);

                if (userAge < User.MIN_AGE || userAge > User.MAX_AGE) {
                    throw new IOException(String.format("User's age must be between %d and %d",
                                                        User.MIN_AGE, User.MAX_AGE));
                }
            } catch (Exception e ) {
                throw new IOException(String.format("Wrong user's age [%s]: %s ", age, e.getMessage()));
            }
        }
    }

    /**
     * Creates response node with three sub-nodes.
     */
    public static ObjectNode createResponseNode(final boolean statusOk,final int status,final String message,
                                                final ObjectNode node) {
        node.put("statusOk", statusOk);
        node.put("statusCode", status);
        node.put("message", message);
        return node;
    }

    /**
     * Creates response node with three sub-nodes.
     */
    public static ObjectNode createResponseNode(final boolean statusOk,final  int status,final ObjectNode message,
                                                final ObjectNode rootNode) {
        rootNode.put("statusOk", statusOk);
        rootNode.put("statusCode", status);
        rootNode.put("message", message);
        return rootNode;
    }

    /**
     * Gets root JSON node from JSON string and sees if it with expected size.
     */
    public static JsonNode getAndCheckJsonNode(final String jsonContext,
                                               final int expectedNodeSize) throws IOException {
        if (jsonContext == null || jsonContext.trim().isEmpty()) {
            throw new IOException("Received empty JSON context");
        }

        JsonNode node = objectMapper.readTree(jsonContext);

        if (node.size() != expectedNodeSize) {
            String stringToFormat = "Received JSON context with wrong node size value [%d], value [%d] is expected";
            throw new IOException(String.format(stringToFormat, node.size(), expectedNodeSize));
        }

        return node;
    }

    /**
     * Validates JSON node, gets TV Show title, validates it and returns it.
     */
    public static String getTvShowTitle(final JsonNode node) throws IOException {
        JsonNode subNode = validateJsonNode(node, TvShow.TITLE);
        String title = subNode.asText();
        validateMovieTitle(title);
        return title;
    }

    /**
     * Validates TV show title. Cannot be empty or longer than 50.
     */
    public static void validateMovieTitle(final String title) throws IOException {
        if (title == null || title.isEmpty()) {
            throw new IOException("TV Show has empty title");
        } else {
            if (title.length() > TvShow.TITLE_LENGTH) {
                String toFormat = "TV Show has too long [max %d characters, but is %d] title: %s";
                throw new IOException(String.format(toFormat, TvShow.TITLE_LENGTH, title.length(), title));
            }
        }
    }

    /**
     * Validates JSON node, TV show date and returns date. Does not return date for <code>TvShow.END_DATE</code>.
     */
    public static String validateTvShowDate(final JsonNode parentNode,final  String dbPropertyName) throws IOException {
        JsonNode subNode = null;
        try {
            subNode = validateJsonNode(parentNode, dbPropertyName);
            if (dbPropertyName.equals(TvShow.END_DATE)) {
                if (subNode != null && !subNode.asText().isEmpty()) {
                    return toApiStringFormat(isDateValid(subNode.asText()));
                } else {
                    return null;
                }
            } else {
                Date currentDate = new Date();
                Date airDate = isDateValid(subNode.asText());
                if (airDate.after(currentDate)) {
                    throw new IOException("TV Show has its air date [" + toApiStringFormat(airDate)
                            + "] after current date [" + toApiStringFormat(currentDate) + "]");
                }
                return toApiStringFormat(airDate);
            }
        } catch (ParseException e) {
            throw new IOException("TV Show has node " + dbPropertyName + " with illegal value: " + subNode.asText()
                                + ". Allowed format: " + TvShow.DATE_FORMAT);
        }
    }

    /**
     * If there is end date, validates that it is not the same as air date. That it is not before air date.
     * And that it is not after current date.
     */
    public static void validateEndDateAfterRelease(final String releaseDateString,
                                                   final String endDateString) throws IOException, ParseException {
        if (endDateString != null) {
            Date endDate = isDateValid(endDateString);
            Date releaseDate = isDateValid(releaseDateString);

            if (releaseDate.compareTo(endDate) == 0) {
                throw new IOException("TV Show has its end date [" + toApiStringFormat(endDate)
                        + "] equal to start date [" + toApiStringFormat(releaseDate) + "]");
            }

            if (releaseDate.after(endDate)) {
                throw new IOException("TV Show has its end date [" + toApiStringFormat(endDate)
                        + "] before start date [" + toApiStringFormat(releaseDate) + "]");
            }

            Date currentDate = new Date();
            if (endDate.after(currentDate)) {
                throw new IOException("TV Show has its end date [" + toApiStringFormat(endDate)
                        + "] after current date [" + toApiStringFormat(currentDate) + "]");
            }
        }
    }

    /**
     * Returns Date object as API string.
     */
    public static String toApiStringFormat(final Date date) {
        return new SimpleDateFormat(TvShow.DATE_FORMAT).format(date);
    }

    /**
     * Validate date according to the API format.
     */
    public static Date isDateValid(String date) throws ParseException {
        DateFormat df = new SimpleDateFormat(TvShow.DATE_FORMAT);
        df.setLenient(false);
        return df.parse(date);
    }
}
