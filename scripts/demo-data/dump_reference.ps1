# =============================================================================
# dump_reference.ps1 — Fige un instantané de la base de démonstration
#
# POURQUOI : les scripts de démonstration datent tout par rapport au JOUR
# D'EXÉCUTION. Rejouer run_all.sql demain produit des données décalées d'un
# jour : toutes les colonnes de date changent, et une campagne de captures
# rejouée ne montre plus les mêmes écrans. Le manuel se contredirait d'une
# édition à l'autre.
#
# La parade est de charger UNE fois, de figer le résultat ici, puis de
# restaurer ce dump avant chaque campagne. Deux campagnes espacées de six mois
# produisent alors des captures identiques.
#
# Utilisation :
#     pwsh scripts/demo-data/dump_reference.ps1                 # produire
#     pwsh scripts/demo-data/dump_reference.ps1 -Restore        # restaurer
#
# Paramètres : -Db, -User, -Password, -Fichier
#
# Le dump est au format personnalisé (-Fc) : compressé, et restaurable table
# par table si besoin. Il n'est PAS versionné — plusieurs dizaines de Mo, cf.
# .gitignore.
# =============================================================================

param(
    [string] $Db       = 'pharma_smart_demo',
    [string] $User     = 'pharma_smart',
    [string] $Password = '2802_pharma_smart',
    [string] $Fichier  = (Join-Path $PSScriptRoot 'reference/pharma_smart_demo.dump'),
    [switch] $Restore
)

$ErrorActionPreference = 'Stop'
$env:PGPASSWORD = $Password
$env:PGCLIENTENCODING = 'UTF8'

function Assert-Outil([string] $nom) {
    if (-not (Get-Command $nom -ErrorAction SilentlyContinue)) {
        throw "$nom est introuvable. Ajouter le dossier bin de PostgreSQL au PATH."
    }
}

if ($Restore) {
    Assert-Outil 'pg_restore'
    if (-not (Test-Path $Fichier)) {
        throw "Aucun instantané à restaurer : $Fichier. Le produire d'abord, sans -Restore."
    }

    Write-Host ">> restauration de $Db depuis $Fichier"

    # --clean --if-exists : la base est remise à l'état du dump, pas fusionnée
    # avec son contenu courant. Sans cela, une restauration par-dessus des
    # données existantes échouerait sur les contraintes d'unicité.
    #
    # Le code de sortie de pg_restore est ignoré à dessein : il vaut 1 dès
    # qu'un DROP porte sur un objet absent, ce qui est le cas normal sur une
    # base fraîche. Les vraies erreurs restent visibles dans la sortie.
    pg_restore --clean --if-exists --no-owner --no-privileges `
               --dbname $Db --username $User --host localhost `
               $Fichier 2>&1 | Where-Object { $_ -notmatch 'does not exist, skipping' }

    Write-Host ""
    Write-Host "Base restaurée. Les captures produites maintenant seront identiques"
    Write-Host "à celles de n'importe quelle campagne suivant la même restauration."
    exit 0
}

Assert-Outil 'pg_dump'

$dossier = Split-Path -Parent $Fichier
if (-not (Test-Path $dossier)) { New-Item -ItemType Directory -Path $dossier -Force | Out-Null }

Write-Host ">> instantané de $Db vers $Fichier"

pg_dump --format=custom --no-owner --no-privileges `
        --dbname $Db --username $User --host localhost `
        --file $Fichier

$taille = [math]::Round((Get-Item $Fichier).Length / 1MB, 1)
Write-Host ""
Write-Host "Instantané écrit : $Fichier ($taille Mo)"
Write-Host ""
Write-Host "Avant chaque campagne de captures :"
Write-Host "    pwsh scripts/demo-data/dump_reference.ps1 -Restore"
Write-Host "    npm run captures"
