package com.ftpclient.ftp;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
    
    /**
     * Default constructor
     */
    public FTPClient() {
        this.port = 21; // Default FTP port
    }
    
    /**
     * Constructor with server and port
     */
    public FTPClient(String server, int port) {
        this.server = server;
        this.port = port;
    }
    
    /**
     * Connect to the FTP server
     */
    public boolean connect(String server, int port) throws IOException {
        this.server = server;
        this.port = port;
        return connect();
    }
    
    /**
     * Connect using previously set server and port
     */
    public boolean connect() throws IOException {
        if (isConnected) {
            return true;
        }
        
        try {
            commandSocket = new Socket(server, port);
            reader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));
            
            // Read the welcome message
            String response = readResponse();
            if (!response.startsWith("220")) {
                throw new IOException("Unable to connect to FTP server. Response: " + response);
            }
            
            isConnected = true;
            return true;
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }
    
    /**
     * Login to the FTP server
     */
    public boolean login(String username, String password) throws IOException {
        if (!isConnected) {
            throw new IOException("Not connected to FTP server");
        }
        
        this.username = username;
        this.password = password;
        
        // Send USER command
        sendCommand("USER " + username);
        String response = readResponse();
        
        // Server asks for password
        if (response.startsWith("331")) {
            // Send PASS command
            sendCommand("PASS " + password);
            response = readResponse();
            
            if (response.startsWith("230")) {
                isLoggedIn = true;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * List files in the current directory
     */
    public List<String> listFiles() throws IOException {
        if (!isLoggedIn) {
            throw new IOException("Not logged in");
        }
        
        // Enter passive mode
        sendCommand("PASV");
        String response = readResponse();
        
        if (!response.startsWith("227")) {
            throw new IOException("Could not enter passive mode. Response: " + response);
        }
        
        // Parse passive mode response to get data port
        Socket dataSocket = createDataSocket(response);
        
        // Send LIST command
        sendCommand("LIST");
        response = readResponse();
        
        if (!response.startsWith("150")) {
            dataSocket.close();
            throw new IOException("Could not get file listing. Response: " + response);
        }
        
        // Read data from data socket
        BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
        List<String> fileList = new ArrayList<>();
        String line;
        while ((line = dataReader.readLine()) != null) {
            fileList.add(line);
        }
        
        // Close data connection
        dataReader.close();
        dataSocket.close();
        
        // Read final response
        response = readResponse();
        if (!response.startsWith("226")) {
            throw new IOException("Error completing file listing. Response: " + response);
        }
        
        return fileList;
    }
    
    /**
     * Change working directory
     */
    public boolean changeDirectory(String directory) throws IOException {
        if (!isLoggedIn) {
            throw new IOException("Not logged in");
        }
        
        sendCommand("CWD " + directory);
        String response = readResponse();
        
        return response.startsWith("250");
    }
    
    /**
     * Download a file from the server
     */
    public boolean downloadFile(String remoteFile, String localFile) throws IOException {
        if (!isLoggedIn) {
            throw new IOException("Not logged in");
        }
        
        // Set binary mode
        sendCommand("TYPE I");
        String response = readResponse();
        if (!response.startsWith("200")) {
            throw new IOException("Could not set binary mode. Response: " + response);
        }
        
        // Enter passive mode
        sendCommand("PASV");
        response = readResponse();
        if (!response.startsWith("227")) {
            throw new IOException("Could not enter passive mode. Response: " + response);
        }
        
        // Parse passive mode response to get data port
        Socket dataSocket = createDataSocket(response);
        
        // Send RETR command
        sendCommand("RETR " + remoteFile);
        response = readResponse();
        
        if (!response.startsWith("150")) {
            dataSocket.close();
            throw new IOException("Could not download file. Response: " + response);
        }
        
        // Read data from data socket and write to local file
        BufferedInputStream dataIn = new BufferedInputStream(dataSocket.getInputStream());
        BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(localFile));
        
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = dataIn.read(buffer)) != -1) {
            fileOut.write(buffer, 0, bytesRead);
        }
        
        // Close streams
        fileOut.close();
        dataIn.close();
        dataSocket.close();
        
        // Read final response
        response = readResponse();
        return response.startsWith("226");
    }
    
    /**
     * Upload a file to the server
     */
    public boolean uploadFile(String localFile, String remoteFile) throws IOException {
        // Implementation similar to downloadFile but in reverse
        // We'll include this in a future update
        // For now, return false to indicate not implemented
        return false;
    }
    
    /**
     * Disconnect from the FTP server
     */
    public void disconnect() {
        if (isConnected) {
            try {
                sendCommand("QUIT");
                // Don't care about the response
                
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (commandSocket != null) commandSocket.close();
            } catch (IOException e) {
                // Ignore errors on disconnect
            } finally {
                isConnected = false;
                isLoggedIn = false;
            }
        }
    }
    
    /**
     * Send a command to the FTP server
     */
    private void sendCommand(String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
    }
    
    /**
     * Read a response from the FTP server
     */
    private String readResponse() throws IOException {
        String line = reader.readLine();
        StringBuilder response = new StringBuilder(line);
        
        // Check if multi-line response
        while (line.length() > 3 && line.charAt(3) == '-') {
            line = reader.readLine();
            response.append("\n").append(line);
        }
        
        return response.toString();
    }
    
    /**
     * Create a data socket from passive mode response
     */
    private Socket createDataSocket(String pasvResponse) throws IOException {
        // Extract IP and port from PASV response
        // Format: 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)
        int start = pasvResponse.indexOf('(');
        int end = pasvResponse.indexOf(')', start);
        String[] parts = pasvResponse.substring(start + 1, end).split(",");
        
        String dataHost = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        int dataPort = (Integer.parseInt(parts[4]) << 8) + Integer.parseInt(parts[5]);
        
        return new Socket(dataHost, dataPort);
    }
    
    // Getters and setters
    public boolean isConnected() {
        return isConnected;
    }
    
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}