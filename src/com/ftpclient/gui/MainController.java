package com.ftpclient.gui;

import javafx.scene.control.ButtonType;
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

/**
 * MainController - JavaFX Controller for the FTP Client GUI
 * 
 * This class handles all user interactions with the FTP client interface,
 * managing both local file operations and remote FTP server communications.
 * It provides a dual-pane file browser with connection management, file transfers,
 * and directory navigation capabilities.
 */
public class MainController {

    // ================= GUI COMPONENT DECLARATIONS =================
    
    // Connection panel fields - handles FTP server authentication
    @FXML private TextField usernameField;        // FTP username input
    @FXML private PasswordField passwordField;   // FTP password input  
    @FXML private TextField serverAddressField;  // FTP server IP/hostname
    @FXML private TextField portField;           // FTP server port (default: 21)
    @FXML private Button connectButton;          // Initiates FTP connection
    @FXML private Button disconnectButton;       // Terminates FTP connection

    // Main file browser lists - dual-pane interface
    @FXML private ListView<String> clientFileList;  // Local filesystem browser
    @FXML private ListView<String> serverFileList;  // Remote FTP server browser

    // Status display labels
    @FXML private Label leftStatusLabel;   // Connection and operation status
    @FXML private Label rightStatusLabel;  // File listing and directory info

    // Menu bar items (File, Edit, Help menus)
    @FXML private MenuItem newMenuItem, openMenuItem, closeMenuItem;
    @FXML private MenuItem saveMenuItem, saveAsMenuItem, revertMenuItem;
    @FXML private MenuItem preferencesMenuItem, quitMenuItem;
    @FXML private MenuItem undoMenuItem, redoMenuItem, cutMenuItem;
    @FXML private MenuItem copyMenuItem, pasteMenuItem, deleteMenuItem;
    @FXML private MenuItem selectAllMenuItem, unselectAllMenuItem;
    @FXML private MenuItem aboutMenuItem;

    // Local directory navigation controls
    @FXML private Button clientGoButton;           // Navigate to selected/typed directory
    @FXML private Button clientUpButton;           // Go up one directory level
    @FXML private TextField clientDirectoryField;  // Manual path input
    @FXML private Button clientNewFolderButton;    // Create new local folder

    // Server directory navigation controls  
    @FXML private Button serverGoButton;           // Navigate on FTP server
    @FXML private Button serverUpButton;           // Go up on FTP server
    @FXML private TextField serverDirectoryField;  // Manual server path input
    @FXML private Button serverNewFolderButton;    // Create new server folder

    // File details panel - shows properties of selected files
    @FXML private Label fileNameLabel;       // Selected file name
    @FXML private Label fileSizeLabel;       // File size (formatted)
    @FXML private Label fileTypeLabel;       // File type/extension info
    @FXML private Label lastModifiedLabel;   // Last modification date
    @FXML private Label filePathLabel;       // Full file path
    @FXML private Label transferStatusLabel; // Transfer operation status

    // File operation controls
    @FXML private Button uploadButton;        // Upload selected local file
    @FXML private Button downloadButton;      // Download selected server file
    @FXML private ProgressBar transferProgressBar; // Progress indicator
    @FXML private Button deleteButton;        // Delete selected file/folder

    // ================= INTERNAL STATE VARIABLES =================
    
    private FTPClient ftpClient;                    // FTP connection handler
    private FileChooser downloadFileChooser;       // File save dialog for downloads
    private FileChooser uploadFileChooser;         // File open dialog for uploads
    private String currentLocalDirectory = ".";    // Current local working directory
    private String currentServerDirectory = "/";   // Current server working directory

    // ================= INITIALIZATION =================

    /**
     * JavaFX initialization method - called automatically after FXML loading
     * Sets up the initial GUI state, event listeners, and default values
     */
    @FXML
    void initialize() {
        // Set FTP default port
        portField.setText("21");
        
        // Load initial local directory contents
        loadLocalFiles();
        
        // Set initial connection status
        leftStatusLabel.setText("Ready");
        rightStatusLabel.setText("Disconnected");

        // Setup file selection listeners for details panel updates
        clientFileList.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> displayLocalFileDetails(newValue)
        );
        
