# ============================================================================
# PharmaSmart — Enregistrement des tâches planifiées de sauvegarde
# ----------------------------------------------------------------------------
# Exécuté une seule fois par l'installeur NSIS ou manuellement en Administrateur.
#
# Toutes les tâches se déclenchent AU DÉMARRAGE de la machine (AtStartup).
# La décision de réellement exécuter une opération coûteuse (base backup, purge)
# appartient au binaire Rust — qui garde des fichiers sentinelles et skip si
# l'opération a déjà eu lieu récemment.
#
# Délais échelonnés pour ne pas saturer le CPU au démarrage :
#   - check  : +2 min  (rapide, vérifie l'intégrité de la dernière nuit)
#   - dump   : +3 min  (laisse la JVM démarrer)
#   - purge  : +5 min  (nettoyage, attend que dump ait tourné)
#   - base   : +7 min  (lourd, en dernier)
#
# Règles d'auto-skip dans le binaire Rust :
#   dump  — toujours exécuté (+ répétition PT2H)
#   base  — skip si un base backup < 6 jours existe
#   purge — skip si la dernière purge date de moins de 23 h
#   check — toujours exécuté (léger, c'est son rôle)
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

$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest

# Paramètres communs (partagés par toutes les tâches)
# - StartWhenAvailable  : garde-fou si le déclencheur AtStartup a été manqué
# - Priority 6          : BELOW_NORMAL — cède le CPU à PharmaSmart sous charge
$commonSettings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable:$false `
    -ExecutionTimeLimit (New-TimeSpan -Hours 1) `
    -Priority 6

function New-StartupTrigger {
    param([string]$Delay)
    $t = New-ScheduledTaskTrigger -AtStartup
    $t.Delay = $Delay
    return $t
}

# ── CHECK (+2 min) ───────────────────────────────────────────────────────────
# Léger, passe en premier : vérifie qu'un dump récent existe.
Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Check' `
    -Description 'PharmaSmart — vérifie la présence d''un dump récent (à chaque démarrage +2 min)' `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'check') `
    -Trigger   (New-StartupTrigger 'PT2M') `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Check enregistrée  (AtStartup +2 min).'

# ── DUMP (+3 min, répétition PT2H) ──────────────────────────────────────────
# Démarrage + répétition indéfinie toutes les 2 h.
# Laisse 3 min pour que la JVM soit opérationnelle.
$tBoot = New-StartupTrigger 'PT3M'

# NOTE : [TimeSpan]::MaxValue produit une durée ISO 8601 hors limites
# ("P99999999DT23H59M59S") que le schéma XML du Planificateur de tâches rejette
# (Register-ScheduledTask échoue avec HRESULT 0x80041318). Pour une répétition
# indéfinie, on construit le trigger avec une durée valable puis on vide la
# propriété Repetition.Duration : une chaîne vide signifie "indéfiniment" pour
# le Planificateur de tâches.
$tRepeat = New-ScheduledTaskTrigger -Once -At (Get-Date -Hour 0 -Minute 0 -Second 0) `
    -RepetitionInterval (New-TimeSpan -Hours 2) `
    -RepetitionDuration (New-TimeSpan -Days 1)
$tRepeat.Repetition.Duration = ''

Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Dump' `
    -Description 'PharmaSmart — pg_dump toutes les 2 h (et au démarrage +3 min)' `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'dump') `
    -Trigger   @($tBoot, $tRepeat) `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Dump  enregistrée  (AtStartup +3 min + PT2H).'

# ── PURGE (+5 min) ───────────────────────────────────────────────────────────
# Le binaire skip automatiquement si une purge a eu lieu il y a moins de 23 h.
Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Purge' `
    -Description 'PharmaSmart — rotation des anciens backups (à chaque démarrage +5 min, auto-skip < 23 h)' `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'purge') `
    -Trigger   (New-StartupTrigger 'PT5M') `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Purge enregistrée  (AtStartup +5 min).'

# ── BASE (+7 min) ────────────────────────────────────────────────────────────
# Le binaire skip automatiquement si un base backup a eu lieu il y a moins de 6 jours.
Register-ScheduledTask `
    -TaskName 'PharmaSmart_Backup_Base' `
    -Description 'PharmaSmart — pg_basebackup hebdomadaire (à chaque démarrage +7 min, auto-skip < 6 j)' `
    -Action    (New-ScheduledTaskAction -Execute $exe -Argument 'base') `
    -Trigger   (New-StartupTrigger 'PT7M') `
    -Settings  $commonSettings `
    -Principal $principal `
    -Force | Out-Null
Write-Host 'PharmaSmart_Backup_Base  enregistrée  (AtStartup +7 min).'

Write-Host ''
Write-Host 'Toutes les tâches planifiées PharmaSmart_Backup_* ont été enregistrées.'
Write-Host 'Résumé des déclencheurs :'
Write-Host '  check  : chaque démarrage +2 min  (toujours exécuté)'
Write-Host '  dump   : chaque démarrage +3 min  + répétition PT2H'
Write-Host '  purge  : chaque démarrage +5 min  (skip si < 23 h)'
Write-Host '  base   : chaque démarrage +7 min  (skip si < 6 j)'
