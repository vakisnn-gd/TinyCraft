@echo off
setlocal
if not exist out mkdir out
if not exist out\test mkdir out\test
javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
if errorlevel 1 exit /b 1
javac -encoding UTF-8 --release 8 -cp "out;lib/*" -d out\test tests\*.java
if errorlevel 1 exit /b 1
java -cp "out\test;out;lib/*" org.junit.runner.JUnitCore MultiplayerProtocolTest InventoryNetworkCodecTest HeadlessProtocolTest ServerAuthorityValidationTest FinishPlanStabilityTest ParadisePortalTest
endlocal
