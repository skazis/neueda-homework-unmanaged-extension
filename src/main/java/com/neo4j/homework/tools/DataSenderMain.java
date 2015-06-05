package com.neo4j.homework.tools;

import java.io.IOException;

/**
 * Main class of the tool.
 */
public class DataSenderMain {
    /** Properties object.*/
    private SenderProperties properties;

    /**
     * Main method.
     */
    public static void main(String ... args) {
        try {
            DataSenderMain main = new DataSenderMain();

            if (args.length == 0 || (args.length == 1 && args[0].equals("-h"))) {
                main.printHelp();
            } else if (!(args.length == 4 || args.length == 5)) {
                System.out.println("Wrong argument length: " + args.length);
                main.printHelp();
            } else if (args[0].equals("-p")) {
                main.readPropertiesFile(args[1]);
                main.parseArgumentsAndSendRequest(args);
            } else {
                System.out.println("Wrong arguments.");
                main.printHelp();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("Main method ends.");
    }

    private void readPropertiesFile(final String propertiesFile) throws IOException {
        properties = new SenderProperties();
        properties.loadProperties(propertiesFile);
    }

    private void parseArgumentsAndSendRequest(String ... args) throws IOException {
        HttpSender sender = new HttpSender(properties);

        switch (args[2]) {
            case "-ishow":
                String pathToShowCsv = args[3];
                sender.importShows(pathToShowCsv);
                break;
            case "-iuser": {
                String pathToUserCsv = args[3];
                sender.importUsers(pathToUserCsv);
                break;
            }
            case "-irelation": {
                String pathToUserCsv = args[3];
                sender.importUserTvShowRelationShips(pathToUserCsv);
                break;
            }
            case "-post":
                if (args.length != 5) {
                    throw new IOException("Missing last parameter (JSON string) for POST method");
                }
                sender.sendPost(args[3], args[4]);
                break;
            case "-get":
                sender.sendGet(args[3]);
                break;
            default:
                System.out.println("Wrong arguments. Read help.");
                printHelp();
                break;
        }
    }


    private void printHelp() {
        System.out.println("Utility help: ");
        System.out.println("\tArgument: -h");
        System.out.println("\t\tHelp text (this)");
        System.out.println();
        System.out.println("\tArgument: -p");
        System.out.println("\t\tArgument value: path to the properties file \"csvsender.properties\"");
        System.out.println();
        System.out.println("\t\tArgument \"-p\" added with one of the arguments below makes request");
        System.out.println();
        System.out.println("\tArgument: -ishow");
        System.out.println("\t\tArgument value: path to the csv file with header");
        System.out.println("\t\tCSV file must be semicolon separated");
        System.out.println("\t\tCSV file      : title;aired;ended");
        System.out.println("\t\t\ttitle       : 1 to 50 alphanumeric string");
        System.out.println("\t\t\taired       : dd-MM-yyyy format string");
        System.out.println("\t\t\tended       : dd-MM-yyyy format string OR N/A if show's not done");
        System.out.println("\tArgument: -iuser");
        System.out.println("\t\tArgument value: path to the csv file with header");
        System.out.println("\t\tCSV file must be semicolon separated");
        System.out.println("\t\tCSV file      : email;age;gender");
        System.out.println("\t\t\temail       : alphanumeric@alpha.alpha");
        System.out.println("\t\t\tage         : from 1 to 100 (both inclusive)");
        System.out.println("\t\t\tgender      : female or male (shorted alternatives are f or m)");
        System.out.println("\tArgument: -irelation");
        System.out.println("\t\tArgument value: path to the csv file with header");
        System.out.println("\t\tCSV file must be semicolon separated");
        System.out.println("\t\tCSV file      : email;title");
        System.out.println("\t\t\temail       : alphanumeric@alpha.alpha (user must exist in the db)");
        System.out.println("\t\t\ttitle       : 1 to 50 alphanumeric string (show must exist in the db)");
        System.out.println("\tArgument: -post");
        System.out.println("\t\tArgument value: API string (space) and JSON request string");
        System.out.println("\tArgument: -get");
        System.out.println("\t\tArgument value: API string with filled path paramaters");
    }
}
