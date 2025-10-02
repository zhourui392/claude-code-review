@echo off
echo Starting Git Review Service...
echo.
echo Please ensure Claude Code CLI is installed and accessible in your PATH
echo You can test it by running: claude --version
echo.

cd /d "%~dp0git-review-service"
java -jar target\git-review-service-1.0.0.jar

pause
