
@echo off
if not exist out mkdir out

javac -encoding UTF-8 -cp "lib\ojdbc17.jar" -d out src\com\hcpms\*.java

if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)

java -cp "out;lib\ojdbc17.jar" com.hcpms.Main

pause