@echo off
setlocal enableDelayedExpansion
set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%
set wrapperJar=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
set wrapperProps=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties

if not exist "%wrapperJar%" (
  echo Maven Wrapper jar not found in %wrapperJar%
  exit /b 1
)

if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java
)

"%JAVA_EXE%" -cp "%wrapperJar%" org.apache.maven.wrapper.BootstrapMainStarter %*
exit /b %ERROR_CODE%