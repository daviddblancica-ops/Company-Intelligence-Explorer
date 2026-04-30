@echo off
setlocal

set MAVEN_VERSION=3.9.6
set MAVEN_DIR=%~dp0.mvn-local\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%~dp0.mvn-local\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  if not exist "%~dp0.mvn-local" mkdir "%~dp0.mvn-local"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%~dp0.mvn-local' -Force"
)

call "%MAVEN_DIR%\bin\mvn.cmd" %*
endlocal
