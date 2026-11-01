@echo off
rem Startet die Seminarverwaltung im Entwicklungsmodus. Beenden mit Strg+C.
cd /d "%~dp0"

if not defined JAVA_HOME (
    echo JAVA_HOME ist nicht gesetzt. Bitte JDK 21 installieren, z. B.:
    echo   winget install EclipseAdoptium.Temurin.21.JDK
    pause
    exit /b 1
)

echo.
echo   Oberflaeche:  http://localhost:8080
echo   API:          http://localhost:8080/api/seminare
echo   H2-Console:   http://localhost:8080/h2-console
echo.

rem clean, weil ein vorheriger Produktions-Build sonst den Entwicklungsmodus abschaltet
call "%~dp0mvnw.cmd" clean spring-boot:run
pause
