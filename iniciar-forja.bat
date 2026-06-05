@echo off
setlocal
set "RAIZ=%~dp0"
set "RAIZ=%RAIZ:~0,-1%"
set "FORJAEXAMENES_RAIZ=%RAIZ%"

set "JAR=%RAIZ%\web\target\forjaexamenes-web-1.0.0.jar"

if exist "%JAR%" (
    echo → Arrancando Forja de ejercicios ^(JAR^)…
    echo   http://localhost:8080
    echo   Detener: Ctrl+C
    java -jar "%JAR%"
    exit /b %ERRORLEVEL%
)

echo → No hay JAR compilado; usando Maven ^(ejecuta install.ps1 para compilar^).
cd /d "%RAIZ%\web"
mvn spring-boot:run
