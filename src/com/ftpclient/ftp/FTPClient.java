package com.ftpclient.ftp;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import com.ftpclient.ftp.FTPResponse;

public class FTPClient {
    private Socket commandSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String server;
    private int port;
    private String username;
    private String password;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;

    /** Default constructor (uses port 21). */
    public FTPClient() {
        this.port = 21;
    }

    /** Constructor with server and port. */
    public FTPClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /** Connect to the FTP server (sets server & port). */
    public boolean connect(String server, int port) throws IOException {
        this.server = server;
        this.port = port;
        return connect();
    }

    /** Connect using previously set server & port. */
    public boolean connect() throws IOException {
        if (isConnected) {
            return true;
        }
        try {
            commandSocket = new Socket(server, port);
            reader = new BufferedReader(
                new InputStreamReader(commandSocket.getInputStream()));
            writer = new BufferedWriter(
                new OutputStreamWriter(commandSocket.getOutputStream()));

            FTPResponse welcome = readResponse();
            if (welcome.getCode() != 220) {
                throw new IOException("Unable to connect. Response: " + welcome);
            }

            isConnected = true;
            return true;
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }

    /** Login to the FTP server. */
    public boolean login(String username, String password) throws IOException {
        if (!isConnected) {
            throw new IOException("Not connected to FTP server");
        }
        this.username = username;
        this.password = password;

        sendCommand("USER " + username);
        FTPResponse userResp = readResponse();
        if (userResp.getCode() == 331) {
            sendCommand("PASS " + password);
            FTPResponse passResp = readResponse();
            if (passResp.getCode() == 230) {
                isLoggedIn = true;
                return true;
            }
        }
        return false;
    }

    /** List files in the current directory. */
    public List<String> listFiles() throws IOException {
        if (!isLoggedIn) {
            throw new IOException("Not logged in");
        }

        sendCommand("PASV");
        FTPResponse pasv = readResponse();
        if (pasv.getCode() != 227) {
            throw new IOException("Could not enter passive mode. Response: " + pasv);
        }
        Socket dataSocket = createDataSocket(pasv);

        sendCommand("LIST");
        FTPResponse listStart = readResponse();
        if (!listStart.isPositivePreliminary()) {
            dataSocket.close();
            throw new IOException(
                "Could not start directory listing. Response: " + listStart);
        }

        BufferedReader dataReader = new BufferedReader(
            new InputStreamReader(dataSocket.getInputStream()));
        List<String> fileList = new ArrayList<>();
        String line;
        while ((line = dataReader.readLine()) != null) {
            fileList.add(line);
        }
        dataReader.close();
        dataSocket.close();

        FTPResponse listEnd = readResponse();
        if (!listEnd.isPositiveCompletion()) {
            throw new IOException("Error completing listing. Response: " + listEnd);
        }
        return fileList;
    }

    /** Change working directory. */
    public boolean changeDirectory(String directory) throws IOException {
        if (!isLoggedIn) {
            throw new IOException("Not logged in");
        }
        sendCommand("CWD " + directory);
        FTPResponse resp = readResponse();
        return resp.getCode() == 250;
    }

    /** Download a file from the server. */
    public boolean downloadFile(String remoteFile, String localFile) throws IOException {
        if (!isLoggedIn) {
            throw new IOException("Not logged in");
        }

        sendCommand("TYPE I");
        FTPResponse typeResp = readResponse();
        if (typeResp.getCode() != 200) {
            throw new IOException("Could not set binary mode. Response: " + typeResp);
        }

        sendCommand("PASV");
        FTPResponse pasv = readResponse();
        if (pasv.getCode() != 227) {
            throw new IOException("Could not enter passive mode. Response: " + pasv);
        }
        Socket dataSocket = createDataSocket(pasv);

        sendCommand("RETR " + remoteFile);
        FTPResponse retr = readResponse();
        if (!retr.isPositivePreliminary()) {
            dataSocket.close();
            throw new IOException("Could not start download. Response: " + retr);
        }

        try (BufferedInputStream dataIn = new BufferedInputStream(dataSocket.getInputStream());
             BufferedOutputStream fileOut = new BufferedOutputStream(
                 new FileOutputStream(localFile))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
            fileOut.flush();
        } finally {
            dataSocket.close();
        }

        FTPResponse done = readResponse();
        return done.isPositiveCompletion();
    }

    /** Upload a file to the server. */
    public boolean uploadFile(String localFile, String remoteFile) throws IOException {
        if (!isLoggedIn) {
            throw new IOException("Not logged in");
        }

        sendCommand("TYPE I");
        FTPResponse typeResp = readResponse();
        if (typeResp.getCode() != 200) {
            throw new IOException("Could not set binary mode. Response: " + typeResp);
        }

        sendCommand("PASV");
        FTPResponse pasv = readResponse();
        if (pasv.getCode() != 227) {
            throw new IOException("Could not enter passive mode. Response: " + pasv);
        }
        Socket dataSocket = createDataSocket(pasv);

        sendCommand("STOR " + remoteFile);
        FTPResponse stor = readResponse();
        if (!stor.isPositivePreliminary()) {
            dataSocket.close();
            throw new IOException("Could not start upload. Response: " + stor);
        }

        try (BufferedInputStream fileIn = new BufferedInputStream(
                 new FileInputStream(localFile));
             BufferedOutputStream dataOut = new BufferedOutputStream(
                 dataSocket.getOutputStream())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
            dataOut.flush();
        } finally {
            dataSocket.close();
        }

        FTPResponse done = readResponse();
        return done.getCode() == 226;
    }

    /** Disconnect from the FTP server. */
    public void disconnect() {
        if (isConnected) {
            try {
                sendCommand("QUIT");  // politely close
                // ignore response
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (commandSocket != null) commandSocket.close();
            } catch (IOException e) {
                // ignore
            } finally {
                isConnected = false;
                isLoggedIn = false;
            }
        }
    }

    /** Send a command on the control connection. */
    private void sendCommand(String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
    }

    /** Read & parse an FTP response (handles multi-line). */
    private FTPResponse readResponse() throws IOException {
        String line = reader.readLine();
        StringBuilder raw = new StringBuilder(line);
        while (line.length() > 3 && line.charAt(3) == '-') {
            line = reader.readLine();
            raw.append("\r\n").append(line);
        }
        return FTPResponse.parse(raw.toString());
    }

    /** Create a data socket from a PASV response. */
    private Socket createDataSocket(FTPResponse pasvResponse) throws IOException {
        String raw = pasvResponse.getLines()
                                 .get(pasvResponse.getLines().size() - 1);
        int start = raw.indexOf('('), end = raw.indexOf(')', start);
        String[] parts = raw.substring(start + 1, end).split(",");

        String host = String.join(".",
            parts[0], parts[1], parts[2], parts[3]);
        int dataPort = (Integer.parseInt(parts[4]) << 8)
                     + Integer.parseInt(parts[5]);
        return new Socket(host, dataPort);
    }

    // Getters
    public boolean isConnected() {
        return isConnected;
    }
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}