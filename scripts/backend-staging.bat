@echo off
setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

cd /d "%PROJECT_ROOT%\backend"

set SPRING_PROFILES_ACTIVE=staging

if exist gradlew.bat (
  call gradlew.bat bootRun
  exit /b %errorlevel%
)

where gradle >nul 2>nul
if %errorlevel%==0 (
  gradle bootRun
  exit /b %errorlevel%
)

echo Backend cannot start yet because neither Gradle Wrapper nor gradle is available.
echo.
echo Options:
echo 1. Install Gradle, then run: scripts\backend-staging.bat
echo 2. Add Gradle Wrapper to this repo, then run: scripts\backend-staging.bat
echo 3. Start with Docker after Docker Desktop is running:
echo    set SPRING_PROFILES_ACTIVE=staging ^&^& docker compose up --build
exit /b 1
