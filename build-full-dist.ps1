#Requires -Version 5.1
<#
.SYNOPSIS
    Construit le ZIP de distribution complet PharmaSmart.

.DESCRIPTION
    Orchestre la compilation de tous les composants :
      - pharmaSmart-app et pharmaSmart-batch (Maven)
      - pharmasmart-backup (Cargo / Rust)
      - Tauri NSIS installer (optionnel — -IncludeTauri)

    Produit : dist\pharmasmart-<version>-full.zip

    Structure du ZIP :
      PharmaSmart-<v>-setup.exe            (Tauri desktop, si -IncludeTauri)
      pharmaSmart-batch-<v>-setup.exe      (installeur batch NSIS, si dispo)
        └─ ou pharmaSmart-batch-<v>.jar    (fallback si pas d'installeur)
      pharmasmart-backup.exe               (binaire Rust)
      config.default.json                  (template — à copier en config.json et éditer)
      service\
        pharmasmart-setup.ps1              (orchestrateur tout-en-un)
        pharmasmart-app.xml                (template WinSW)
        pharmasmart-batch.xml              (template WinSW)
        setup-backup-tasks.ps1
        remove-backup-tasks.ps1

.PARAMÈTRES
    -IncludeTauri : compiler et inclure l'installeur Tauri desktop (long)
    -SkipBuild    : utiliser les artefacts déjà présents dans target/ et release/
    -OutDir       : répertoire de sortie (défaut : .\dist)

.EXEMPLES
    .\build-full-dist.ps1
    .\build-full-dist.ps1 -IncludeTauri
    .\build-full-dist.ps1 -SkipBuild -OutDir "D:\releases"
#>
param(
    [switch]$IncludeTauri,
    [switch]$SkipBuild,
    [string]$OutDir = ".\dist"
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot

# ── Lire la version depuis pharmaSmart-batch/pom.xml ─────────────────────────
function Get-Version {
    $pom = [xml](Get-Content (Join-Path $Root "pharmaSmart-batch\pom.xml") -Raw)
    $ver = $pom.project.version
    if (-not $ver) { $ver = $pom.project.parent.version }
    return $ver
}

$Version  = Get-Version
$ZipName  = "pharmasmart-$Version-full.zip"
$StageDir = Join-Path $Root ".dist-stage"

Write-Host "=== PharmaSmart $Version — Build Distribution ===" -ForegroundColor Cyan
Write-Host "  OutDir  : $OutDir"
Write-Host "  Tauri   : $(if ($IncludeTauri) {'oui'} else {'non (utiliser -IncludeTauri)'})"
Write-Host ""

# ── 1. Build Maven ────────────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Host "[1/4] Build Maven (pharmaSmart-app + pharmaSmart-batch)..." -ForegroundColor Cyan
    & "$Root\mvnw.cmd" clean package -DskipTests -Pprod `
        -pl "pharmaSmart-app,pharmaSmart-batch" -am
    if ($LASTEXITCODE -ne 0) { Write-Error "Build Maven échoué (code $LASTEXITCODE)." }
    Write-Host "  Maven OK" -ForegroundColor Green
} else {
    Write-Host "[1/4] Build Maven : ignoré (-SkipBuild)"
}

# ── 2. Build Cargo ────────────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Host "[2/4] Build Cargo (pharmasmart-backup)..." -ForegroundColor Cyan
    Push-Location (Join-Path $Root "src-backup")
    & cargo build --release
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Error "Build Cargo échoué (code $code)." }
    Write-Host "  Cargo OK" -ForegroundColor Green
} else {
    Write-Host "[2/4] Build Cargo : ignoré (-SkipBuild)"
}

# ── 3. Build Tauri (optionnel) ────────────────────────────────────────────────
if ($IncludeTauri -and -not $SkipBuild) {
    Write-Host "[3/4] Build Tauri (installeur NSIS)..." -ForegroundColor Cyan
    & npm run tauri:build
    if ($LASTEXITCODE -ne 0) { Write-Error "Build Tauri échoué (code $LASTEXITCODE)." }
    Write-Host "  Tauri OK" -ForegroundColor Green
} else {
    Write-Host "[3/4] Build Tauri : ignoré"
}

# ── 4. Assembler ──────────────────────────────────────────────────────────────
Write-Host "[4/4] Assemblage de $ZipName..." -ForegroundColor Cyan
Remove-Item $StageDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$StageDir\service" | Out-Null

function Add-File {
    param([string]$Src, [string]$Dst)
    if (Test-Path $Src) {
        $dstDir = Split-Path $Dst -Parent
        if ($dstDir) { New-Item -ItemType Directory -Force -Path $dstDir | Out-Null }
        Copy-Item $Src $Dst -Force
        Write-Host "  + $(Split-Path $Dst -Leaf)"
        return $true
    }
    return $false
}

# Tauri NSIS setup
if ($IncludeTauri) {
    $tauriExe = Get-ChildItem (Join-Path $Root "src-tauri\target\release\bundle\nsis\PharmaSmart_*.exe") `
        -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($tauriExe) { Add-File $tauriExe.FullName "$StageDir\$($tauriExe.Name)" | Out-Null }
    else           { Write-Warning "Installeur Tauri NSIS introuvable — ignoré." }
}

# pharmaSmart-batch : installeur NSIS ou JAR
$batchSetup = Get-ChildItem (Join-Path $Root "pharmaSmart-batch\target\pharmasmart-batch-*-setup.exe") `
    -ErrorAction SilentlyContinue | Select-Object -First 1
if ($batchSetup) {
    Add-File $batchSetup.FullName "$StageDir\$($batchSetup.Name)" | Out-Null
} else {
    $batchJar = Get-ChildItem (Join-Path $Root "pharmaSmart-batch\target\pharmaSmart-batch-*.jar") `
        -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
    if ($batchJar) { Add-File $batchJar.FullName "$StageDir\$($batchJar.Name)" | Out-Null }
    else           { Write-Warning "pharmaSmart-batch JAR introuvable — ignoré." }
}

