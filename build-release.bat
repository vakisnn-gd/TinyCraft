@echo off
setlocal
cd /d "%~dp0"

set "NAME=TinyCraft-v0.2-final-windows"
set "RELEASE_DIR=release"
set "DEST=%RELEASE_DIR%\%NAME%"
set "ZIP=%RELEASE_DIR%\%NAME%.zip"

echo Running tests...
call run-tests.bat
if errorlevel 1 (
    echo Tests failed. Release build stopped.
    exit /b 1
)

echo Building TinyCraft v0.2 final...
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
