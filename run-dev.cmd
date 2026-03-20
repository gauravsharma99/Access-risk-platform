@echo off
setlocal
title Access Risk Monitoring Platform

echo ================================================
echo  Access Risk Monitoring Platform - DEV startup
echo ================================================
echo.

:: Use JDK 21 bundled with JetBrains PyCharm (quotes handle the spaces in path)
set "JAVA_HOME=C:\Program Files\JetBrains\PyCharm 2024.3\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Java version:
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo Starting app with H2 in-memory database (no PostgreSQL needed)...
echo.
echo Once started, open:
echo   Swagger UI  : http://localhost:8080/swagger-ui.html
echo   H2 Console  : http://localhost:8080/h2-console
echo.

call "%~dp0mvnw.cmd" spring-boot:run -Dspring-boot.run.profiles=dev

pause
