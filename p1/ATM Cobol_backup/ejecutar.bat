@echo off
rem Lanzador del cajero UnizarBank (doble clic).
rem Se ejecuta siempre desde la carpeta del .bat para encontrar
rem las DLL y los ficheros .ubd.
cd /d "%~dp0"

if "%~1"=="run" goto run

rem En Windows 11 la terminal por defecto (Windows Terminal) ignora
rem "mode con", asi que se abre una consola clasica (conhost) que
rem si respeta el tamano 80x25. Si no existe, se ejecuta aqui mismo.
start "UnizarBank" conhost.exe cmd.exe /c ""%~f0" run" && exit /b

:run
title Cajero Automatico UnizarBank
mode con: cols=80 lines=25
BANK1.exe
