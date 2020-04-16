set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_HOME=%DIRNAME%..
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

java -Dnashorn.args="--no-deprecation-warning" %JAVA_OPTS% -cp "%APP_HOME%\*;%APP_HOME%\lib\*" net.adamcin.oakpal.cli.Main %*
