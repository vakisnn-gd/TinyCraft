@echo off
setlocal
cd /d "%~dp0"

set "JAVA_EXE="
if exist "%~dp0runtime\bin\java.exe" set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not defined JAVA_EXE if defined TINYCRAFT_JAVA_HOME if exist "%TINYCRAFT_JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%TINYCRAFT_JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21*") do if exist "%%~fD\bin\java.exe" set "JAVA_EXE=%%~fD\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do if exist "%%~fD\bin\java.exe" set "JAVA_EXE=%%~fD\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do if exist "%%~fD\bin\java.exe" set "JAVA_EXE=%%~fD\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\Microsoft\jdk-21*") do if exist "%%~fD\bin\java.exe" set "JAVA_EXE=%%~fD\bin\java.exe"
if not defined JAVA_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE for %%J in (java.exe) do if not "%%~$PATH:J"=="" set "JAVA_EXE=%%~$PATH:J"

if not defined JAVA_EXE (
    echo Java runtime was not found.
    echo Reinstall TinyCraft with its bundled runtime folder or install Java 21.
    pause
    exit /b 1
)

if not exist out\TinyCraft.class (
    echo Compiled game files were not found.
    echo Reinstall TinyCraft or rebuild the release package.
    pause
    exit /b 1
)

set "JAVA_FLAGS=-Dfile.encoding=UTF-8"

"%JAVA_EXE%" %JAVA_FLAGS% -cp "out;lib/*" TinyCraft
if errorlevel 1 (
    echo.
    echo TinyCraft stopped with an error.
    pause
    exit /b 1
)
endlocal
