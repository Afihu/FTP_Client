
/*
 * FTP Client Command Line Interface (Legacy)
 * This was created as a simple testing method for the FTPCommands class.s
*/

package com.ftpclient.ftp;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class CLI {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            // 1) Connection setup
            System.out.print("Host: ");
            String host = sc.nextLine().trim();
            System.out.print("Port: ");
            int port = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Username: ");
            String user = sc.nextLine().trim();
            System.out.print("Password: ");
            String pass = sc.nextLine().trim();

            FTPCommands ftp = new FTPCommands(host, port);
            System.out.print("Logging in… ");
            if (!ftp.login(user, pass)) {
                System.err.println("failed");
                ftp.quit();
                return;
            }
            System.out.println("ok");
            System.out.println("Connected. Type 'help' for commands, 'quit' to exit.");

            // 2) Interactive command loop
            while (true) {
                System.out.print("ftp> ");
                String line = sc.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();

                if ("quit".equals(cmd) || "exit".equals(cmd)) {
                    break;
                }
                switch (cmd) {
                    case "help":
                        System.out.println("Commands:");
                        System.out.println("  list");
                        System.out.println("  get <remote> [<local>]");
                        System.out.println("  put <local> [<remote>]");
                        System.out.println("  quit");
                        break;

                    case "list":
                        try {
                            List<String> files = ftp.list();
                            System.out.println("Directory listing:");
                            files.forEach(f -> System.out.println("  " + f));
                        } catch (IOException ex) {
                            System.err.println("Error listing: " + ex.getMessage());
                        }
                        break;

                    case "get":
                        if (parts.length < 2) {
                            System.err.println("Usage: get <remote> [<local>]");
                            break;
                        }
                        String remote = parts[1];
                        String local = (parts.length >= 3 ? parts[2] : remote);
                        System.out.printf("Downloading %s → %s… ", remote, local);
                        try {
                            boolean ok = ftp.get(remote, local);
                            System.out.println(ok ? "succeeded" : "failed");
                        } catch (IOException ex) {
                            System.err.println("failed: " + ex.getMessage());
                        }
                        break;

                    case "put":
                        if (parts.length < 2) {
                            System.err.println("Usage: put <local> [<remote>]");
                            break;
                        }
                        String localFile = parts[1];
                        String remoteName = (parts.length >= 3 ? parts[2] : localFile);
                        System.out.printf("Uploading %s → %s… ", localFile, remoteName);
                        try {
                            boolean ok = ftp.put(localFile, remoteName);
                            System.out.println(ok ? "succeeded" : "failed");
                        } catch (IOException ex) {
                            System.err.println("failed: " + ex.getMessage());
                        }
                        break;
                    case "cd":
                    case "cwd":
                        if (parts.length < 2) {
                            System.err.println("Usage: cd <directory>");
                        } else {
                            try {
                                boolean ok = ftp.cwd(parts[1]);
                                System.out.println(ok
                                    ? "Directory changed to “" + parts[1] + "”"
                                    : "Failed to change directory");
                            } catch (IOException ex) {
                                System.err.println("Error: " + ex.getMessage());
                            }
                        }
                        break;

                    case "mkd":
                    case "mkdir":
                        if (parts.length < 2) {
                            System.err.println("Usage: mkd <directory>");
                        } else {
                            try {
                                boolean ok = ftp.mkd(parts[1]);
                                System.out.println(ok
                                    ? "Directory created: " + parts[1]
                                    : "Failed to create directory");
                            } catch (IOException ex) {
                                System.err.println("Error: " + ex.getMessage());
                            }
                        }
                        break;

                    default:
                        System.err.println("Unknown command: " + cmd);
                }
            }

            // 3) Clean up
            ftp.quit();
            System.out.println("Disconnected.");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            sc.close();
        }
    }
}