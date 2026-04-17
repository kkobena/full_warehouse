# ============================================================================
# PharmaSmart — Enregistrement des tâches planifiées de sauvegarde
# ----------------------------------------------------------------------------
# Exécuté une seule fois par l'installeur NSIS ou manuellement en Administrateur.
#
# Ce script enregistre 4 tâches planifiées :
#   - PharmaSmart_Backup_Dump   : pg_dump au démarrage + toutes les 2 h
#   - PharmaSmart_Backup_Base   : pg_basebackup hebdomadaire (dimanche 02h00)
#   - PharmaSmart_Backup_Purge  : rotation hebdomadaire (dimanche 03h00)
#   - PharmaSmart_Backup_Check  : vérification hebdomadaire (lundi 08h00)
#
# Le chemin du binaire est résolu automatiquement :
#   1. Paramètre -ExePath s'il est fourni
#   2. pharmasmart-backup.exe situé à côté du script
#   3. %ProgramFiles%\PharmaSmart\pharmasmart-backup.exe (install par défaut)
# ============================================================================
[CmdletBinding()]
param(
    [string]$ExePath
)

$ErrorActionPreference = 'Stop'

function Resolve-Exe {
    param([string]$Explicit)

    if ($Explicit -and (Test-Path $Explicit)) { return (Resolve-Path $Explicit).Path }

    $candidates = @(
        (Join-Path $PSScriptRoot 'pharmasmart-backup.exe'),
        (Join-Path (Split-Path -Parent $PSScriptRoot) 'pharmasmart-backup.exe'),
        (Join-Path $env:ProgramFiles 'PharmaSmart\pharmasmart-backup.exe')
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return (Resolve-Path $c).Path }
    }
    throw "pharmasmart-backup.exe introuvable. Utilisez -ExePath pour le spécifier."
}

$exe = Resolve-Exe -Explicit $ExePath
Write-Host "Binaire utilisé : $exe"

# ── Paramètres communs ──────────────────────────────────────────────────────
# - StartWhenAvailable      : rattrape les tâches manquées au prochain démarrage
# - ExecutionTimeLimit 1 h  : garde-fou en cas de blocage
# - Priority 6 (BELOW_NORMAL) : pg_dump cède le CPU à PharmaSmart sous charge
$commonSettings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable:$false `
    -ExecutionTimeLimit (New-TimeSpan -Hours 1) `
    -Priority 6

$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest

# ── DUMP : au démarrage (+ 3 min) + toutes les 2 h ──────────────────────────
#
# Deux triggers combinés :
#   1. AtStartup + PT3M : laisse la JVM démarrer avant un dump CPU-intensif
#   2. Once + Repetition PT2H (StopAtDurationEnd = false) : répétition indéfinie
#
# Résultat : au moins un dump par session, quel que soit l'horaire de démarrage.
$tBoot = New-ScheduledTaskTrigger -AtStartup
$tBoot.Delay = 'PT3M'

$tRepeat = New-ScheduledTaskTrigger -Once -At (Get-Date -Hour 0 -Minute 0 -Second 0)
$tRepeat.Repetition.Interval          = 'PT2H'
$tRepeat.Repetition.StopAtDurationEnd = $false

Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Dump' `
    -Description 'PharmaSmart — pg_dump toutes les 2 h (et au démarrage)' `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'dump') `
    -Trigger   @($tBoot, $tRepeat) `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Dump enregistrée.'

# ── BASE BACKUP hebdomadaire (dimanche 02h00) ───────────────────────────────
Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Base' `
    -Description 'PharmaSmart — pg_basebackup hebdomadaire (dimanche 02h00)' `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'base') `
    -Trigger   (New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At '02:00') `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Base enregistrée.'

# ── PURGE hebdomadaire (dimanche 03h00) ─────────────────────────────────────
Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Purge' `
    -Description 'PharmaSmart — purge des anciens backups (dimanche 03h00)' `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'purge') `
    -Trigger   (New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At '03:00') `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Purge enregistrée.'

# ── VÉRIFICATION hebdomadaire (lundi 08h00) ─────────────────────────────────
Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Check' `
    -Description "PharmaSmart — vérification qu'un dump récent existe (lundi 08h00)" `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'check') `
    -Trigger   (New-ScheduledTaskTrigger -Weekly -DaysOfWeek Monday -At '08:00') `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Check enregistrée.'

Write-Host ''
Write-Host 'Toutes les tâches planifiées PharmaSmart_Backup_* ont été enregistrées avec succès.'
