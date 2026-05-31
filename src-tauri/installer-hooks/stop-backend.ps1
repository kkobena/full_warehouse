# PharmaSmart — Stop backend Java process before uninstall
# Step 1: graceful HTTP shutdown (Spring Boot Actuator)
# Step 2: kill java.exe processes that own the PharmaSmart JAR
# Step 3: fallback — kill by TCP port using Get-NetTCPConnection
param(
    [string]$InstallDir = "",
    [int]$Port = 9080
)

# Read the actual port from config.json (user may have changed it)
$configCandidates = @(
    (Join-Path $env:PROGRAMDATA "PharmaSmart\config.json"),
    (Join-Path $env:APPDATA     "PharmaSmart\config.json")
)
if ($InstallDir -ne "") {
    $configCandidates = @(Join-Path $InstallDir "config.json") + $configCandidates
}
foreach ($cfg in $configCandidates) {
    if (Test-Path $cfg) {
        try {
            $json = Get-Content $cfg -Raw | ConvertFrom-Json
            if ($json.server.port) { $Port = [int]$json.server.port }
        } catch {}
        break
    }
}

Write-Host "PharmaSmart stop-backend: targeting port $Port"

# Step 1 — graceful HTTP shutdown (Spring Boot Actuator)
Write-Host "Trying graceful shutdown on http://localhost:$Port/management/shutdown ..."
try {
    Invoke-RestMethod -Uri "http://localhost:$Port/management/shutdown" `
                      -Method Post -TimeoutSec 5 -ErrorAction Stop | Out-Null
    Write-Host "Graceful shutdown accepted. Waiting 5s..."
    Start-Sleep -Seconds 5
} catch {
    Write-Host "Graceful shutdown unavailable ($($_.Exception.Message)). Proceeding to force-kill."
}

$killed = $false

# Step 2 — kill via CimInstance (reliable on Windows 10/11, no WMI issues)
try {
    $procs = Get-CimInstance -ClassName Win32_Process -Filter "Name LIKE 'java%'" -ErrorAction Stop |
        Where-Object {
            $cl = $_.CommandLine
            $cl -and (
                $cl -like '*pharmaSmart*' -or
                $cl -like '*pharmasmart*' -or
                $cl -like '*PharmaSmart*'
            )
        }
    foreach ($p in $procs) {
        Write-Host "Force-killing java.exe PID=$($p.ProcessId) — $($p.CommandLine -replace '.{1,80}$','')"
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
        $killed = $true
    }
} catch {
    Write-Warning "CimInstance query failed: $_"

    # Sub-fallback: legacy Get-WmiObject
    try {
        $procs = Get-WmiObject Win32_Process -ErrorAction Stop |
            Where-Object { $_.Name -like 'java*' -and $_.CommandLine -like '*pharmasmart*' }
        foreach ($p in $procs) {
            Write-Host "Force-killing java.exe PID=$($p.ProcessId) (WMI)"
            Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
            $killed = $true
        }
    } catch {
        Write-Warning "WMI query also failed: $_"
    }
}

# Step 3 — fallback: kill by port using Get-NetTCPConnection (no external tools needed)
if (-not $killed) {
    Write-Host "Trying to kill by port $Port via Get-NetTCPConnection..."
    try {
        $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        foreach ($conn in $conns) {
            $pid = $conn.OwningProcess
            if ($pid -gt 4) {
                Write-Host "Killing PID=$pid (listening on port $Port)"
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                $killed = $true
            }
        }
    } catch {
        Write-Warning "Get-NetTCPConnection failed: $_"
    }
}

# Step 4 — last resort: netstat
if (-not $killed) {
    Write-Host "Trying netstat fallback..."
    try {
        $lines = & netstat -ano 2>$null | Where-Object { $_ -match ":$Port\s" }
        foreach ($line in $lines) {
            if ($line -match '\s+(\d+)\s*$') {
                $pid = [int]$Matches[1]
                if ($pid -gt 4) {
                    Write-Host "Killing PID=$pid (netstat, port $Port)"
                    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                    $killed = $true
                }
            }
        }
    } catch {
        Write-Warning "netstat fallback failed: $_"
    }
}

if ($killed) {
    Write-Host "Waiting 3s for process cleanup and file-lock release..."
    Start-Sleep -Seconds 3
}

Write-Host "stop-backend completed."
exit 0
