@echo off
rem ---- Arranque con doble clic ----
rem 1) Busca Java 17+ y comprueba que el puerto está libre, antes de abrir nada.
rem 2) Abre DOSBox con la aplicación y dosbox-wrapper.conf (ventana fija de 640x400).
rem 3) Arranca el servidor, que espera a DOSBox y abre el navegador.
rem No se modifica nada de Database-MSDOS.
setlocal
cd /d "%~dp0"

if not exist "target\wrapper-legacy.jar" (
  echo Falta target\wrapper-legacy.jar. Compila antes con:  mvn package
  pause
  exit /b 1
)

rem ---- Java 17+ ----
rem El java del PATH puede ser un Java 8, con el que el .jar no arranca. Se usa el primero de versión 17 o mayor
rem entre JAVA_HOME, Program Files y el PATH.
set "JAVA="
if defined JAVA_HOME call :probar_java "%JAVA_HOME%\bin\java.exe"
for /d %%J in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\jdk-*" "%ProgramFiles%\OpenJDK\*" "%ProgramFiles%\Zulu\*" "%ProgramFiles%\Amazon Corretto\*" "%ProgramFiles%\BellSoft\*") do (
  call :probar_java "%%~J\bin\java.exe"
)
for /f "delims=" %%J in ('where java 2^>nul') do call :probar_java "%%~J"

if not defined JAVA (
  echo.
  echo ERROR: no se ha encontrado Java 17 o superior, y la practica lo necesita.
  if defined VIEJO echo        Solo hay un Java mas antiguo ^(version %VIEJO%^): %VIEJO_EXE%
  echo   1. Instala un JDK 17 o superior, por ejemplo desde https://adoptium.net/
  echo   2. Vuelve a hacer doble clic en arrancar.bat. Si lo has instalado en otra carpeta,
  echo      define JAVA_HOME con la carpeta del JDK.
  echo.
  pause
  exit /b 1
)
echo Usando Java %JAVA_VER%: %JAVA%

rem ---- Puerto libre ----
rem server.port de config.properties (8090 por defecto). En netstat, un puerto en escucha tiene como dirección remota
rem 0.0.0.0:0 o [::]:0 en cualquier idioma de Windows.
set "PORT=8090"
for /f "tokens=2 delims==" %%P in ('findstr /b /c:"server.port=" config.properties 2^>nul') do set "PORT=%%P"
set "PORT=%PORT: =%"
set "PID="
for /f "tokens=5" %%A in ('netstat -ano -p TCP ^| findstr /r /c:":%PORT% *0\.0\.0\.0:0 "') do set "PID=%%A"
if not defined PID for /f "tokens=5" %%A in ('netstat -ano -p TCPv6 ^| findstr /r /c:"\]:%PORT% *\[::\]:0 "') do set "PID=%%A"
if defined PID (
  set "PROG=desconocido"
  for /f "tokens=1 delims=," %%N in ('tasklist /fi "PID eq %PID%" /fo csv /nh 2^>nul') do set "PROG=%%~N"
  call :puerto_ocupado
  pause
  exit /b 1
)

rem La misma orden que database.bat, más -conf (si no, DOSBox usaría la configuración del usuario).
start "" /d "%~dp0Database-MSDOS" "DOSBox-0.74\DOSBox.exe" Database\gwbasic.bat -noconsole -conf "%~dp0dosbox-wrapper.conf"

rem --abrir: el servidor abre el navegador cuando ya escucha. A DOSBox lo espera el propio wrapper.
"%JAVA%" -jar "target\wrapper-legacy.jar" servidor --abrir
pause
exit /b


rem Elige %1 si es un java.exe de versión 17 o mayor y aún no hay ninguno. Si es más antiguo, lo guarda en VIEJO.
:probar_java
if defined JAVA exit /b
if not exist "%~1" exit /b
set "VER="
for /f "tokens=2 delims==" %%V in ('call "%~1" -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.specification.version"') do set "VER=%%V"
if not defined VER exit /b
set "VER=%VER: =%"
rem Hasta Java 8 la versión es 1.8, 1.7...; desde Java 9, un número entero.
if "%VER:~0,2%"=="1." goto :java_viejo
set "NUM=0"
set /a NUM=%VER% 2>nul
if %NUM% LSS 17 goto :java_viejo
rem %%~fI limpia la ruta (JAVA_HOME puede acabar en "\").
for %%I in ("%~1") do set "JAVA=%%~fI"
set "JAVA_VER=%VER%"
exit /b
:java_viejo
if not defined VIEJO (
  set "VIEJO=%VER%"
  set "VIEJO_EXE=%~1"
)
exit /b

:puerto_ocupado
echo.
echo ERROR: el puerto %PORT% ya lo está usando otro programa (%PROG%, PID %PID%) y el servidor no puede arrancar.
echo   - Si es otra ventana de arrancar.bat ^(java.exe^), usa esa: http://localhost:%PORT%/
echo     o ciérrala antes de volver a arrancar.
echo   - Si no, abre config.properties, cambia server.port=%PORT% por otro puerto libre
echo     (por ejemplo 8091) y vuelve a hacer doble clic en arrancar.bat.
echo No se ha abierto DOSBox.
echo.
exit /b
