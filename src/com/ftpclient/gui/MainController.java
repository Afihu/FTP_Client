package com.ftpclient.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.scene.control.Alert; 
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

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

    // Client-side navigation controls
    @FXML
    private Button clientGoButton;

    @FXML
    private Button clientUpButton;

    @FXML
    private TextField clientDirectoryField;

    @FXML
    private Button clientNewFolderButton;

    // Server-side navigation controls
    @FXML
    private Button serverGoButton;

    @FXML
    private Button serverUpButton;

    @FXML
    private TextField serverDirectoryField;

    @FXML
    private Button serverNewFolderButton;

    // FTP client instance
    private FTPClient ftpClient;
    
    // File choosers
    private FileChooser downloadFileChooser;
    private FileChooser uploadFileChooser;
    private String currentLocalDirectory = ".";
    private String currentServerDirectory = "/";

    @FXML
    private Label fileNameLabel;

    @FXML
    private Label fileSizeLabel;

    @FXML
    private Label fileTypeLabel;

    @FXML
    private Label lastModifiedLabel;

    @FXML
    private Label filePathLabel;

    @FXML
    private Label transferStatusLabel;

    @FXML
    private Button uploadButton;

    @FXML
    private Button downloadButton;

    @FXML
    private ProgressBar transferProgressBar;

    @FXML
    void initialize() {
        // Set default port
        portField.setText("21");
        
        // Initialize client file list with local directory
        loadLocalFiles();
        
        // Set initial status
        leftStatusLabel.setText("Ready");
        rightStatusLabel.setText("Disconnected");

        // Listeners for file details
        clientFileList.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> displayLocalFileDetails(newValue)
        );
        
        serverFileList.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> displayServerFileDetails(newValue)
        );

        // Double-click navigation for LOCAL files
        clientFileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = clientFileList.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.startsWith("[DIR]")) {
                    handleLocalDirectoryNavigation(selectedItem);
                }
            }
        });
        // Double-click navigation for SERVER files
        serverFileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = serverFileList.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.startsWith("d")) { // Directory indicator
                    handleServerDirectoryNavigation(selectedItem);
                }
            }
        });

        serverGoButton.setDisable(true);
        serverUpButton.setDisable(true);
        serverNewFolderButton.setDisable(true);
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
        
        final int finalPort = port;
        
        // Disable connect button during connection attempt
        connectButton.setDisable(true);
        leftStatusLabel.setText("Connecting to " + serverIP + ":" + finalPort + "...");
        
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
                            connectButton.setDisable(true); 
                            disconnectButton.setDisable(false);
                            loadServerFiles();
                            serverGoButton.setDisable(false);
                            serverUpButton.setDisable(false);
                            serverNewFolderButton.setDisable(false);
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
        connectButton.setDisable(false);  // ✅ RE-ENABLE CONNECT BUTTON
        serverFileList.getItems().clear();
        
        // Disable server controls
        serverGoButton.setDisable(true);
        serverUpButton.setDisable(true);
        serverNewFolderButton.setDisable(true);
        
        // Reset server directory
        currentServerDirectory = "/";
    }


    private void loadLocalFiles() {
        ObservableList<String> items = FXCollections.observableArrayList();
        File currentDir = new File(currentLocalDirectory);
        File[] files = currentDir.listFiles();
        
        // Parent directory option (except for root)
        if (!currentLocalDirectory.equals("/") && !currentLocalDirectory.matches("[A-Z]:\\\\?")) {
            items.add("[DIR] ..");
        }
        
        if (files != null) {
            // Directories first
            for (File file : files) {
                if (file.isDirectory()) {
                    items.add("[DIR] " + file.getName());
                }
            }
            
            // Then add files
            for (File file : files) {
                if (file.isFile()) {
                    items.add("[FILE] " + file.getName());
                }
            }
        }
        
        clientFileList.setItems(items);
        rightStatusLabel.setText("Local: " + currentLocalDirectory);
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

        // Method to display local file details
    private void displayLocalFileDetails(String selectedFile) {
        if (selectedFile == null) {
            clearFileDetails();
            downloadButton.setDisable(true);
            uploadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
            return;
        }
        
        // Handle parent directory
        if (selectedFile.equals("[DIR] ..")) {
            fileNameLabel.setText("Parent Directory");
            fileSizeLabel.setText("-");
            fileTypeLabel.setText("Directory");
            lastModifiedLabel.setText("-");
            filePathLabel.setText("Go up one level");
            transferStatusLabel.setText("Double-click or press 'Go' to navigate");
            
            uploadButton.setDisable(true);
            downloadButton.setDisable(true);
            return;
        }
        
        // Remove [DIR] or [FILE] prefix
        String fileName = selectedFile.replaceFirst("^\\[(DIR|FILE)\\] ", "");
        File file = new File(currentLocalDirectory, fileName);
        
        if (file.exists()) {
            fileNameLabel.setText(file.getName());
            fileSizeLabel.setText(file.isDirectory() ? "-" : formatFileSize(file.length()));
            fileTypeLabel.setText(file.isDirectory() ? "Directory" : getFileType(fileName));
            lastModifiedLabel.setText(formatDate(file.lastModified()));
            filePathLabel.setText(file.getAbsolutePath());
            
            if (file.isDirectory()) {
                transferStatusLabel.setText("Double-click or press 'Go' to enter folder");
                uploadButton.setDisable(true);
            } else {
                transferStatusLabel.setText("Ready to upload");
                uploadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
            }
            
            downloadButton.setDisable(true);
        }
    }
    // Method to display server file details
    private void displayServerFileDetails(String selectedFile) {
        if (selectedFile == null) {
            clearFileDetails();
            uploadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
            downloadButton.setDisable(true);
            return;
        }
        
        // Parse FTP directory listing format
        ParsedServerFile parsedFile = parseServerFileDetails(selectedFile);
        
        fileNameLabel.setText(parsedFile.name);
        fileSizeLabel.setText(parsedFile.size);
        fileTypeLabel.setText(parsedFile.type);
        lastModifiedLabel.setText(parsedFile.lastModified);
        filePathLabel.setText("Server: " + currentServerDirectory + "/" + parsedFile.name);
        
        if (parsedFile.isDirectory) {
            transferStatusLabel.setText("Double-click or press 'Go' to enter folder");
            downloadButton.setDisable(true);
        } else {
            transferStatusLabel.setText("Ready to download");
            downloadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
        }
        
        uploadButton.setDisable(true);
    }
    
    // Helper class for parsed server file info
    private static class ParsedServerFile {
        String name;
        String size;
        String type;
        String lastModified;
        boolean isDirectory;
        
        ParsedServerFile(String name, String size, String type, String lastModified, boolean isDirectory) {
            this.name = name;
            this.size = size;
            this.type = type;
            this.lastModified = lastModified;
            this.isDirectory = isDirectory;
        }
    }
    
    // Parse FTP directory listing format
    private ParsedServerFile parseServerFileDetails(String ftpListing) {
        try {
            // Example: "drwxr-xr-x    2 user     group        4096 May 25 12:00 dirname"
            // Example: "-rw-r--r--    1 user     group        1024 May 25 12:00 filename.txt"
            
            String[] parts = ftpListing.trim().split("\\s+");
            
            if (parts.length < 9) {
                // Fallback for simple listings
                return new ParsedServerFile(ftpListing, "Unknown", "Unknown", "Unknown", 
                                          ftpListing.startsWith("d") || ftpListing.contains("<DIR>"));
            }
            
            // Parse permissions and type
            String permissions = parts[0];
            boolean isDirectory = permissions.startsWith("d");
            
            // Parse size
            String size = isDirectory ? "-" : formatFileSize(Long.parseLong(parts[4]));
            
            // Parse date (parts 5, 6, 7)
            String month = parts.length > 5 ? parts[5] : "";
            String day = parts.length > 6 ? parts[6] : "";
            String timeOrYear = parts.length > 7 ? parts[7] : "";
            String lastModified = month + " " + day + " " + timeOrYear;
            
            // Parse filename (everything after the date)
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 8; i < parts.length; i++) {
                if (i > 8) nameBuilder.append(" ");
                nameBuilder.append(parts[i]);
            }
            String name = nameBuilder.toString();
            
            // Determine file type
            String type = isDirectory ? "Directory" : getFileType(name);
            
            return new ParsedServerFile(name, size, type, lastModified, isDirectory);
            
        } catch (Exception e) {
            // Fallback parsing
            String name = extractDirectoryName(ftpListing);
            boolean isDir = ftpListing.startsWith("d") || ftpListing.contains("<DIR>");
            return new ParsedServerFile(name, "Unknown", isDir ? "Directory" : "File", 
                                      "Unknown", isDir);
        }
    }
    // Clear file details
    private void clearFileDetails() {
        fileNameLabel.setText("-");
        fileSizeLabel.setText("-");
        fileTypeLabel.setText("-");
        lastModifiedLabel.setText("-");
        filePathLabel.setText("-");
        transferStatusLabel.setText("No file selected");
    }

    // Helper methods
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String getFileType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = fileName.substring(lastDot + 1).toLowerCase();
            switch (extension) {
                case "txt": return "Text File";
                case "pdf": return "PDF Document";
                case "jpg": case "jpeg": case "png": case "gif": return "Image File";
                case "mp3": case "wav": return "Audio File";
                case "mp4": case "avi": return "Video File";
                case "zip": case "rar": return "Archive File";
                default: return extension.toUpperCase() + " File";
            }
        }
        return "Unknown";
    }

    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    // Updated transfer methods using selected files
    @FXML
    void handleUploadSelected() {
        String selectedFile = clientFileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            leftStatusLabel.setText("Please select a file to upload");
            return;
        }
        
        String fileName = selectedFile.replaceFirst("^\\[(DIR|FILE)\\] ", "");
        File file = new File(currentLocalDirectory, fileName);
        
        if (file.exists() && !file.isDirectory() && ftpClient != null) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        transferStatusLabel.setText("Uploading...");
                        leftStatusLabel.setText("Uploading " + file.getName() + "...");
                    });
                    
                    boolean success = ftpClient.uploadFile(file.getAbsolutePath(), file.getName());
                    
                    Platform.runLater(() -> {
                        if (success) {
                            transferStatusLabel.setText("Upload completed");
                            leftStatusLabel.setText("Upload completed: " + file.getName());
                            loadServerFiles();
                        } else {
                            transferStatusLabel.setText("Upload failed");
                            leftStatusLabel.setText("Upload failed");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        transferStatusLabel.setText("Upload error");
                        leftStatusLabel.setText("Upload error: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    @FXML
    void handleDownloadSelected() {
        String selectedFile = serverFileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            leftStatusLabel.setText("Please select a file to download");
            return;
        }
        
        // Parse the server file to get the actual filename
        ParsedServerFile parsedFile = parseServerFileDetails(selectedFile);
        
        if (parsedFile.isDirectory) {
            leftStatusLabel.setText("Cannot download a directory");
            return;
        }
        
        if (ftpClient != null) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        transferStatusLabel.setText("Downloading...");
                        leftStatusLabel.setText("Downloading " + parsedFile.name + "...");
                    });
                    
                    // Download to current local directory with the actual filename
                    String localPath = new File(currentLocalDirectory, parsedFile.name).getAbsolutePath();
                    boolean success = ftpClient.downloadFile(parsedFile.name, localPath);
                    
                    Platform.runLater(() -> {
                        if (success) {
                            transferStatusLabel.setText("Download completed");
                            leftStatusLabel.setText("Download completed: " + parsedFile.name);
                            loadLocalFiles();
                        } else {
                            transferStatusLabel.setText("Download failed");
                            leftStatusLabel.setText("Download failed");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        transferStatusLabel.setText("Download error");
                        leftStatusLabel.setText("Download error: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private String extractDirectoryName(String selectedItem) {
        // Parse FTP directory listing format
        // Example: "drwxr-xr-x    2 user     group        4096 May 25 12:00 dirname"
        String[] parts = selectedItem.split("\\s+");
        if (parts.length > 8) {
            return parts[parts.length - 1]; // Last part is usually the directory name
        }
        return selectedItem; // Fallback
    }

    private void handleLocalDirectoryNavigation(String selectedFolder) {
        String folderName = selectedFolder.replaceFirst("^\\[DIR\\] ", "");
        
        // Handle parent directory navigation
        if (folderName.equals("..")) {
            handleLocalGoUp();
            return;
        }
        
        File targetDir = new File(currentLocalDirectory, folderName);
        
        if (targetDir.exists() && targetDir.isDirectory()) {
            try {
                currentLocalDirectory = targetDir.getCanonicalPath();
                loadLocalFiles();
                leftStatusLabel.setText("Changed local directory to: " + folderName);
                
                // Update file details to show the new directory
                filePathLabel.setText(currentLocalDirectory);
                transferStatusLabel.setText("Navigated to local folder");
            } catch (IOException e) {
                leftStatusLabel.setText("Error accessing local directory: " + e.getMessage());
            }
        } else {
            leftStatusLabel.setText("Local directory not accessible: " + folderName);
        }
    }

    private void handleServerDirectoryNavigation(String selectedFolder) {
        if (ftpClient == null || !ftpClient.isLoggedIn()) {
            leftStatusLabel.setText("Not connected to server");
            return;
        }
        
        // Use the new parsing method
        ParsedServerFile parsedFile = parseServerFileDetails(selectedFolder);
        
        if (!parsedFile.isDirectory) {
            leftStatusLabel.setText("Selected item is not a directory");
            return;
        }
        
        new Thread(() -> {
            try {
                boolean success = ftpClient.changeDirectory(parsedFile.name);
                Platform.runLater(() -> {
                    if (success) {
                        // Update current server directory tracking
                        if (currentServerDirectory.endsWith("/")) {
                            currentServerDirectory += parsedFile.name;
                        } else {
                            currentServerDirectory += "/" + parsedFile.name;
                        }
                        
                        leftStatusLabel.setText("Changed server directory to: " + parsedFile.name);
                        loadServerFiles();
                        
                        // Update file details
                        filePathLabel.setText("Server: " + currentServerDirectory);
                        transferStatusLabel.setText("Navigated to server folder");
                    } else {
                        leftStatusLabel.setText("Failed to change server directory");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> 
                    leftStatusLabel.setText("Server directory change error: " + e.getMessage()));
            }
        }).start();
    }

    private void handleLocalGoUp() {
        File currentDir = new File(currentLocalDirectory);
        File parentDir = currentDir.getParentFile();
        
        if (parentDir != null && parentDir.exists()) {
            try {
                currentLocalDirectory = parentDir.getCanonicalPath();
                loadLocalFiles();
                leftStatusLabel.setText("Moved up to: " + currentLocalDirectory);
            } catch (IOException e) {
                leftStatusLabel.setText("Error navigating up: " + e.getMessage());
            }
        } else {
            leftStatusLabel.setText("Already at root directory");
        }
    }

    // CLIENT-SIDE HANDLERS
    
    @FXML
    void handleClientGo() {
        String path = clientDirectoryField.getText().trim();
        
        if (!path.isEmpty()) {
            // Manual path navigation
            File targetDir = new File(path);
            if (targetDir.exists() && targetDir.isDirectory()) {
                try {
                    currentLocalDirectory = targetDir.getCanonicalPath();
                    loadLocalFiles();
                    leftStatusLabel.setText("Changed local directory to: " + path);
                    clientDirectoryField.clear();
                } catch (IOException e) {
                    leftStatusLabel.setText("Error accessing directory: " + e.getMessage());
                }
            } else {
                leftStatusLabel.setText("Directory not found: " + path);
            }
        } else {
            // Selected folder navigation
            String selectedFile = clientFileList.getSelectionModel().getSelectedItem();
            if (selectedFile != null && selectedFile.startsWith("[DIR]")) {
                handleLocalDirectoryNavigation(selectedFile);
            } else {
                leftStatusLabel.setText("Please select a folder or enter a path");
            }
        }
    }
    @FXML
    void handleClientNewFolder() {
        String folderName = showNewFolderDialog("Create New Local Folder");
        if (folderName != null && !folderName.trim().isEmpty()) {
            File newFolder = new File(currentLocalDirectory, folderName.trim());
            if (newFolder.mkdir()) {
                leftStatusLabel.setText("Created local folder: " + folderName);
                loadLocalFiles();
            } else {
                leftStatusLabel.setText("Failed to create local folder: " + folderName);
            }
        }
    }

    @FXML
    void handleClientUp() {
        handleLocalGoUp();
    }    

    // SERVER-SIDE HANDLERS
    
    @FXML
    void handleServerGo() {
        String path = serverDirectoryField.getText().trim();
        
        if (!path.isEmpty()) {
            // Manual path navigation
            if (ftpClient == null || !ftpClient.isLoggedIn()) {
                leftStatusLabel.setText("Please ensure you're connected to server");
                return;
            }
            
            new Thread(() -> {
                try {
                    boolean success = ftpClient.changeDirectory(path);
                    Platform.runLater(() -> {
                        if (success) {
                            currentServerDirectory = path;
                            leftStatusLabel.setText("Changed server directory to: " + path);
                            loadServerFiles();
                            serverDirectoryField.clear();
                        } else {
                            leftStatusLabel.setText("Failed to change server directory");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> 
                        leftStatusLabel.setText("Server directory change error: " + e.getMessage()));
                }
            }).start();
        } else {
            // Selected folder navigation
            String selectedFile = serverFileList.getSelectionModel().getSelectedItem();
            if (selectedFile != null && (selectedFile.startsWith("d") || selectedFile.contains("<DIR>"))) {
                handleServerDirectoryNavigation(selectedFile);
            } else {
                leftStatusLabel.setText("Please select a folder or enter a path");
            }
        }
    }
    
    @FXML
    void handleServerUp() {
        if (ftpClient == null || !ftpClient.isLoggedIn()) {
            leftStatusLabel.setText("Please ensure you're connected to server");
            return;
        }
        
        new Thread(() -> {
            try {
                boolean success = ftpClient.changeDirectory("..");
                Platform.runLater(() -> {
                    if (success) {
                        // Update currentServerDirectory tracking
                        if (currentServerDirectory.contains("/")) {
                            int lastSlash = currentServerDirectory.lastIndexOf("/");
                            if (lastSlash > 0) {
                                currentServerDirectory = currentServerDirectory.substring(0, lastSlash);
                            } else {
                                currentServerDirectory = "/";
                            }
                        }
                        
                        leftStatusLabel.setText("Moved up one server directory");
                        loadServerFiles();
                    } else {
                        leftStatusLabel.setText("Failed to go up server directory");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> 
                    leftStatusLabel.setText("Error going up server directory: " + e.getMessage()));
            }
        }).start();
    }
    
    @FXML
    void handleServerNewFolder() {
        if (ftpClient == null || !ftpClient.isLoggedIn()) {
            leftStatusLabel.setText("Please ensure you're connected to server");
            return;
        }
        
        String folderName = showNewFolderDialog("Create New Server Folder");
        if (folderName != null && !folderName.trim().isEmpty()) {
            final String finalFolderName = folderName.trim();
            
            new Thread(() -> {
                try {
                    boolean success = ftpClient.makeDirectory(finalFolderName);
                    Platform.runLater(() -> {
                        if (success) {
                            leftStatusLabel.setText("Created server folder: " + finalFolderName);
                            loadServerFiles();
                        } else {
                            leftStatusLabel.setText("Failed to create server folder: " + finalFolderName);
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> 
                        leftStatusLabel.setText("Server folder creation error: " + e.getMessage()));
                }
            }).start();
        }
    }

        // Show dialog for new folder name input
    private String showNewFolderDialog(String title) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter folder name:");
        dialog.setContentText("Folder name:");
        
        // Style the dialog
        dialog.getDialogPane().setPrefWidth(350);
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
}