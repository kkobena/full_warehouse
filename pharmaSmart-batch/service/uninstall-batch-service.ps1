#Requires -RunAsAdministrator
<#
.SYNOPSIS
    Arrête et désinstalle le service Windows pharmaSmart-batch.

.PARAMÈTRES
    -InstallDir : Répertoire de déploiement (défaut : C:\PharmaSmartBatch)
    -KeepFiles  : Ne pas supprimer le répertoire (JARs et logs conservés)
#>
param(
    [string]$InstallDir = "C:\PharmaSmartBatch",
    [switch]$KeepFiles
)

$ErrorActionPreference = 'Stop'

$ServiceName = "pharmasmart-batch"
$exe = Join-Path $InstallDir "pharmasmart-batch.exe"

$svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($svc) {
    if (Test-Path $exe) {
        Push-Location $InstallDir
        & ".\pharmasmart-batch.exe" stop 2>$null
        & ".\pharmasmart-batch.exe" uninstall
        Pop-Location
    } else {
        Stop-Service $ServiceName -Force -ErrorAction SilentlyContinue
        & sc.exe delete $ServiceName
    }
    Write-Host "Service '$ServiceName' désinstallé."
} else {
    Write-Host "Service '$ServiceName' non trouvé — ignoré."
}

if (-not $KeepFiles -and (Test-Path $InstallDir)) {
    Remove-Item $InstallDir -Recurse -Force
    Write-Host "Répertoire $InstallDir supprimé."
}
