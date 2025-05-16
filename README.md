# FTP Client

A simple, JavaFX-based FTP client application for connecting to FTP servers, browsing directories, and transferring files.

## Current State

This project is under active development. However, there is suggested switching to JavaSwing for the GUI. If this is through, please modify the MainController.java file to use JavaSwing components instead of JavaFX.

## Setup Instructions

### Prerequisites

- Java 11 or higher
- JavaFX SDK (added to lib folder)

## Project Structure

```
FTP_Client/
├── src/
│   └── com/
│       ├── ftpclient/
│       │   ├── Main.java                  # Application entry point
│       │   ├── ftp/
│       │   │   ├── FTPClient.java         # Core FTP implementation
│       │   │   ├── FTPCommands.java       # FTP command constants
│       │   │   └── FTPResponse.java       # FTP response handling
│       │   └── gui/
│       │       └── MainController.java    # JavaFX UI controller
├── lib/                                  # JavaFX libraries
└── .vscode/                              # VS Code configuration
```

## Usage

Currently, the application is in development and not fully functional. When complete, it will allow:
- Connecting to FTP servers using host, port, username, and password
- Browsing remote directories
- Downloading and uploading files
