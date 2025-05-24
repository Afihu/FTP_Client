package com.ftpclient.ftp;

import java.io.IOException;
import java.util.List;

public class FTPCommands {
    private FTPClient client;

    public FTPCommands(String server, int port) throws IOException {
        client = new FTPClient(server, port);
        client.connect();
    }

    public boolean login(String user, String pass) throws IOException {
        return client.login(user, pass);
    }

    public List<String> list() throws IOException {
        return client.listFiles();
    }

    public boolean get(String remoteFile, String localPath) throws IOException {
        return client.downloadFile(remoteFile, localPath);
    }

    public boolean put(String localFile, String remoteName) throws IOException {
        return client.uploadFile(localFile, remoteName);
    }

    public boolean cwd(String directory) throws IOException {
        return client.changeDirectory(directory);
    }

    public boolean mkd(String directory) throws IOException {
        return client.makeDirectory(directory);
    }

    public void quit() throws IOException {
        client.disconnect();
    }
}