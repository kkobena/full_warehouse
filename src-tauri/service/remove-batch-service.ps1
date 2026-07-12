<#
.SYNOPSIS
    Arrête et désinstalle le service Windows pharmasmart-batch.
    Appelé par NSIS_HOOK_PREUNINSTALL, juste après remove-backend-service.ps1.
#>
$ErrorActionPreference = 'SilentlyContinue'

$ServiceName = "pharmasmart-batch"
$ServiceDir  = Join-Path $env:ProgramData "PharmaSmart\batch"
$WinSwExe    = Join-Path $ServiceDir "pharmasmart-batch.exe"

$svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $svc) { exit 0 }

if (Test-Path $WinSwExe) {
    Push-Location $ServiceDir
    & ".\pharmasmart-batch.exe" stop
    & ".\pharmasmart-batch.exe" uninstall
    Pop-Location
} else {
    Stop-Service $ServiceName -Force -ErrorAction SilentlyContinue
    & sc.exe delete $ServiceName | Out-Null
}
Write-Host "Service '$ServiceName' supprimé."
