# ============================================================================
# PharmaSmart — Suppression des tâches planifiées de sauvegarde
# ----------------------------------------------------------------------------
# Exécuté par l'installeur NSIS à la désinstallation, ou manuellement.
# ============================================================================
[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'

$tasks = @(
    'PharmaSmart_Backup_Dump',
    'PharmaSmart_Backup_Base',
    'PharmaSmart_Backup_Purge',
    'PharmaSmart_Backup_Check'
)

foreach ($name in $tasks) {
    $task = Get-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue
    if ($task) {
        Unregister-ScheduledTask -TaskName $name -Confirm:$false
        Write-Host "$name supprimée."
    } else {
        Write-Host "$name absente (rien à faire)."
    }
}
