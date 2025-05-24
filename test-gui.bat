@echo off
cd /d "%~dp0"

echo Testing JavaFX GUI with complete SDK...
java --module-path "javafx-sdk-17.0.15\lib" --add-modules javafx.controls,javafx.fxml -Dprism.verbose=true -cp "bin" com.ftpclient.Main

echo.
echo If GUI failed, testing CLI...
java -cp "bin" com.ftpclient.ftp.CLI

pause