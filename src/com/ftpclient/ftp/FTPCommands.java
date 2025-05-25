package com.ftpclient.ftp;

import java.io.IOException;
import java.util.List;

public class FTPCommands {
    private FTPClient client;

    /**
     * Initialize and connect to the server.
     */
    public FTPCommands(String server, int port) throws IOException {
        client = new FTPClient(server, port);
        client.connect();
    }

    /**
     * Log in with credentials.
     */
    public boolean login(String user, String pass) throws IOException {
        return client.login(user, pass);
    }

    /**
     * List files and directories.
     */
    public List<String> list() throws IOException {
        return client.listFiles();
    }

    /**
     * Download a file.
     */
    public boolean get(String remoteFile, String localPath) throws IOException {
        return client.downloadFile(remoteFile, localPath);
    }

    /**
     * Upload a file.
     */
    public boolean put(String localFile, String remoteName) throws IOException {
        return client.uploadFile(localFile, remoteName);
    }

    /**
     * Change remote directory.
     */
    public boolean cwd(String directory) throws IOException {
        return client.changeDirectory(directory);
    }

    /**
     * Make remote directory.
     */
    public boolean mkd(String directory) throws IOException {
        return client.makeDirectory(directory);
    }

    /** 
     * Delete a remote file.
     */
    public boolean delete(String remoteFile) throws IOException {
        return client.deleteFile(remoteFile);
    }

    /** 
     * Remove a remote directory.
     */
    public boolean rmdir(String directory) throws IOException {
        return client.removeDirectory(directory);
    }

    /**
     * Disconnect from the server.
     */
    public void quit() throws IOException {
        client.disconnect();
    }
}
