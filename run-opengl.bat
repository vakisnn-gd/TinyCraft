@echo off
setlocal
cd /d "%~dp0"

set "JDK_HOME="
if defined TINYCRAFT_JDK_HOME if exist "%TINYCRAFT_JDK_HOME%\bin\javac.exe" set "JDK_HOME=%TINYCRAFT_JDK_HOME%"
if not defined JDK_HOME for /d %%D in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21*") do if exist "%%~fD\bin\javac.exe" set "JDK_HOME=%%~fD"
if not defined JDK_HOME for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do if exist "%%~fD\bin\javac.exe" set "JDK_HOME=%%~fD"
if not defined JDK_HOME for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do if exist "%%~fD\bin\javac.exe" set "JDK_HOME=%%~fD"
if not defined JDK_HOME for /d %%D in ("%ProgramFiles%\Microsoft\jdk-21*") do if exist "%%~fD\bin\javac.exe" set "JDK_HOME=%%~fD"
if not defined JDK_HOME if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_HOME=%JAVA_HOME%"

set "JAVA_EXE=java"
set "JAVAC_EXE=javac"
if defined JDK_HOME (
    set "JAVA_EXE=%JDK_HOME%\bin\java.exe"
    set "JAVAC_EXE=%JDK_HOME%\bin\javac.exe"
)

if not exist out mkdir out

"%JAVAC_EXE%" -Xlint:-options -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)

set "JAVA_FLAGS=-Dfile.encoding=UTF-8"

"%JAVA_EXE%" %JAVA_FLAGS% -cp "out;lib/*" TinyCraft
endlocal
