package com.neo4j.homework.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Properties class. Loads sender properties.
 */
public class SenderProperties {
    /** Is server using authentication. */
    private boolean isServerAuth;
    /** Server user name. */
    private String userName;
    /** Server password. */
    private String userPass;
    /** Base path of the unmanaged extension. */
    private String unmanagedExtensionsBasePath;
    /** Server address.*/
    private String serverAddress;
    /** Server port.*/
    private int serverPort;

    void loadProperties(final String propertiesPath) throws IOException {
        InputStream in = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        in.close();

        String prop = properties.getProperty("is_server_auth");
        isServerAuth = Boolean.valueOf(prop);

        if (isServerAuth) {
            userName = properties.getProperty("server_user_name");
            userPass = properties.getProperty("server_user_password");
        }

        unmanagedExtensionsBasePath = properties.getProperty("unmanaged_extensions_base_path");
        serverAddress = properties.getProperty("server_address");
        serverPort = Integer.parseInt(properties.getProperty("server_port"));
    }

    public boolean isServerAuth() {
        return isServerAuth;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPass() {
        return userPass;
    }

    public String getUnmanagedExtensionsBasePath() {
        return unmanagedExtensionsBasePath;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }
}
