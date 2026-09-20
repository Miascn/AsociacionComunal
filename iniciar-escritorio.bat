@echo off
title Asociacion Comunal ERP - Frontend
cd /d "%~dp0frontend"
echo ===================================================
echo Iniciando Asociacion Comunal ERP (JavaFX)...
echo ===================================================
mvn javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error al ejecutar la aplicacion.
    pause
)
