@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo    FTP Client Build and Launch
echo    (JavaFX SDK 17.0.15)
echo ========================================
echo.

cd /d "%~dp0"

REM Clean previous build
echo [1/4] Cleaning previous build...
if exist "bin" rmdir /s /q "bin" 2>nul
mkdir "bin" 2>nul

REM Compile Java sources
echo [2/4] Compiling FTP Client...
javac --module-path "javafx-sdk-17.0.15\lib" --add-modules javafx.controls,javafx.fxml ^
      -cp "src" -d "bin" ^
      src\com\ftpclient\Main.java ^
      src\com\ftpclient\gui\MainController.java ^
      src\com\ftpclient\ftp\*.java

if !errorlevel! neq 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

REM Copy resources
echo [3/4] Copying resources...
if not exist "bin\com\ftpclient\gui" mkdir "bin\com\ftpclient\gui"
copy "src\com\ftpclient\gui\MainScreen.fxml" "bin\com\ftpclient\gui\" >nul

REM Launch application
echo [4/4] Starting FTP Client...
echo.

echo Attempting GUI with JavaFX SDK 17.0.15...
java --module-path "javafx-sdk-17.0.15\lib" --add-modules javafx.controls,javafx.fxml -Dprism.order=sw -cp "bin" com.ftpclient.Main

if !errorlevel! neq 0 (
    echo GUI failed. Starting CLI...
    echo.
    java -cp "bin" com.ftpclient.ftp.CLI
)

pause