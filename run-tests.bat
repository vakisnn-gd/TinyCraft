@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

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

mkdir out\test
if errorlevel 1 (
    echo Failed to create out\test.
    exit /b 1
)

javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
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

javac -encoding UTF-8 --release 8 -cp "out;lib/*" -d out\test tests\*Test.java
if errorlevel 1 (
    echo Test compilation failed.
    exit /b 1
)

java -cp "out\test;out;lib/*" org.junit.runner.JUnitCore !TEST_CLASSES!
if errorlevel 1 (
    echo Tests failed.
    exit /b 1
)

endlocal
exit /b 0
