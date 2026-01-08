@ECHO OFF

@REM ---------------------------------------------------------------------------
@REM Copyright by Wolfgang Mueller-Haas
@REM ---------------------------------------------------------------------------

setlocal

@REM ---------------------------------------------------------------------------
@REM Initialize basic parameters
@REM ---------------------------------------------------------------------------

set USER_ARGS=%*
set "CURRENT_DIR=%cd%"
cd ..
set "PARENT_DIR=%cd%"
cd %CURRENT_DIR%

@REM ---------------------------------------------------------------------------
@REM Try to find CAR_OSGI_HOME
@REM ---------------------------------------------------------------------------

if exist "%CAR_OSGI_HOME%\bin\car.osgi.bat" goto :SETUP_JVM
if not "%CAR_OSGI_HOME%" == "" goto BAD_HOME
if exist "%CURRENT_DIR%\bin\car.osgi.bat" (
	set CAR_OSGI_HOME=%CURRENT_DIR%"
	goto :SETUP_JVM
)
if exist "%PARENT_DIR%\bin\car.osgi.bat" (
	set "CAR_OSGI_HOME=%PARENT_DIR%"
	goto :SETUP_JVM
)

@REM ---------------------------------------------------------------------------
:BAD_HOME
@REM ---------------------------------------------------------------------------

echo CAR_OSGI_HOME is set to %OSGI_HOME%
echo CAR_OSGI_HOME environment variable is not set or points to the wrong directory.
echo Please set CAR_OSGI_HOME correctly and launch CAR OSGi again.
set ERRORLEVEL=1

goto EXIT

@REM ---------------------------------------------------------------------------
:SETUP_JVM
@REM ---------------------------------------------------------------------------

set JAVA=java
if exist "%JAVA_HOME%\bin\java.exe" (
	set JAVA="%JAVA_HOME%\bin\java.exe"
	if exist "%JAVA_HOME%\bin\server\jvm.dll" (
		set "JAVA_OPTS=-server"
	)
)
set JAVA_OPTS=%JAVA_OPTS% -Xms32M -Xmx128M
set JAVA_OPTS=%JAVA_OPTS% --add-modules=ALL-SYSTEM
set JAVA_OPTS=%JAVA_OPTS% --add-opens=java.base/java.lang=ALL-UNNAMED
set JAVA_OPTS=%JAVA_OPTS% --add-opens=java.base/java.util=ALL-UNNAMED
set JAVA_OPTS=%JAVA_OPTS% --add-opens=java.base/java.time=ALL-UNNAMED
set JAVA_OPTS=%JAVA_OPTS% --add-opens=java.base/java.nio=ALL-UNNAMED

if "%1" == "debug" (
   set JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
   shift)

@REM ---------------------------------------------------------------------------
@REM Setup Classpath
@REM ---------------------------------------------------------------------------

set "CLASSPATH=lib\*"
set "CLASSPATH=%CLASSPATH%;configuration"

@REM ---------------------------------------------------------------------------
@REM Set user vars
@REM ---------------------------------------------------------------------------

@REM set USER_ARGS=%*

@REM ---------------------------------------------------------------------------
@REM LAUNCH CAR OSGi
@REM ---------------------------------------------------------------------------

echo CAR_OSGI_HOME  is set to %CAR_OSGI_HOME%
echo JAVA           is set to %JAVA%
echo JAVA_OPTS      is set to %JAVA_OPTS%
echo CLASSPATH      is set to %CLASSPATH%
echo USER_ARGS      is set to %USER_ARGS%
echo:

cd %CAR_OSGI_HOME%

@REM ---------------------------------------------------------------------------
:LAUNCH
@REM ---------------------------------------------------------------------------

%JAVA% %JAVA_OPTS% -cp %CLASSPATH% biz.car.osgi.Main %USER_ARGS%

@REM ---------------------------------------------------------------------------
:EXIT
@REM ---------------------------------------------------------------------------
cd %CURRENT_DIR%
endlocal

exit /b %ERRORLEVEL%