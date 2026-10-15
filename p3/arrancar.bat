@echo off
rem ---- Arranque "doble clic" (ESQUELETO) ----
rem 1) Lanza la aplicacion legada en DOSBox (visible: Robot necesita verla y darle el foco).
rem 2) Espera a que cargue.  3) Arranca el servidor web y abre el navegador.
rem No se modifica nada de Database-MSDOS.
setlocal
cd /d "%~dp0"

if not exist "target\wrapper-legacy.jar" (
  echo Falta target\wrapper-legacy.jar. Compila antes con:  mvn package
  pause
  exit /b 1
)

start "" /d "%~dp0Database-MSDOS" database.bat
echo Esperando a que arranque DOSBox...
timeout /t 6 /nobreak >nul

start "" cmd /c "timeout /t 3 /nobreak >nul & start http://localhost:8080/"
java -jar "target\wrapper-legacy.jar"
pause
