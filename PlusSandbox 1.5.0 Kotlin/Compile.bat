@echo off
title Kotlin Compiler for PlusSandbox
echo.
echo ============================================
echo   PlusSandbox - Kotlin Compilation Script
echo ============================================
echo.

cd %USERPROFILE%\Downloads\PSKE
REM Set Kotlin compiler path
set KOTLIN_HOME=C:\MyKotlin
set PATH=%KOTLIN_HOME%\bin;%PATH%

REM Check if Kotlin compiler exists
echo [INFO] Checking Kotlin compiler...
where kotlinc >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Kotlin compiler not found at %KOTLIN_HOME%\bin
    echo [ERROR] Please check:
    echo [ERROR] 1. Is Kotlin installed in C:\MyKotlin?
    echo [ERROR] 2. Does C:\MyKotlin\bin\kotlinc.bat exist?
    echo.
    echo Press any key to exit...
    pause > nul
    exit /b 1
)

echo [OK] Kotlin compiler found
echo.

REM Check if source file exists
echo [INFO] Checking source files...
if not exist "Main.kt" (
    echo [ERROR] Main.kt not found in current directory
    echo [ERROR] Current directory: %CD%
    echo [ERROR] Please ensure Main.kt is in the same folder
    echo.
    echo Press any key to exit...
    pause > nul
    exit /b 1
)

echo [OK] Main.kt found
echo.

REM Create output directory if it doesn't exist
echo [INFO] Preparing output directory...
if not exist "out" (
    mkdir out
    echo [OK] Created 'out' directory
) else (
    echo [INFO] 'out' directory exists
)

echo.

REM Compile the project
echo [INFO] Starting compilation...
echo [INFO] Command: kotlinc Main.kt -include-runtime -d out/PlusSandbox.jar
echo.

REM Show timestamp before compilation
echo [INFO] Start time: %time%
echo.

kotlinc Main.kt -include-runtime -d out/PlusSandbox.jar

REM Check compilation result
if errorlevel 0 (
    echo.
    echo [INFO] End time: %time%
    echo.
    echo ============================================
    echo [SUCCESS] COMPILATION COMPLETED
    echo ============================================
    echo.
    
    REM Get file size
    if exist "out\PlusSandbox.jar" (
        for %%F in ("out\PlusSandbox.jar") do set size=%%~zF
        set /a sizeMB=size/1048576
        set /a sizeKB=size/1024
        echo [INFO] Output file: out\PlusSandbox.jar
        echo [INFO] File size: %size% bytes (%sizeKB% KB / %sizeMB% MB)
    ) else (
        echo [WARNING] Output file not created
    )
    
    echo.
    echo [INSTRUCTIONS]
    echo 1. To run the game: java -jar out\PlusSandbox.jar
    echo 2. Or use: run.bat
    echo 3. To clean: clean.bat (deletes output folder)
    
    echo.
    echo [STATUS] Ready to run!
    echo ============================================
) else (
    echo.
    echo ============================================
    echo [FAILURE] COMPILATION FAILED
    echo ============================================
    echo.
    echo [ERROR] Possible causes:
    echo [ERROR] 1. Syntax errors in Main.kt
    echo [ERROR] 2. Kotlin compiler issues
    echo [ERROR] 3. Missing dependencies
    echo.
    echo [TROUBLESHOOTING]
    echo - Check Main.kt for errors
    echo - Verify Kotlin is properly installed
    echo - Ensure Java is installed (java -version)
    echo.
)

echo.
echo Press any key to continue...
pause > nul
where kotlinc >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Kotlin compiler not found at %KOTLIN_HOME%\bin
    echo [ERROR] Please check Kotlin installation path
    echo.
    pause
    exit /b 1
)

echo [OK] Kotlin compiler found: %KOTLIN_HOME%\bin\kotlinc.bat
echo.

REM Check if source file exists
echo [INFO] Checking source files...
if not exist "Main.kt" (
    echo [ERROR] Main.kt not found in current directory
    echo [ERROR] Please ensure Main.kt is in the same folder
    echo.
    pause
    exit /b 1
)

echo [OK] Main.kt found
echo.

REM Create output directory if it doesn't exist
echo [INFO] Preparing output directory...
if not exist "out" (
    mkdir out
    echo [OK] Created 'out' directory
) else (
    echo [OK] 'out' directory exists
)

echo.

REM Compile the project
echo [INFO] Starting compilation...
echo [INFO] Command: kotlinc Main.kt -include-runtime -d out/PlusSandbox.jar
echo.

kotlinc Main.kt -include-runtime -d out/PlusSandbox.jar

REM Check compilation result
if errorlevel 0 (
    echo.
    echo ============================================
    echo [SUCCESS] COMPILATION COMPLETED
    echo ============================================
    echo.
    
    REM Get file size
    for %%F in ("out\PlusSandbox.jar") do set size=%%~zF
    
    echo [INFO] Output file: out\PlusSandbox.jar
    echo [INFO] File size: %size% bytes
    
    echo.
    echo [INSTRUCTIONS]
    echo 1. To run the game: java -jar out\PlusSandbox.jar
    echo 2. Or use: run.bat
    echo 3. To clean: clean.bat (deletes output folder)
    
    echo.
    echo [STATUS] Ready to run!
    echo ============================================
) else (
    echo.
    echo ============================================
    echo [FAILURE] COMPILATION FAILED
    echo ============================================
    echo.
    echo [ERROR] Check for syntax errors in Main.kt
    echo [ERROR] Verify Kotlin installation
    echo.
)

echo.
pause