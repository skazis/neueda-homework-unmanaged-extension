package com.neo4j.homework.tools;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP POST and GET method sender and CSV file parser and sender.
 */
public class HttpSender {
    private SenderProperties properties;

    public HttpSender(SenderProperties properties) {
        this.properties = properties;
    }

    private HttpClient createHttpClient() {
        if (properties.isServerAuth()) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, "basic"),
                    new UsernamePasswordCredentials(properties.getUserName(), properties.getUserPass()));
            return HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        } else {
            return HttpClients.custom().build();
        }
    }

    private HttpHost getHttpHost() {
        return new HttpHost(properties.getServerAddress(), properties.getServerPort());
    }

    private String getFullUrl(final String apiUrl) {
        return String.format("http://%s:%d%s%s",properties.getServerAddress(), properties.getServerPort(),
                                                properties.getUnmanagedExtensionsBasePath(), apiUrl);
    }

    private HttpClientContext getHttpContext(final HttpHost httpHost) {
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(httpHost, basicAuth);
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
        return localContext;
    }

    /**
     * Sends HTTP POST request. Parameter URL is API url (not full URL).
     */
    void sendPost(final String url, final String json) throws IOException {
        HttpClient client = createHttpClient();
        HttpHost httpHost = getHttpHost();

        HttpPost post = new HttpPost(getFullUrl(url));
        post.setHeader("User-Agent", properties.getUserName());
        post.setHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(json));

        System.out.println("\nSending 'POST' request to URL : " + post.getURI().toString());
        System.out.println("Request line: " + post.getRequestLine());
        System.out.println("JSON string: " + json);

        HttpResponse response = client.execute(httpHost, post, getHttpContext(httpHost));

        System.out.println("HTTP Response Code : " + response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        System.out.println("Message body:");
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        System.out.println(result.toString());

    }

    /**
     * Sends HTTP GET request. Parameter URL is API url (not full URL).
     */
    void sendGet(String url) throws IOException {
        HttpClient client = createHttpClient();

        HttpHost httpHost = getHttpHost();
        HttpGet request = new HttpGet(getFullUrl(url));
        request.addHeader("User-Agent", properties.getUserName());

        System.out.println("\nSending 'GET' request to URL : " + request.getURI().toString());
        System.out.println("Executing request " + request.getRequestLine());

        HttpResponse response = client.execute(httpHost, request, getHttpContext(httpHost));

        System.out.println("HTTP Response Code : " + response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        System.out.println("Message body:");
        StringBuilder result = new StringBuilder();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        System.out.println(result.toString());
    }

    private static class TvShow {
        private String title;
        private String aired;
        private String end;

        private TvShow(String title, String aired, String end) {
            this.title = title;
            this.aired = aired;
            this.end = (end == null || end.equals("N/A"))? "" : end;
        }

        @Override
        public String toString() {
            return "Show [title=" + title + " , air=" + aired + " , end=" + end + "]";
        }

        public String toJson() {
            String json;
            if (end == null || end.isEmpty()) {
                json = String.format("{\"title\":\"%s\", \"releaseDate\":\"%s\", \"endDate\":\"\"}", title, aired);
            } else {
                json = String.format("{\"title\":\"%s\", \"releaseDate\":\"%s\", \"endDate\":\"%s\"}", title, aired, end);
            }

            return json;
        }
    }

    private static class User {
        private String mail;
        private String age;
        private String gender;

        private User(String mail, String age, String gender) {
            this.mail = mail;
            this.age = age;
            this.gender = gender;
        }

        @Override
        public String toString() {
            return "User [mail=" + mail + " , age=" + age + " , gender=" + gender + "]";
        }

        public String toJson() {
            return String.format("{\"mail\":\"%s\", \"age\":\"%s\", \"gender\":\"%s\"}", mail, age, gender);
        }
    }

    private static class Relation {
        private String mail;
        private String title;

        private Relation(String mail, String title) {
            this.mail = mail;
            this.title = title;
        }

        @Override
        public String toString() {
            return "Like relation [mail=" + mail + " , title=" + title + "]";
        }

        public String toJson() {
            return String.format("{\"mail\":\"%s\", \"title\":\"%s\"}", mail, title);
        }
    }

    /**
     * Parses CSV file and sends POST requests to add TV Shows.
     */
    void importShows(final String csvFile) throws IOException {

        BufferedReader br = null;
        String line;
        String cvsSplitBy = ";";
        List<TvShow> showList = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(csvFile));
            boolean isHeader = true;
            int linesRead = 0;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] tvShow = line.split(cvsSplitBy);
                if (tvShow.length == 2) {
                    showList.add(new TvShow(tvShow[0], tvShow[1], null));
                } else {
                    showList.add(new TvShow(tvShow[0], tvShow[1], tvShow[2]));
                }
                linesRead++;
            }
            for (TvShow show : showList) {
                System.out.println(show.toString());
            }

            System.out.println("TV Shows parsed: " + linesRead);

        } finally {
            if (br != null) {
                br.close();
            }
        }

        for (TvShow show : showList) {
            try {
                sendPost("/neueda/tvshow/add", show.toJson());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error while sending tv show: " + show.toString());
            }
        }
    }

    /**
     * Parses CSV file and sends POST requests to add Users.
     */
    void importUsers(String csvFile) throws IOException {
        BufferedReader br = null;
        String line;
        String cvsSplitBy = ";";
        List<User> userList = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(csvFile));
            boolean isHeader = true;
            int linesRead = 0;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] user = line.split(cvsSplitBy);
                userList.add(new User(user[0], user[1], user[2]));
                linesRead++;
            }
            for (User user : userList) {
                System.out.println(user.toString());
            }
            System.out.println("Users parsed: " + linesRead);
        } finally {
            if (br != null) {
                br.close();
            }
        }

        for (User user : userList) {
            try {
                sendPost("/neueda/user/add", user.toJson());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error while sending User: " + user.toString());
            }
        }
    }

    /**
     * Parses CSV file and sends POST requests to add User->LIKES->TV Show relationship.
     */
    void importUserTvShowRelationShips(String csvFile) throws IOException {
        BufferedReader br = null;
        String line;
        String cvsSplitBy = ";";
        List<Relation> relationList = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(csvFile));
            boolean isHeader = true;
            int linesRead = 0;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] user = line.split(cvsSplitBy);
                relationList.add(new Relation(user[0], user[1]));
                linesRead++;
            }
            for (Relation show : relationList) {
                System.out.println(show.toString());
            }
            System.out.println("Like relations parsed: " + linesRead);


        } finally {
            if (br != null) {
                br.close();
            }
        }

        for (Relation relation : relationList) {
            try {
                sendPost("/neueda/user/liketvshow", relation.toJson());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error while sending relationship: " + relation.toString());
            }
        }
    }
}
