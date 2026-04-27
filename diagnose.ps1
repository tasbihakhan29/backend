#!/usr/bin/env pwsh
<#
.SYNOPSIS
    MedSync Database Connection Diagnostic Script

.DESCRIPTION
    This script helps diagnose and fix database connection issues with the MedSync backend

.NOTES
    Requirements: PowerShell 5.0+, .env file in project root
#>

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "MedSync Database Diagnostics (v1.0)" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Get current directory
$projectRoot = Get-Location
Write-Host "[INFO] Project root: $projectRoot" -ForegroundColor Gray

# Step 1: Check .env file
Write-Host ""
Write-Host "[STEP 1/5] Checking .env file..." -ForegroundColor Yellow

$envFile = Join-Path $projectRoot ".env"
if (Test-Path $envFile) {
    Write-Host "✓ .env file found at: $envFile" -ForegroundColor Green
} else {
    Write-Host "✗ .env file NOT found!" -ForegroundColor Red
    Write-Host "   Expected location: $envFile" -ForegroundColor Red
    Write-Host "   Please create .env with your database credentials" -ForegroundColor Red
    exit 1
}

# Parse .env file
$envVars = @{}
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        $envVars[$key] = $value
    }
}

Write-Host "   Database URL: $($envVars['DB_URL'])" -ForegroundColor Gray
Write-Host "   Username: $($envVars['DB_USERNAME'])" -ForegroundColor Gray
Write-Host "   Password: $(if ($envVars['DB_PASSWORD']) { '[SET]' } else { '[NOT SET]' })" -ForegroundColor Gray

# Step 2: Verify .env contents
Write-Host ""
Write-Host "[STEP 2/5] Verifying .env contents..." -ForegroundColor Yellow

$requiredVars = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET')
$missingVars = @()

foreach ($var in $requiredVars) {
    if ([string]::IsNullOrWhiteSpace($envVars[$var])) {
        $missingVars += $var
    }
}

if ($missingVars.Count -eq 0) {
    Write-Host "✓ All required environment variables are set" -ForegroundColor Green
} else {
    Write-Host "✗ Missing environment variables: $($missingVars -join ', ')" -ForegroundColor Red
    exit 1
}

# Step 3: Test network connectivity
Write-Host ""
Write-Host "[STEP 3/5] Testing network connectivity to Supabase..." -ForegroundColor Yellow

$host_parts = $envVars['DB_URL'] -match 'postgresql://([^:]+):' | Out-Null
$dbHost = $matches[1]
$dbPort = 6543

try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $tcpClient.SendTimeout = 5000
    $tcpClient.ReceiveTimeout = 5000

    $tcpClient.Connect($dbHost, $dbPort)

    if ($tcpClient.Connected) {
        Write-Host "✓ Network connectivity to $dbHost`:$dbPort is working" -ForegroundColor Green
    } else {
        Write-Host "✗ Could not connect to $dbHost`:$dbPort" -ForegroundColor Red
    }
    $tcpClient.Close()
} catch {
    Write-Host "✗ Network test failed: $_" -ForegroundColor Red
    Write-Host "   Host: $dbHost" -ForegroundColor Gray
    Write-Host "   Port: $dbPort" -ForegroundColor Gray
    Write-Host "   Possible causes:" -ForegroundColor Yellow
    Write-Host "   - Firewall blocking the connection" -ForegroundColor Yellow
    Write-Host "   - ISP/Network policy blocking AWS connections" -ForegroundColor Yellow
    Write-Host "   - Supabase server is down" -ForegroundColor Yellow
    Write-Host "   - Incorrect hostname in DB_URL" -ForegroundColor Yellow
}

# Step 4: Check Java/Maven
Write-Host ""
Write-Host "[STEP 4/5] Checking Java and Maven..." -ForegroundColor Yellow

try {
    $javaVersion = (java -version 2>&1)[0]
    Write-Host "✓ Java is installed: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found in PATH" -ForegroundColor Red
    Write-Host "   Please install JDK 17 or later" -ForegroundColor Yellow
}

