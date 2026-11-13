@echo off
setlocal
set "APP_HOME=%~dp0"
set "GRADLE_VERSION=8.11.1"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "DIST_ROOT=%GRADLE_USER_HOME%\rivan-wrapper"
set "GRADLE_HOME=%DIST_ROOT%\gradle-%GRADLE_VERSION%\gradle-%GRADLE_VERSION%"
set "GRADLE_EXE=%GRADLE_HOME%\bin\gradle.bat"
set "ZIP_FILE=%DIST_ROOT%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_EXE%" (
  if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
  echo Downloading Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_FILE%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP_FILE%' -DestinationPath '%DIST_ROOT%\gradle-%GRADLE_VERSION%' -Force"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_EXE%" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
