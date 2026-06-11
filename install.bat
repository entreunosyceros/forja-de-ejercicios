@echo off
setlocal EnableExtensions
set "RAIZ=%~dp0"
set "RAIZ=%RAIZ:~0,-1%"
cd /d "%RAIZ%"

echo [*] Instalador Forja de ejercicios (Windows)
echo     No hace falta Set-ExecutionPolicy: este .bat lo aplica solo para esta ejecucion.
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%RAIZ%\install.ps1" %*
exit /b %ERRORLEVEL%
