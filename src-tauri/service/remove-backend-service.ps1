<#
.SYNOPSIS
    Arrête et désinstalle le service Windows pharmasmart-app.
    Appelé par l'action personnalisée WiX PS_RemoveBackendService.
#>
$ErrorActionPreference = 'SilentlyContinue'

$ServiceName = "pharmasmart-app"
$ServiceDir  = Join-Path $env:ProgramData "PharmaSmart\service"
$WinSwExe    = Join-Path $ServiceDir "pharmasmart-app.exe"

$svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $svc) { exit 0 }

if (Test-Path $WinSwExe) {
    Push-Location $ServiceDir
    & ".\pharmasmart-app.exe" stop
    & ".\pharmasmart-app.exe" uninstall
    Pop-Location
} else {
    Stop-Service $ServiceName -Force -ErrorAction SilentlyContinue
    & sc.exe delete $ServiceName | Out-Null
}
Write-Host "Service '$ServiceName' supprimé."