# pharmasmart-backup.exe
$backupExe = Join-Path $Root "src-backup\target\release\pharmasmart-backup.exe"
if (-not (Add-File $backupExe "$StageDir\pharmasmart-backup.exe")) {
    Write-Warning "pharmasmart-backup.exe introuvable ($backupExe) — exécuter : cargo build --release dans src-backup/"
}

# config.default.json (source unique dans src-tauri/)
Add-File (Join-Path $Root "src-tauri\config.default.json") "$StageDir\config.default.json" | Out-Null

# Scripts de service
Add-File (Join-Path $Root "service\pharmasmart-setup.ps1")  "$StageDir\service\pharmasmart-setup.ps1"  | Out-Null
Add-File (Join-Path $Root "service\pharmasmart-app.xml")    "$StageDir\service\pharmasmart-app.xml"    | Out-Null
Add-File (Join-Path $Root "service\pharmasmart-batch.xml")  "$StageDir\service\pharmasmart-batch.xml"  | Out-Null

# Scripts backup (source canonique : src-backup/scripts/)
Add-File (Join-Path $Root "src-backup\scripts\setup-backup-tasks.ps1")  "$StageDir\service\setup-backup-tasks.ps1"  | Out-Null
Add-File (Join-Path $Root "src-backup\scripts\remove-backup-tasks.ps1") "$StageDir\service\remove-backup-tasks.ps1" | Out-Null

# ── 5. Créer le ZIP ──────────────────────────────────────────────────────────
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$zipPath = Join-Path (Resolve-Path $OutDir) $ZipName
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Compress-Archive -Path "$StageDir\*" -DestinationPath $zipPath -Force
Remove-Item $StageDir -Recurse -Force

Write-Host ""
Write-Host "=== Distribution prête ===" -ForegroundColor Green
Write-Host "  $zipPath"
Write-Host ""
Write-Host "Contenu :"
Get-ChildItem -Recurse (Join-Path (Resolve-Path $OutDir) $ZipName) | ForEach-Object { Write-Host "  $_" }
Write-Host ""
Write-Host "Déploiement serveur :" -ForegroundColor Yellow
Write-Host "  1. Extraire le ZIP"
Write-Host "  2. Copier config.default.json en config.json et renseigner database.*, jvm.java_home"
Write-Host "  3. .\service\pharmasmart-setup.ps1 install"
