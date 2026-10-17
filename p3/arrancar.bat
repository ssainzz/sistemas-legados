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

rem Java 17+ (el "java" del PATH puede ser un Java 8 antiguo). Se busca un JDK conocido.
set "JAVA=java"
for /d %%J in ("%ProgramFiles%\OpenJDK\jdk-*" "%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Java\jdk-*" "%ProgramFiles%\Microsoft\jdk-*") do (
  if exist "%%~J\bin\java.exe" set "JAVA=%%~J\bin\java.exe"
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" if "%JAVA%"=="java" set "JAVA=%JAVA_HOME%\bin\java.exe"
echo Usando Java: %JAVA%

start "" /d "%~dp0Database-MSDOS" database.bat
echo Esperando a que arranque DOSBox...
timeout /t 6 /nobreak >nul

start "" cmd /c "timeout /t 3 /nobreak >nul & start http://localhost:8090/"
"%JAVA%" -jar "target\wrapper-legacy.jar"
pause
