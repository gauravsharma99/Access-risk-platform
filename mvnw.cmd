@echo off
setlocal

set ERROR_CODE=0

:: -----------------------------------------------------------------------
:: Locate JAVA_HOME — use env var if set, otherwise search PATH
:: -----------------------------------------------------------------------
if not "%JAVA_HOME%"=="" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    goto checkJava
)

for %%i in (java.exe) do set "JAVA_EXE=%%~$PATH:i"
if not "%JAVA_EXE%"=="" goto checkJava

echo.
echo Error: Java not found. Set JAVA_HOME or add java to PATH.
goto error

:checkJava
if not exist "%JAVA_EXE%" (
    echo.
    echo Error: java.exe not found at "%JAVA_EXE%"
    goto error
)

:: -----------------------------------------------------------------------
:: Locate project base directory (directory containing .mvn folder)
:: -----------------------------------------------------------------------
set "MAVEN_PROJECTBASEDIR=%CD%"

:findBaseDir
if exist "%MAVEN_PROJECTBASEDIR%\.mvn" goto baseDirFound
cd ..
if "%MAVEN_PROJECTBASEDIR%"=="%CD%" goto baseDirNotFound
set "MAVEN_PROJECTBASEDIR=%CD%"
goto findBaseDir

:baseDirNotFound
set "MAVEN_PROJECTBASEDIR=%CD%"

:baseDirFound
cd "%MAVEN_PROJECTBASEDIR%"

:: -----------------------------------------------------------------------
:: Download maven-wrapper.jar if not present
:: -----------------------------------------------------------------------
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven wrapper jar...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%WRAPPER_JAR%')"
    if not exist "%WRAPPER_JAR%" (
        echo Error: Failed to download maven-wrapper.jar
        goto error
    )
    echo Download complete.
)

:: -----------------------------------------------------------------------
:: Launch Maven via the wrapper
:: -----------------------------------------------------------------------
"%JAVA_EXE%" ^
  %MAVEN_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
endlocal & exit /B %ERROR_CODE%
