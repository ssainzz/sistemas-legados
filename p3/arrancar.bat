@echo off
rem ---- Arranque "doble clic" ----
rem 1) Lanza la aplicacion legada en DOSBox (visible: Robot necesita verla y darle el foco), con la misma orden que
rem    Database-MSDOS\database.bat mas la configuracion propia dosbox-wrapper.conf (ventana fija de 640x400).
rem 2) Arranca el servidor web, que espera a que aparezca DOSBox y abre el navegador cuando esta listo.
rem No se modifica nada de Database-MSDOS.
setlocal
cd /d "%~dp0"

if not exist "target\wrapper-legacy.jar" (
  echo Falta target\wrapper-legacy.jar. Compila antes con:  mvn package
  pause
  exit /b 1
)

rem Java 17+ (el "java" del PATH puede ser un Java 8 antiguo). Se busca un JDK conocido.
set "JAVA=java"
for /d %%J in ("%ProgramFiles%\OpenJDK\jdk-*" "%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Java\jdk-*" "%ProgramFiles%\Microsoft\jdk-*") do (
  if exist "%%~J\bin\java.exe" set "JAVA=%%~J\bin\java.exe"
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" if "%JAVA%"=="java" set "JAVA=%JAVA_HOME%\bin\java.exe"
echo Usando Java: %JAVA%

rem Misma orden que database.bat (desde su carpeta) + -conf. Sin -conf, DOSBox usaria la configuracion del usuario.
start "" /d "%~dp0Database-MSDOS" "DOSBox-0.74\DOSBox.exe" Database\gwbasic.bat -noconsole -conf "%~dp0dosbox-wrapper.conf"

rem --abrir: el servidor abre el navegador en cuanto esta escuchando. No hace falta esperar a DOSBox aqui: el wrapper
rem lo espera en la primera operacion.
"%JAVA%" -jar "target\wrapper-legacy.jar" servidor --abrir
pause
