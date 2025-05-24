package com.ftpclient.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;

import com.ftpclient.ftp.FTPClient;
import java.io.*;
import java.util.List;

public class MainController {

    // Connection fields from the left panel
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField serverAddressField;

    @FXML
    private TextField portField;

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    // File list views from the center panel
    @FXML
    private ListView<String> clientFileList;

    @FXML
    private ListView<String> serverFileList;

    // Status labels from the bottom panel
    @FXML
    private Label leftStatusLabel;

    @FXML
    private Label rightStatusLabel;

    // Menu items
    @FXML
    private MenuItem newMenuItem;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem closeMenuItem;

    @FXML
    private MenuItem saveMenuItem;

    @FXML
    private MenuItem saveAsMenuItem;

    @FXML
    private MenuItem revertMenuItem;

    @FXML
    private MenuItem preferencesMenuItem;

    @FXML
    private MenuItem quitMenuItem;

    @FXML
    private MenuItem undoMenuItem;

    @FXML
    private MenuItem redoMenuItem;

    @FXML
    private MenuItem cutMenuItem;

    @FXML
    private MenuItem copyMenuItem;

    @FXML
    private MenuItem pasteMenuItem;

    @FXML
    private MenuItem deleteMenuItem;

    @FXML
    private MenuItem selectAllMenuItem;

    @FXML
    private MenuItem unselectAllMenuItem;

    @FXML
    private MenuItem aboutMenuItem;

    // FTP client instance
    private FTPClient ftpClient;
    
    // File choosers
    private FileChooser downloadFileChooser;
    private FileChooser uploadFileChooser;

    @FXML
    void initialize() {
        // Set default port
        portField.setText("21");
        
        // Initialize client file list with local directory
        loadLocalFiles();
        
        // Set initial status
        leftStatusLabel.setText("Ready");
        rightStatusLabel.setText("Disconnected");
    }

    @FXML
    void handleConnect() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String serverIP = serverAddressField.getText().trim();
        String portText = portField.getText().trim();
        
        if (username.isEmpty() || serverIP.isEmpty()) {
            leftStatusLabel.setText("Please fill in username and server IP");
            return;
        }
        
        int port = 21;
        if (!portText.isEmpty()) {
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                leftStatusLabel.setText("Invalid port number");
                return;
            }
        }
        
        // Make port effectively final by creating a final copy
        final int finalPort = port;
        
        // Disable connect button during connection attempt
        connectButton.setDisable(true);
        leftStatusLabel.setText("Connecting to " + serverIP + ":" + finalPort + "...");
        
        // Run connection in background thread
        new Thread(() -> {
            try {
                ftpClient = new FTPClient(serverIP, finalPort);
                boolean connected = ftpClient.connect();
                
                if (connected) {
                    boolean loggedIn = ftpClient.login(username, password);
                    
                    Platform.runLater(() -> {
                        if (loggedIn) {
                            leftStatusLabel.setText("Connected to " + serverIP);
                            rightStatusLabel.setText("Logged in as " + username);
                            connectButton.setDisable(false);
                            disconnectButton.setDisable(false);
                            loadServerFiles();
                        } else {
                            leftStatusLabel.setText("Login failed");
                            connectButton.setDisable(false);
                            ftpClient = null;
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        leftStatusLabel.setText("Connection failed");
                        connectButton.setDisable(false);
                        ftpClient = null;
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    leftStatusLabel.setText("Error: " + e.getMessage());
                    connectButton.setDisable(false);
                    ftpClient = null;
                });
            }
        }).start();
    }
    
    @FXML
    void handleDisconnect() {
        if (ftpClient != null) {
            ftpClient.disconnect();
            ftpClient = null;
        }
        leftStatusLabel.setText("Disconnected");
        rightStatusLabel.setText("Ready");
        disconnectButton.setDisable(true);
        serverFileList.getItems().clear();
    }

    private void loadLocalFiles() {
        ObservableList<String> items = FXCollections.observableArrayList();
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        
        if (files != null) {
            for (File file : files) {
                String prefix = file.isDirectory() ? "[DIR] " : "[FILE] ";
                items.add(prefix + file.getName());
            }
        }
        
        clientFileList.setItems(items);
    }

    private void loadServerFiles() {
        if (ftpClient == null || !ftpClient.isLoggedIn()) {
            return;
        }
        
        new Thread(() -> {
            try {
                List<String> files = ftpClient.listFiles();
                Platform.runLater(() -> {
                    ObservableList<String> items = FXCollections.observableArrayList(files);
                    serverFileList.setItems(items);
                    rightStatusLabel.setText("Files loaded (" + files.size() + " items)");
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    rightStatusLabel.setText("Error loading files: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    void handleListFiles() {
        loadServerFiles();
    }

    @FXML
    void handleDownloadFile() {
        String selectedFile = serverFileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            leftStatusLabel.setText("Please select a file to download");
            return;
        }
        
        if (downloadFileChooser == null) {
            downloadFileChooser = new FileChooser();
        }
        File file = downloadFileChooser.showSaveDialog(null);
        if (file != null && ftpClient != null) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> 
                        leftStatusLabel.setText("Downloading " + selectedFile + "..."));
                    
                    boolean success = ftpClient.downloadFile(selectedFile, file.getAbsolutePath());
                    
                    Platform.runLater(() -> {
                        if (success) {
                            leftStatusLabel.setText("Download completed: " + file.getName());
                            loadLocalFiles(); // Refresh local file list
                        } else {
                            leftStatusLabel.setText("Download failed");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> 
                        leftStatusLabel.setText("Download error: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    void handleUploadFile() {
        if (uploadFileChooser == null) {
            uploadFileChooser = new FileChooser();
        }
        File file = uploadFileChooser.showOpenDialog(null);
        if (file != null && ftpClient != null) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> 
                        leftStatusLabel.setText("Uploading " + file.getName() + "..."));
                    
                    boolean success = ftpClient.uploadFile(file.getAbsolutePath(), file.getName());
                    
                    Platform.runLater(() -> {
                        if (success) {
                            leftStatusLabel.setText("Upload completed: " + file.getName());
                            loadServerFiles(); // Refresh server file list
                        } else {
                            leftStatusLabel.setText("Upload failed");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> 
                        leftStatusLabel.setText("Upload error: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    void handleRefreshFiles() {
        loadLocalFiles();
        loadServerFiles();
        leftStatusLabel.setText("File lists refreshed");
    }

    // Menu handlers
    @FXML
    void handleNew() {
        // Handle new file
    }

    @FXML
    void handleOpen() {
        // Handle open file
    }

    @FXML
    void handleClose() {
        handleDisconnect();
    }

    @FXML
    void handleSave() {
        // Handle save
    }

    @FXML
    void handleQuit() {
        handleDisconnect();
        System.exit(0);
    }

    @FXML
    void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("FTP Client");
        alert.setContentText("A simple FTP client application for connecting to FTP servers");
        alert.showAndWait();
    }
}