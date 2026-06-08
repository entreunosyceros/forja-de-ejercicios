@echo off
setlocal EnableExtensions
set "RAIZ=%~dp0"
set "RAIZ=%RAIZ:~0,-1%"
set "FORJAEXAMENES_RAIZ=%RAIZ%"

set "JAR=%RAIZ%\web\target\forjaexamenes-web-1.0.0.jar"

if exist "%JAR%" (
    echo [*] Arrancando Forja de ejercicios - JAR
    echo     http://localhost:8080
    echo     Detener: Ctrl+C
    java -jar "%JAR%"
    exit /b %ERRORLEVEL%
)

set "MVN="
where mvn >nul 2>&1
if not errorlevel 1 set "MVN=mvn"

if not defined MVN (
    for /d %%D in ("%RAIZ%\tools\apache-maven-*") do (
        if exist "%%D\bin\mvn.cmd" set "MVN=%%D\bin\mvn.cmd"
    )
)

if not defined MVN (
    echo [X] No hay JAR compilado ni Maven en el PATH.
    echo     Ejecuta install.ps1 para compilar e instalar Maven.
    exit /b 1
)

echo [*] No hay JAR - arrancando con Maven spring-boot:run
set "WEB=%RAIZ%\web"
set "POM=%WEB%\pom.xml"
if not exist "%POM%" (
    echo [X] No se encontro pom.xml en carpeta web
    echo     Ruta esperada: %POM%
    exit /b 1
)
cd /d "%WEB%"
call "%MVN%" -f "%POM%" spring-boot:run
exit /b %ERRORLEVEL%
