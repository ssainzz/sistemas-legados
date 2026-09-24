@echo off
rem Lanzador del cajero UnizarBank (doble clic).
rem Se situa en la carpeta del .bat para encontrar las DLL y los .ubd.
cd /d "%~dp0"

if not exist BANK1.exe (
    echo No se encuentra BANK1.exe en esta carpeta. Compile antes con:
    echo     cobc -x BANK1.cbl
    pause
    exit /b 1
)

rem Windows Terminal (Windows 11) ignora "mode con", por eso se abre
rem una consola clasica (conhost) que si respeta el tamano 80x25.
rem Si el programa termina con error, la ventana espera para poder
rem leer el mensaje.
start "Cajero UnizarBank" conhost.exe cmd.exe /c "mode con: cols=80 lines=25 & BANK1.exe & if errorlevel 1 pause"
if errorlevel 1 (
    mode con: cols=80 lines=25
    BANK1.exe
    if errorlevel 1 pause
)