try {
    $mvnVersion = mvn --version 2>&1 | Select-Object -First 1
    Write-Host "✓ Maven is installed" -ForegroundColor Green
} catch {
    Write-Host "⚠ Maven not found in PATH" -ForegroundColor Yellow
    Write-Host "   You can still use Maven wrapper if available" -ForegroundColor Yellow
}

# Step 5: Display options
Write-Host ""
Write-Host "[STEP 5/5] Choose an action:" -ForegroundColor Yellow
Write-Host "   1. View application.properties" -ForegroundColor Cyan
Write-Host "   2. Start application (requires Maven)" -ForegroundColor Cyan
Write-Host "   3. Build application (requires Maven)" -ForegroundColor Cyan
Write-Host "   4. Test connection with psql (requires PostgreSQL client)" -ForegroundColor Cyan
Write-Host "   5. View .env file contents" -ForegroundColor Cyan
Write-Host "   6. View DATABASE_TROUBLESHOOTING.md guide" -ForegroundColor Cyan
Write-Host "   7. Load env variables and show them" -ForegroundColor Cyan
Write-Host "   0. Exit" -ForegroundColor Cyan
Write-Host ""

$choice = Read-Host "Enter your choice (0-7)"

switch ($choice) {
    "1" {
        Write-Host ""
        Get-Content "src\main\resources\application.properties"
    }

    "2" {
        Write-Host ""
        Write-Host "Starting application..." -ForegroundColor Green
        # Load env variables
        foreach ($var in $envVars.GetEnumerator()) {
            [System.Environment]::SetEnvironmentVariable($var.Key, $var.Value, "Process")
        }
        mvn spring-boot:run
    }

    "3" {
        Write-Host ""
        Write-Host "Building application..." -ForegroundColor Green
        # Load env variables
        foreach ($var in $envVars.GetEnumerator()) {
            [System.Environment]::SetEnvironmentVariable($var.Key, $var.Value, "Process")
        }
        mvn clean package
    }

    "4" {
        Write-Host ""
        Write-Host "Testing PostgreSQL connection with psql..." -ForegroundColor Green
        Write-Host "   Host: $dbHost" -ForegroundColor Gray
        Write-Host "   Port: $dbPort" -ForegroundColor Gray
        Write-Host "   User: $($envVars['DB_USERNAME'])" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Running: psql -h $dbHost -p $dbPort -U $($envVars['DB_USERNAME']) -d postgres -c 'SELECT version();'" -ForegroundColor Gray
        Write-Host ""

        psql -h $dbHost -p $dbPort -U $($envVars['DB_USERNAME']) -d postgres -c "SELECT version();"
    }

    "5" {
        Write-Host ""
        Write-Host "=== .env file contents ===" -ForegroundColor Yellow
        Get-Content $envFile
        Write-Host "==========================" -ForegroundColor Yellow
    }

    "6" {
        Write-Host ""
        if (Test-Path "DATABASE_TROUBLESHOOTING.md") {
            Get-Content "DATABASE_TROUBLESHOOTING.md" | more
        } else {
            Write-Host "DATABASE_TROUBLESHOOTING.md not found" -ForegroundColor Red
        }
    }

    "7" {
        Write-Host ""
        Write-Host "=== Environment Variables ===" -ForegroundColor Yellow
        foreach ($var in $envVars.GetEnumerator()) {
            if ($var.Key -eq "DB_PASSWORD" -or $var.Key -eq "JWT_SECRET") {
                Write-Host "$($var.Key): [REDACTED]" -ForegroundColor Green
            } else {
                Write-Host "$($var.Key): $($var.Value)" -ForegroundColor Green
            }
        }
        Write-Host "=============================" -ForegroundColor Yellow
    }

    "0" {
        Write-Host "Exiting..." -ForegroundColor Yellow
    }

    default {
        Write-Host "Invalid choice" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Diagnostics complete" -ForegroundColor Gray

