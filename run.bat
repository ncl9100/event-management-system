@echo off
REM ---------------------------------------------------------------
REM  Event Management System - compile and run  (Windows)
REM
REM    run.bat          starts the command line interface
REM    run.bat gui      starts the windowed interface
REM
REM  Needs a JDK 8 or newer. If javac is not on your PATH this script
REM  will look in JAVA_HOME, then in the usual install folders.
REM ---------------------------------------------------------------
setlocal enabledelayedexpansion
cd /d "%~dp0"

REM ---- find javac -------------------------------------------------
set "JAVAC="
set "JAVACMD="

where javac >nul 2>&1
if not errorlevel 1 (
    set "JAVAC=javac"
    set "JAVACMD=java"
    goto :found
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC=%JAVA_HOME%\bin\javac.exe"
        set "JAVACMD=%JAVA_HOME%\bin\java.exe"
        goto :found
    )
)

for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk*" "%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Microsoft\jdk*" "%ProgramFiles%\Amazon Corretto\jdk*" "%LocalAppData%\Programs\Eclipse Adoptium\jdk*") do (
    if exist "%%~D\bin\javac.exe" (
        set "JAVAC=%%~D\bin\javac.exe"
        set "JAVACMD=%%~D\bin\java.exe"
        goto :found
    )
)

echo.
echo   Could not find javac ^(the Java compiler^).
echo.
echo   A plain "Java runtime" is not enough - you need the JDK.
echo   Install one, then open a NEW terminal and run this again:
echo.
echo       winget install EclipseAdoptium.Temurin.21.JDK
echo.
echo   or download it from https://adoptium.net
echo.
echo   Already have a JDK? Set JAVA_HOME to its folder, for example:
echo       setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21"
echo.
exit /b 1

:found
echo Using: %JAVAC%
if not exist build mkdir build

echo Compiling...
dir /s /b src\*.java > build\sources.txt
"%JAVAC%" -encoding UTF-8 -d build @build\sources.txt
if errorlevel 1 (
    echo.
    echo Compilation failed.
    exit /b 1
)

echo Starting...
if /I "%1"=="gui" (
    "%JAVACMD%" -cp build app.EventRegistrationApplication --gui
) else (
    "%JAVACMD%" -cp build app.EventRegistrationApplication --cli
)
endlocal
