@echo off
REM MedSync Backend - Database Connection Diagnostic Script
REM This script helps diagnose database connection issues

echo =====================================
echo MedSync Database Diagnostics
echo =====================================
echo.

REM Check if .env file exists
echo [1/4] Checking .env file...
if exist ".env" (
    echo ✓ .env file found
) else (
    echo ✗ .env file NOT found in current directory
    echo Please create .env file with your database credentials
    pause
    exit /b 1
)
echo.

REM Check required environment variables
echo [2/4] Verifying .env contents...
for /f "tokens=*" %%a in (.env) do (
    if "%%a"=="" goto skip_line
    setlocal enabledelayedexpansion
    set line=%%a
    if "!line:~0,1!"=="D" echo   Found: !line:~0,10!...
    if "!line:~0,1!"=="J" echo   Found: !line:~0,10!...
    endlocal
    :skip_line
)
echo.

REM Offer options
echo [3/4] Choose an action:
echo   1. Run tests with Maven (compile and package)
echo   2. Start the application with Maven
echo   3. View application.properties
echo   4. Export environment variables and start
echo   5. Exit
echo.
set /p choice="Enter choice (1-5): "

if "%choice%"=="1" (
    echo.
    echo Running Maven clean package...
    call mvn clean package -DskipTests
    goto end
)

if "%choice%"=="2" (
    echo.
    echo Starting application with Maven...
    REM Load .env variables
    for /f "tokens=*" %%a in (.env) do set %%a
    call mvn spring-boot:run
    goto end
)

if "%choice%"=="3" (
    echo.
    echo Displaying application.properties...
    more src\main\resources\application.properties
    goto end
)

if "%choice%"=="4" (
    echo.
    echo Loading environment variables and starting...
    REM Load .env variables
    for /f "tokens=*" %%a in (.env) do set %%a
    echo   DB_URL: !DB_URL!
    echo   DB_USERNAME: !DB_USERNAME!
    echo   JWT_SECRET: [REDACTED]
    echo.
    REM Try to run Maven
    if exist "mvnw.cmd" (
        call mvnw.cmd spring-boot:run
    ) else if exist "mvnw" (
        call .\mvnw spring-boot:run
    ) else (
        echo Maven wrapper not found. Trying system Maven...
        call mvn spring-boot:run
    )
    goto end
)

if "%choice%"=="5" (
    goto end
)

:end
echo.
echo Diagnostics complete.
pause