        serverFileList.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> displayServerFileDetails(newValue)
        );

        // Setup double-click navigation for local files
        clientFileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = clientFileList.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.startsWith("[DIR]")) {
                    handleLocalDirectoryNavigation(selectedItem);
                }
            }
        });
        
        // Setup double-click navigation for server files
        serverFileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = serverFileList.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.startsWith("d")) { // Unix-style directory indicator
                    handleServerDirectoryNavigation(selectedItem);
                }
            }
        });

        // Disable server controls until connection is established
        serverGoButton.setDisable(true);
        serverUpButton.setDisable(true);
        serverNewFolderButton.setDisable(true);
    }

    // ================= CONNECTION MANAGEMENT =================

    /**
     * Handles FTP server connection attempt
     * Validates input fields, establishes connection, and authenticates user
     * Runs connection process in background thread to prevent GUI freezing
     */
    @FXML
    void handleConnect() {
        // Validate required connection parameters
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String serverIP = serverAddressField.getText().trim();
        String portText = portField.getText().trim();
        
        if (username.isEmpty() || serverIP.isEmpty()) {
            leftStatusLabel.setText("Please fill in username and server IP");
            return;
        }
        
        // Parse port number with fallback to default
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
        
        // Perform connection in background thread
        new Thread(() -> {
            try {
                ftpClient = new FTPClient(serverIP, finalPort);
                boolean connected = ftpClient.connect();
                
                if (connected) {
                    boolean loggedIn = ftpClient.login(username, password);
                    
                    // Update GUI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (loggedIn) {
                            // Successful connection - enable server features
                            leftStatusLabel.setText("Connected to " + serverIP);
                            rightStatusLabel.setText("Logged in as " + username);
                            connectButton.setDisable(true); 
                            disconnectButton.setDisable(false);
                            loadServerFiles();
                            serverGoButton.setDisable(false);
                            serverUpButton.setDisable(false);
                            serverNewFolderButton.setDisable(false);
                        } else {
                            // Authentication failed
                            leftStatusLabel.setText("Login failed");
                            connectButton.setDisable(false);
                            ftpClient = null;
                        }
                    });
                } else {
                    // Connection failed
                    Platform.runLater(() -> {
                        leftStatusLabel.setText("Connection failed");
                        connectButton.setDisable(false);
                        ftpClient = null;
                    });
                }
            } catch (IOException e) {
                // Network error occurred
                Platform.runLater(() -> {
                    leftStatusLabel.setText("Error: " + e.getMessage());
                    connectButton.setDisable(false);
                    ftpClient = null;
                });
            }
        }).start();
    }

    /**
     * Handles FTP server disconnection
     * Closes FTP connection and resets GUI to disconnected state
     */
    @FXML
    void handleDisconnect() {
        if (ftpClient != null) {
            ftpClient.disconnect();
            ftpClient = null;
        }
        
        // Reset GUI to disconnected state
        leftStatusLabel.setText("Disconnected");
        rightStatusLabel.setText("Ready");
        disconnectButton.setDisable(true);
        connectButton.setDisable(false);
        serverFileList.getItems().clear();
        
        // Disable server-specific controls
        serverGoButton.setDisable(true);
        serverUpButton.setDisable(true);
        serverNewFolderButton.setDisable(true);
        
        // Reset server directory tracking
        currentServerDirectory = "/";
    }

    // ================= FILE LISTING AND NAVIGATION =================

    /**
     * Loads and displays files from the current local directory
     * Populates the client file list with directories first, then files
     * Uses [DIR] and [FILE] prefixes for visual distinction
     */
    private void loadLocalFiles() {
        ObservableList<String> items = FXCollections.observableArrayList();
        File currentDir = new File(currentLocalDirectory);
        File[] files = currentDir.listFiles();
        
        // Add parent directory option (except for root directories)
        if (!currentLocalDirectory.equals("/") && !currentLocalDirectory.matches("[A-Z]:\\\\?")) {
            items.add("[DIR] ..");
        }
        
        if (files != null) {
            // Add directories first (sorted presentation)
            for (File file : files) {
                if (file.isDirectory()) {
                    items.add("[DIR] " + file.getName());
                }
            }
            
            // Add files after directories
            for (File file : files) {
                if (file.isFile()) {
                    items.add("[FILE] " + file.getName());
                }
            }
        }
        
        // Update GUI components
        clientFileList.setItems(items);
        rightStatusLabel.setText("Local: " + currentLocalDirectory);
    }

    /**
     * Loads and displays files from the current FTP server directory
     * Runs in background thread to prevent GUI blocking during network operations
     * Uses FTP LIST command through FTPClient.listFiles()
     */
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

    /**
     * Manual refresh of server file listing
     * Called by refresh button or menu item
     */
    @FXML
    void handleListFiles() {
        loadServerFiles();
    }

    /**
     * Refreshes both local and server file listings
     * Useful after file operations that might change directory contents
     */
    @FXML
    void handleRefreshFiles() {
        loadLocalFiles();
        loadServerFiles();
        leftStatusLabel.setText("File lists refreshed");
    }

    // ================= FILE TRANSFER OPERATIONS =================

    /**
     * Legacy download method using file chooser dialog
     * Downloads selected server file to user-specified local location
     * Maintained for compatibility with existing UI elements
     */
    @FXML
    void handleDownloadFile() {
        String selectedFile = serverFileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            leftStatusLabel.setText("Please select a file to download");
            return;
        }
        
        // Show save dialog
        if (downloadFileChooser == null) {
            downloadFileChooser = new FileChooser();
        }
        File file = downloadFileChooser.showSaveDialog(null);
        
        if (file != null && ftpClient != null) {
            // Perform download in background thread
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

    /**
     * Legacy upload method using file chooser dialog
     * Uploads user-selected local file to current server directory
     * Maintained for compatibility with existing UI elements
     */
    @FXML
    void handleUploadFile() {
        // Show file selection dialog
        if (uploadFileChooser == null) {
            uploadFileChooser = new FileChooser();
        }
        File file = uploadFileChooser.showOpenDialog(null);
        
        if (file != null && ftpClient != null) {
            // Perform upload in background thread
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

    /**
     * Uploads currently selected local file to FTP server
     * Uses current file selection instead of file chooser dialog
     * Integrated with file browser for seamless operation
     */
    @FXML
    void handleUploadSelected() {
        String selectedFile = clientFileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            leftStatusLabel.setText("Please select a file to upload");
            return;
        }
        
        // Extract filename from list item format
        String fileName = selectedFile.replaceFirst("^\\[(DIR|FILE)\\] ", "");
        File file = new File(currentLocalDirectory, fileName);
        
        // Validate file exists and is not a directory
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

    /**
     * Downloads currently selected server file to local directory
     * Uses current file selection and automatically names/places file
     * More user-friendly than the legacy download method
     */
    @FXML
    void handleDownloadSelected() {
        String selectedFile = serverFileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            leftStatusLabel.setText("Please select a file to download");
            return;
        }
        
        // Parse server file listing to extract actual filename
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
                    
                    // Download to current local directory with original filename
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

    // ================= DIRECTORY NAVIGATION =================

    /**
     * Handles local directory navigation via double-click or Go button
     * Supports both parent directory (..) and subdirectory navigation
     */
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
                
                // Update file details panel
                filePathLabel.setText(currentLocalDirectory);
                transferStatusLabel.setText("Navigated to local folder");
            } catch (IOException e) {
                leftStatusLabel.setText("Error accessing local directory: " + e.getMessage());
            }
        } else {
            leftStatusLabel.setText("Local directory not accessible: " + folderName);
        }
    }

    /**
     * Handles server directory navigation via double-click or Go button
     * Uses FTP CWD (Change Working Directory) command
     * Parses FTP directory listings to extract directory names
     */
    private void handleServerDirectoryNavigation(String selectedFolder) {
        if (ftpClient == null || !ftpClient.isLoggedIn()) {
            leftStatusLabel.setText("Not connected to server");
            return;
        }
        
        // Parse FTP listing format to extract directory name
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
                        // Update server directory tracking
                        if (currentServerDirectory.endsWith("/")) {
                            currentServerDirectory += parsedFile.name;
                        } else {
                            currentServerDirectory += "/" + parsedFile.name;
                        }
                        
                        leftStatusLabel.setText("Changed server directory to: " + parsedFile.name);
                        loadServerFiles();
                        
                        // Update file details panel
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

    /**
     * Navigates to parent directory in local filesystem
     * Updates current directory and refreshes file listing
     */
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

    // ================= GUI EVENT HANDLERS =================

    /**
     * Local directory "Go" button handler
     * Supports both manual path entry and selected folder navigation
     */
    @FXML
    void handleClientGo() {
        String path = clientDirectoryField.getText().trim();
        
        if (!path.isEmpty()) {
            // Manual path navigation from text field
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
            // Navigate to selected folder in file list
            String selectedFile = clientFileList.getSelectionModel().getSelectedItem();
            if (selectedFile != null && selectedFile.startsWith("[DIR]")) {
                handleLocalDirectoryNavigation(selectedFile);
            } else {
                leftStatusLabel.setText("Please select a folder or enter a path");
            }
        }
    }

    /**
     * Creates new folder in current local directory
     * Shows input dialog for folder name and creates directory
     */
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

    /**
     * Local directory "Up" button handler
     * Navigates to parent directory
     */
    @FXML
    void handleClientUp() {
        handleLocalGoUp();
    }    

    /**
     * Server directory "Go" button handler
     * Supports both manual path entry and selected folder navigation
     */
    @FXML
    void handleServerGo() {
        String path = serverDirectoryField.getText().trim();
        
        if (!path.isEmpty()) {
            // Manual path navigation from text field
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
            // Navigate to selected folder in server file list
            String selectedFile = serverFileList.getSelectionModel().getSelectedItem();
            if (selectedFile != null && (selectedFile.startsWith("d") || selectedFile.contains("<DIR>"))) {
                handleServerDirectoryNavigation(selectedFile);
            } else {
                leftStatusLabel.setText("Please select a folder or enter a path");
            }
        }
    }
    
    /**
     * Server directory "Up" button handler
     * Uses FTP CWD command with ".." to go to parent directory
     */
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
                        // Update server directory path tracking
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
    
    /**
     * Creates new folder on FTP server in current directory
     * Shows input dialog and uses FTP MKD command
     */
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

    // ================= FILE DELETION OPERATIONS =================

    /**
     * Main delete handler - determines whether to delete local or server file
     * Routes to appropriate deletion method based on current selection
     */
    @FXML
    void handleDelete() {
        // Check which pane has a selected item
        String clientSelected = clientFileList.getSelectionModel().getSelectedItem();
        String serverSelected = serverFileList.getSelectionModel().getSelectedItem();
        
        if (clientSelected != null) {
            handleDeleteLocal(clientSelected);
        } else if (serverSelected != null) {
            handleDeleteServer(serverSelected);
        } else {
            leftStatusLabel.setText("Please select a file or folder to delete");
        }
    }

    /**
     * Handles deletion of local files and directories
     * Shows confirmation dialog and performs recursive deletion for directories
     */
    private void handleDeleteLocal(String selectedItem) {
        // Prevent deletion of parent directory reference
        if (selectedItem.equals("[DIR] ..")) {
            leftStatusLabel.setText("Cannot delete parent directory");
            return;
        }
        
        String fileName = selectedItem.replaceFirst("^\\[(DIR|FILE)\\] ", "");
        File file = new File(currentLocalDirectory, fileName);
        
        if (!file.exists()) {
            leftStatusLabel.setText("File not found: " + fileName);
            return;
        }
        
        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete " + (file.isDirectory() ? "folder" : "file") + "?");
        confirmAlert.setContentText("Are you sure you want to delete \"" + fileName + "\"?" + 
                                (file.isDirectory() ? "\n\nThis can only delete an empty folder. \nDeleting a non-empty one will result in error." : ""));
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = deleteLocalFile(file);
            if (success) {
                leftStatusLabel.setText("Deleted: " + fileName);
                loadLocalFiles();
                clearFileDetails();
            } else {
                leftStatusLabel.setText("Failed to delete: " + fileName);
            }
        }
    }

    /**
     * Handles deletion of server files and directories
     * Uses FTP DELE command for files and RMD command for directories
     */
    private void handleDeleteServer(String selectedItem) {
        if (ftpClient == null || !ftpClient.isLoggedIn()) {
            leftStatusLabel.setText("Not connected to server");
            return;
        }
        
        ParsedServerFile parsedFile = parseServerFileDetails(selectedItem);
        
        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete server " + (parsedFile.isDirectory ? "folder" : "file") + "?");
        confirmAlert.setContentText("Are you sure you want to delete \"" + parsedFile.name + "\"?" + 
                                (parsedFile.isDirectory ? "\n\nThis can only delete an empty folder. \nDeleting a non-empty one will result in error." : ""));
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    boolean success;
                    if (parsedFile.isDirectory) {
                        success = ftpClient.removeDirectory(parsedFile.name);
                    } else {
                        success = ftpClient.deleteFile(parsedFile.name);
                    }
                    
                    Platform.runLater(() -> {
                        if (success) {
                            leftStatusLabel.setText("Deleted server item: " + parsedFile.name);
                            loadServerFiles();
                            clearFileDetails();
                        } else {
                            leftStatusLabel.setText("Failed to delete server item: " + parsedFile.name);
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> 
                        leftStatusLabel.setText("Delete error: " + e.getMessage()));
                }
            }).start();
        }
    }

    /**
     * Recursively deletes local files and directories
     * Helper method for handleDeleteLocal()
     * @param file The file or directory to delete
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteLocalFile(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteLocalFile(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    // ================= FILE DETAILS MANAGEMENT =================

    /**
     * Displays detailed information for selected local file
     * Updates the file details panel with name, size, type, and path
     * Manages button states based on file type and connection status
     */
    private void displayLocalFileDetails(String selectedFile) {
        if (selectedFile == null) {
            clearFileDetails();
            downloadButton.setDisable(true);
            uploadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
            deleteButton.setDisable(true);
            return;
        }
        
        // Handle parent directory display
        if (selectedFile.equals("[DIR] ..")) {
            fileNameLabel.setText("Parent Directory");
            fileSizeLabel.setText("-");
            fileTypeLabel.setText("Directory");
            lastModifiedLabel.setText("-");
            filePathLabel.setText("Go up one level");
            transferStatusLabel.setText("Double-click or press 'Go' to navigate");
            
            uploadButton.setDisable(true);
            downloadButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }
        
        // Extract filename from list format and get file details
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
                deleteButton.setDisable(false);
            } else {
                transferStatusLabel.setText("Ready to upload");
                uploadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
                deleteButton.setDisable(false);
            }
            
            downloadButton.setDisable(true); // Can't download to local from local
        }
    }

    /**
     * Displays detailed information for selected server file
     * Parses FTP directory listing format to extract file information
     * Updates file details panel and manages button states
     */
    private void displayServerFileDetails(String selectedFile) {
        if (selectedFile == null) {
            clearFileDetails();
            uploadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
            downloadButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }
        
        // Parse FTP directory listing to extract file details
        ParsedServerFile parsedFile = parseServerFileDetails(selectedFile);
        
        fileNameLabel.setText(parsedFile.name);
        fileSizeLabel.setText(parsedFile.size);
        fileTypeLabel.setText(parsedFile.type);
        lastModifiedLabel.setText(parsedFile.lastModified);
        filePathLabel.setText("Server: " + currentServerDirectory + "/" + parsedFile.name);
        
        if (parsedFile.isDirectory) {
            transferStatusLabel.setText("Double-click or press 'Go' to enter folder");
            downloadButton.setDisable(true); // Can't download directories
            deleteButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
        } else {
            transferStatusLabel.setText("Ready to download");
            downloadButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
            deleteButton.setDisable(ftpClient == null || !ftpClient.isLoggedIn());
        }
        
        uploadButton.setDisable(true); // Can't upload from server
    }
    
    /**
     * Clears all file detail labels and disables operation buttons
     * Called when no file is selected or during transitions
     */
    private void clearFileDetails() {
        fileNameLabel.setText("-");
        fileSizeLabel.setText("-");
        fileTypeLabel.setText("-");
        lastModifiedLabel.setText("-");
        filePathLabel.setText("-");
        transferStatusLabel.setText("No file selected");
        deleteButton.setDisable(true);
    }

    // ================= FTP LISTING PARSER =================

    /**
     * Helper class to store parsed FTP directory listing information
     * Encapsulates file details extracted from FTP LIST command output
     */
    private static class ParsedServerFile {
        String name;           // File/directory name
        String size;           // Formatted file size
        String type;           // File type description
        String lastModified;   // Last modification date
        boolean isDirectory;   // Directory flag
        
        ParsedServerFile(String name, String size, String type, String lastModified, boolean isDirectory) {
            this.name = name;
            this.size = size;
            this.type = type;
            this.lastModified = lastModified;
            this.isDirectory = isDirectory;
        }
    }
    
    /**
     * Parses FTP directory listing entries into structured data
     * Handles Unix-style listings (drwxr-xr-x format) and simple formats
     * Extracts filename, size, permissions, and modification date
     * @param ftpListing Raw FTP LIST command output line
     * @return ParsedServerFile object with extracted information
     */
    private ParsedServerFile parseServerFileDetails(String ftpListing) {
        try {
            // Example formats:
            // "drwxr-xr-x    2 user     group        4096 May 25 12:00 dirname"
            // "-rw-r--r--    1 user     group        1024 May 25 12:00 filename.txt"
            
            String[] parts = ftpListing.trim().split("\\s+");
            
            if (parts.length < 9) {
                // Fallback for simple listings that don't follow Unix format
                return new ParsedServerFile(ftpListing, "Unknown", "Unknown", "Unknown", 
                                          ftpListing.startsWith("d") || ftpListing.contains("<DIR>"));
            }
            
            // Parse permissions string (first character indicates type)
            String permissions = parts[0];
            boolean isDirectory = permissions.startsWith("d");
            
            // Parse file size (skip for directories)
            String size = isDirectory ? "-" : formatFileSize(Long.parseLong(parts[4]));
            
            // Parse date components (month, day, time/year)
            String month = parts.length > 5 ? parts[5] : "";
            String day = parts.length > 6 ? parts[6] : "";
            String timeOrYear = parts.length > 7 ? parts[7] : "";
            String lastModified = month + " " + day + " " + timeOrYear;
            
            // Parse filename (everything after the date - handles spaces in names)
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 8; i < parts.length; i++) {
                if (i > 8) nameBuilder.append(" ");
                nameBuilder.append(parts[i]);
            }
            String name = nameBuilder.toString();
            
            // Determine file type based on extension or directory flag
            String type = isDirectory ? "Directory" : getFileType(name);
            
            return new ParsedServerFile(name, size, type, lastModified, isDirectory);
            
        } catch (Exception e) {
            // Fallback parsing for unusual listing formats
            String name = extractDirectoryName(ftpListing);
            boolean isDir = ftpListing.startsWith("d") || ftpListing.contains("<DIR>");
            return new ParsedServerFile(name, "Unknown", isDir ? "Directory" : "File", 
                                      "Unknown", isDir);
        }
    }

    /**
     * Extracts directory name from FTP listing when standard parsing fails
     * Used as fallback method for unusual FTP server listing formats
     */
    private String extractDirectoryName(String selectedItem) {
        // Parse FTP directory listing format
        // Example: "drwxr-xr-x    2 user     group        4096 May 25 12:00 dirname"
        String[] parts = selectedItem.split("\\s+");
        if (parts.length > 8) {
            return parts[parts.length - 1]; // Last part is usually the directory name
        }
        return selectedItem; // Fallback to entire string
    }

    // ================= UTILITY METHODS =================

    /**
     * Formats file size in human-readable format
     * Converts bytes to KB, MB, or GB as appropriate
     * @param bytes File size in bytes
     * @return Formatted size string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Determines file type based on file extension
     * Returns user-friendly file type descriptions
     * @param fileName Name of the file with extension
     * @return File type description
     */
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

    /**
     * Formats timestamp into readable date string
     * @param timestamp Unix timestamp in milliseconds
     * @return Formatted date string (yyyy-MM-dd HH:mm:ss)
     */
    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    /**
     * Shows input dialog for new folder creation
     * @param title Dialog title text
     * @return User-entered folder name or null if cancelled
     */
    private String showNewFolderDialog(String title) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter folder name:");
        dialog.setContentText("Folder name:");
        
        // Style the dialog for better appearance
        dialog.getDialogPane().setPrefWidth(350);
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    // ================= MENU HANDLERS =================

    /**
     * Menu handler placeholders - implement file operations as needed
     */
    @FXML void handleNew() { /* Implement new file creation */ }
    @FXML void handleOpen() { /* Implement file opening */ }
    @FXML void handleSave() { /* Implement file saving */ }

    /**
     * Closes FTP connection (File -> Close menu)
     */
    @FXML
    void handleClose() {
        handleDisconnect();
    }

    /**
     * Exits application (File -> Quit menu)
     */
    @FXML
    void handleQuit() {
        handleDisconnect();
        System.exit(0);
    }

    /**
     * Shows application information dialog (Help -> About menu)
     */
    @FXML
    void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("FTP Client");
        alert.setContentText("A simple FTP client application for connecting to FTP servers");
        alert.showAndWait();
    }
}