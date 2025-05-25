# FTP Client

A JavaFX-based FTP client application for connecting to FTP servers and transferring files.
   By:
      *Dương Võ Thiên Bảo  - 10422015*
      *Lê Quang Kiên       - 10422040*
      *Mai Thị Yến Nhi     - 10422061*

## Features

- Connect to FTP servers with username/password authentication
- Dual-pane interface (local files + remote server files)
- Browse directories on both local system and FTP server
- Upload files from local to server
- Download files from server to local
- Create and delete folders (empty folders only)
- File details panel showing size, type, and modification date

## Prerequisites

- Java 11 or higher
- JavaFX SDK 17.0.15 (included in project)

## Project Structure

```
FTP_Client/
├── src/
│   └── com/ftpclient/
│       ├── Main.java                 # Application entry point
│       ├── ftp/                      # FTP protocol implementation
│       │   ├── FTPClient.java
│       │   ├── FTPCommands.java
│       │   └── FTPResponse.java
│       └── gui/                      # JavaFX user interface
│           ├── MainController.java
│           └── MainScreen.fxml
├── javafx-sdk-17.0.15/              # JavaFX libraries
└── run.bat                          # Build and run script
```

## How to Run

### Option 1: Using the run script (Windows)
- Double-click `run.bat` in File Explorer
- Or run `run.bat` from Command Prompt in the project directory

### Option 2: Manual compilation and execution
```bash
# Compile
javac --module-path "javafx-sdk-17.0.15\lib" --add-modules javafx.controls,javafx.fxml -cp "src" -d "bin" src\com\ftpclient\*.java src\com\ftpclient\ftp\*.java src\com\ftpclient\gui\*.java

# Run
java --module-path "javafx-sdk-17.0.15\lib" --add-modules javafx.controls,javafx.fxml -cp "bin" com.ftpclient.Main
```

## Usage

1. **Connect to FTP Server:**
   - Enter server address, port (default: 21), username, and password
   - Click "Connect"

2. **Navigate Directories:**
   - Double-click folders to enter them
   - Use "Up" buttons to go to parent directories
   - Type paths manually in the directory fields

3. **Transfer Files:**
   - Select a local file and click "Upload" to send to server
   - Select a server file and click "Download" to save locally

4. **Manage Folders:**
   - Use "New Folder" buttons to create directories
   - Select folders and click "Delete" to remove empty folders

## Notes

- Only empty folders can be deleted (FTP protocol limitation)
- File transfers run in background threads to prevent UI freezing
- Connection status is displayed in the status labels