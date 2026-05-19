@echo off
setlocal
cd /d "%~dp0"

set "NAME=TinyCraft-v0.2-final-windows"
set "RELEASE_DIR=release"
set "DEST=%RELEASE_DIR%\%NAME%"
set "ZIP=%RELEASE_DIR%\%NAME%.zip"

if not exist "%RELEASE_DIR%" mkdir "%RELEASE_DIR%"
if exist "%DEST%" rmdir /s /q "%DEST%"
if exist "%ZIP%" del /q "%ZIP%"

echo Building TinyCraft v0.2 final...
if not exist out mkdir out
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

mkdir "%DEST%"
mkdir "%DEST%\out"
mkdir "%DEST%\lib"

xcopy /E /I /Y out "%DEST%\out" >nul
xcopy /E /I /Y lib "%DEST%\lib" >nul
if exist assets xcopy /E /I /Y assets "%DEST%\assets" >nul
if exist sounds xcopy /E /I /Y sounds "%DEST%\sounds" >nul
if exist docs xcopy /E /I /Y docs "%DEST%\docs" >nul

copy /Y run-game.bat "%DEST%\run-game.bat" >nul
copy /Y run-server.bat "%DEST%\run-server.bat" >nul
copy /Y README.md "%DEST%\README.md" >nul
copy /Y FAQ.md "%DEST%\FAQ.md" >nul
copy /Y KNOWN_ISSUES.md "%DEST%\KNOWN_ISSUES.md" >nul
copy /Y LICENSE "%DEST%\LICENSE" >nul
copy /Y terrain.png "%DEST%\terrain.png" >nul
copy /Y ChunkShader.vsh "%DEST%\ChunkShader.vsh" >nul
copy /Y ChunkShader.fsh "%DEST%\ChunkShader.fsh" >nul

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
