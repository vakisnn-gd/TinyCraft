@echo off
setlocal
cd /d "%~dp0"

set "NAME=TinyCraft-v0.2.1-windows"
set "RELEASE_DIR=release"
set "DEST=%RELEASE_DIR%\%NAME%"
set "ZIP=%RELEASE_DIR%\%NAME%.zip"
set "JLINK_EXE="

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jlink.exe" set "JLINK_EXE=%JAVA_HOME%\bin\jlink.exe"
for /d %%D in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21*") do if exist "%%~fD\bin\jlink.exe" set "JLINK_EXE=%%~fD\bin\jlink.exe"
for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do if exist "%%~fD\bin\jlink.exe" set "JLINK_EXE=%%~fD\bin\jlink.exe"
for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do if exist "%%~fD\bin\jlink.exe" set "JLINK_EXE=%%~fD\bin\jlink.exe"
for /d %%D in ("%ProgramFiles%\Microsoft\jdk-21*") do if exist "%%~fD\bin\jlink.exe" set "JLINK_EXE=%%~fD\bin\jlink.exe"
if not defined JLINK_EXE for %%J in (jlink.exe) do if not "%%~$PATH:J"=="" set "JLINK_EXE=%%~$PATH:J"

if not defined JLINK_EXE (
    echo JDK 21 with jlink was not found.
    echo Install JDK 21 or set JAVA_HOME to its folder.
    exit /b 1
)

for /f "tokens=1 delims=." %%V in ('"%JLINK_EXE%" --version') do set "JLINK_MAJOR=%%V"
if not "%JLINK_MAJOR%"=="21" (
    echo TinyCraft release requires JDK 21, but jlink %JLINK_MAJOR% was found.
    echo Set JAVA_HOME to a JDK 21 folder and try again.
    exit /b 1
)

echo Running tests...
call run-tests.bat
if errorlevel 1 (
    echo Tests failed. Release build stopped.
    exit /b 1
)

echo Building TinyCraft v0.2.1...
if exist out (
    rmdir /s /q out
    if errorlevel 1 (
        echo Failed to remove out.
        exit /b 1
    )
)

mkdir out
if errorlevel 1 (
    echo Failed to create out.
    exit /b 1
)

javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

if not exist "%RELEASE_DIR%" (
    mkdir "%RELEASE_DIR%"
    if errorlevel 1 (
        echo Failed to create %RELEASE_DIR%.
        exit /b 1
    )
)
if exist "%DEST%" (
    rmdir /s /q "%DEST%"
    if errorlevel 1 (
        echo Failed to remove %DEST%.
        exit /b 1
    )
)
if exist "%ZIP%" (
    del /q "%ZIP%"
    if errorlevel 1 (
        echo Failed to remove %ZIP%.
        exit /b 1
    )
)

mkdir "%DEST%"
if errorlevel 1 goto :copy_failed
mkdir "%DEST%\out"
if errorlevel 1 goto :copy_failed
mkdir "%DEST%\lib"
if errorlevel 1 goto :copy_failed

echo Building bundled Java 21 runtime...
"%JLINK_EXE%" --add-modules java.base,java.desktop,jdk.unsupported --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output "%DEST%\runtime"
if errorlevel 1 (
    echo Failed to build bundled Java runtime.
    exit /b 1
)

xcopy /E /I /Y out "%DEST%\out" >nul
if errorlevel 1 goto :copy_failed
xcopy /E /I /Y lib "%DEST%\lib" >nul
if errorlevel 1 goto :copy_failed
xcopy /E /I /Y assets "%DEST%\assets" >nul
if errorlevel 1 goto :copy_failed
xcopy /E /I /Y sounds "%DEST%\sounds" >nul
if errorlevel 1 goto :copy_failed
xcopy /E /I /Y docs "%DEST%\docs" >nul
if errorlevel 1 goto :copy_failed

copy /Y run-game.bat "%DEST%\run-game.bat" >nul
if errorlevel 1 goto :copy_failed
copy /Y run-server.bat "%DEST%\run-server.bat" >nul
if errorlevel 1 goto :copy_failed
copy /Y README.md "%DEST%\README.md" >nul
if errorlevel 1 goto :copy_failed
copy /Y FAQ.md "%DEST%\FAQ.md" >nul
if errorlevel 1 goto :copy_failed
copy /Y KNOWN_ISSUES.md "%DEST%\KNOWN_ISSUES.md" >nul
if errorlevel 1 goto :copy_failed
copy /Y LICENSE "%DEST%\LICENSE" >nul
if errorlevel 1 goto :copy_failed
copy /Y terrain.png "%DEST%\terrain.png" >nul
if errorlevel 1 goto :copy_failed
copy /Y ChunkShader.vsh "%DEST%\ChunkShader.vsh" >nul
if errorlevel 1 goto :copy_failed
copy /Y ChunkShader.fsh "%DEST%\ChunkShader.fsh" >nul
if errorlevel 1 goto :copy_failed

del /q "%DEST%\lib\junit-*.jar" 2>nul
del /q "%DEST%\lib\hamcrest-*.jar" 2>nul

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%DEST%\*' -DestinationPath '%ZIP%' -Force"
if errorlevel 1 (
    echo Zip creation failed, but release folder exists: %DEST%
    exit /b 1
)

echo Done: %DEST%
echo Zip:  %ZIP%
endlocal
exit /b 0

:copy_failed
echo Failed to prepare release files.
exit /b 1
