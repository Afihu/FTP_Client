package com.ftpclient.ftp;

import java.io.IOException;
import java.util.List;

public class CLI {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java com.ftpclient.CLI <server> <port> <user> <pass>");
            System.exit(1);
        }
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        String user = args[2];
        String pass = args[3];

        try {
            System.out.printf("Connecting to %s:%d…%n", server, port);
            FTPCommands ftp = new FTPCommands(server, port);

            System.out.print("Logging in… ");
            if (!ftp.login(user, pass)) {
                System.err.println("failed");
                System.exit(2);
            }
            System.out.println("ok");

            System.out.println("Listing files:");
            List<String> files = ftp.list();
            files.forEach(f -> System.out.println("  " + f));

            // optional: download the first file if any
            if (!files.isEmpty()) {
                String remote = files.get(0).split("\\s+")[files.get(0).split("\\s+").length-1];
                String local = "downloaded_" + remote;
                System.out.printf("Downloading %s → %s…%n", remote, local);
                if (ftp.get(remote, local)) {
                    System.out.println("Download succeeded");
                } else {
                    System.err.println("Download failed");
                }
            }

            ftp.quit();
            System.out.println("Disconnected");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(99);
        }
    }
}