@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "MAIN_OUT=out-check"
set "TEST_OUT=out-check-test"

if exist "%MAIN_OUT%" (
    rmdir /s /q "%MAIN_OUT%"
    if errorlevel 1 (
        echo Failed to remove %MAIN_OUT%.
        exit /b 1
    )
)

if exist "%TEST_OUT%" (
    rmdir /s /q "%TEST_OUT%"
    if errorlevel 1 (
        echo Failed to remove %TEST_OUT%.
        exit /b 1
    )
)

mkdir "%MAIN_OUT%"
if errorlevel 1 (
    echo Failed to create %MAIN_OUT%.
    exit /b 1
)

mkdir "%TEST_OUT%"
if errorlevel 1 (
    echo Failed to create %TEST_OUT%.
    exit /b 1
)

javac -encoding UTF-8 --release 8 -cp "lib/*" -d "%MAIN_OUT%" *.java
if errorlevel 1 (
    echo Production compilation failed.
    exit /b 1
)

set "TEST_CLASSES="
set /a TEST_COUNT=0
for %%F in (tests\*Test.java) do (
    set /a TEST_COUNT+=1
    set "TEST_CLASSES=!TEST_CLASSES! %%~nF"
)

if !TEST_COUNT! equ 0 (
    echo No test classes found.
    exit /b 1
)

echo Found !TEST_COUNT! test classes:
for %%C in (!TEST_CLASSES!) do echo   %%C

javac -encoding UTF-8 --release 8 -cp "%MAIN_OUT%;lib/*" -d "%TEST_OUT%" tests\*Test.java
if errorlevel 1 (
    echo Test compilation failed.
    exit /b 1
)

java -cp "%TEST_OUT%;%MAIN_OUT%;lib/*" org.junit.runner.JUnitCore !TEST_CLASSES!
if errorlevel 1 (
    echo Tests failed.
    exit /b 1
)

endlocal
exit /b 0
