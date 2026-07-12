#Requires -RunAsAdministrator
<#
.SYNOPSIS
    Installe pharmaSmart-batch comme service Windows (WinSW).

.PRÉREQUIS
    - Exécuter en tant qu'Administrateur.
    - Java 25+ installé (JAVA_HOME configuré).
    - JAR construit : mvnw.cmd clean package -DskipTests -pl pharmaSmart-batch

.PARAMÈTRES
    -InstallDir : Répertoire de déploiement (défaut : C:\ProgramData\PharmaSmart\batch)
    -DbUrl      : URL JDBC (défaut : jdbc:postgresql://localhost:5432/pharma_smart)
    -DbUser     : Utilisateur PostgreSQL (défaut : pharma_smart)
    -JavaHome   : Chemin du JRE/JDK (ex: "C:\jdk-25"). Si absent, utilise JAVA_HOME puis PATH.

    Le mot de passe est demandé de façon sécurisée (SecureString) — il n'est
    jamais passé en argument de ligne de commande.

.EXEMPLES
    .\install-batch-service.ps1
    .\install-batch-service.ps1 -InstallDir "D:\Services\Batch"
    .\install-batch-service.ps1 -JavaHome "C:\Program Files\Java\jdk-25"
    .\install-batch-service.ps1 -JavaHome "C:\Users\k.kobena\Documents\jdk25" -InstallDir "D:\Services\Batch"
#>
param(
    [string]$InstallDir = "$env:ProgramData\PharmaSmart\batch",
    [string]$DbUrl      = "jdbc:postgresql://localhost:5432/pharma_smart",
    [string]$DbUser     = "pharma_smart",
    [string]$DbSchema   = "warehouse",
    [string]$HeapMin    = "128m",
    [string]$HeapMax    = "512m",
    [string]$JavaHome   = ""
)

$ErrorActionPreference = 'Stop'

# SID bien connus plutôt que noms pour éviter IdentityNotMappedException
# ("Impossible de traduire certaines ou toutes les références d'identité")
# sur AddAccessRule — cf. src-tauri/service/setup-backend-service.ps1.
$RestrictedSids = @(
    (New-Object System.Security.Principal.SecurityIdentifier([System.Security.Principal.WellKnownSidType]::BuiltinAdministratorsSid, $null)),
    (New-Object System.Security.Principal.SecurityIdentifier([System.Security.Principal.WellKnownSidType]::LocalSystemSid, $null))
)

$ServiceName = "pharmasmart-batch"
$ScriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot    = Split-Path -Parent (Split-Path -Parent $ScriptDir)
$WinSwExe    = Join-Path $InstallDir "pharmasmart-batch.exe"

# WinSW peut être bundlé à côté du script (installeur .exe) ou dans $InstallDir
$WinSwCandidates = @(
    (Join-Path $ScriptDir   "..\pharmasmart-batch.exe"),  # installeur .exe
    (Join-Path $ScriptDir   "..\WinSW.exe"),              # ZIP distribué
    (Join-Path $ScriptDir   "WinSW.exe"),                 # même dossier
    $WinSwExe                                             # déjà en place
)

# ── Résoudre java.exe + heaps depuis config ──────────────────────────────────
# Priorité : 1) -JavaHome param  2) config.json  3) config.default.json  4) JAVA_HOME env  5) PATH
# HeapMin/HeapMax : lus depuis jvm.batch si non passés en param CLI
if (-not $JavaHome -or -not $PSBoundParameters.ContainsKey('HeapMin') -or -not $PSBoundParameters.ContainsKey('HeapMax')) {
    foreach ($cfgFile in @("$InstallDir\config.json", "$InstallDir\config.default.json")) {
        if (Test-Path $cfgFile) {
            try {
                $cfgObj = Get-Content $cfgFile -Raw | ConvertFrom-Json
                if (-not $JavaHome -and $cfgObj.jvm.java_home) {
                    $JavaHome = $cfgObj.jvm.java_home; Write-Host "java_home lu depuis : $cfgFile"
                }
                if (-not $PSBoundParameters.ContainsKey('HeapMin') -and $cfgObj.jvm.batch.heap_min) {
                    $HeapMin = $cfgObj.jvm.batch.heap_min
                }
                if (-not $PSBoundParameters.ContainsKey('HeapMax') -and $cfgObj.jvm.batch.heap_max) {
                    $HeapMax = $cfgObj.jvm.batch.heap_max
                }
                if ($JavaHome) { break }
            } catch {}
        }
    }
}

if ($JavaHome -and (Test-Path "$JavaHome\bin\java.exe")) {
    $JavaExe = "$JavaHome\bin\java.exe"
} elseif ($JavaHome) {
    Write-Error "JavaHome '$JavaHome' spécifié mais '$JavaHome\bin\java.exe' introuvable."
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $JavaExe = "$env:JAVA_HOME\bin\java.exe"
} else {
    $JavaExe = "java"
}
Write-Host "Java : $JavaExe"

# ── Mot de passe — prompt sécurisé ───────────────────────────────────────────
$secPwd = Read-Host "Mot de passe PostgreSQL (utilisateur '$DbUser')" -AsSecureString
$bstr   = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPwd)
try {
    $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
} finally {
    # Zéroïser la mémoire immédiatement après lecture
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

# ── Répertoire de déploiement protégé ────────────────────────────────────────
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path "$InstallDir\logs" | Out-Null

# Restreindre à Administrators + SYSTEM (le XML contiendra le mot de passe)
$acl = Get-Acl $InstallDir
$acl.SetAccessRuleProtection($true, $false)
foreach ($sid in $RestrictedSids) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $sid, "FullControl",
        "ContainerInherit,ObjectInherit", "None", "Allow")
    $acl.AddAccessRule($rule)
}
Set-Acl $InstallDir $acl

# ── Résoudre et copier le JAR ─────────────────────────────────────────────────
# Ordre de recherche :
#   1. Déjà présent dans $InstallDir (installeur .exe — NSIS a déjà tout copié)
#   2. Dossier parent du script      (ZIP distribué : jar à côté du dossier service/)
#   3. target/ Maven                 (déploiement depuis repo)
$jarDest = Join-Path $InstallDir "pharmasmart-batch.jar"

if (Test-Path $jarDest) {
    Write-Host "JAR présent : $jarDest"
} else {
    $jarSource = $null

    $jarInParent = Get-ChildItem (Join-Path (Split-Path -Parent $ScriptDir) "pharmaSmart-batch*.jar") `
        -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jarInParent) { $jarSource = $jarInParent.FullName }

    if (-not $jarSource) {
        $jarInTarget = Get-ChildItem (Join-Path $RepoRoot "pharmaSmart-batch\target\pharmaSmart-batch-*.jar") `
            -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($jarInTarget) { $jarSource = $jarInTarget.FullName }
    }

    if (-not $jarSource) {
        Write-Error "JAR introuvable. Exécuter : mvnw.cmd clean package -DskipTests -pl pharmaSmart-batch"
    }
    Copy-Item $jarSource $jarDest -Force
    Write-Host "JAR copié : $jarDest"
}

# ── WinSW ────────────────────────────────────────────────────────────────────
if (-not (Test-Path $WinSwExe)) {
    # Chercher parmi les candidates bundlées avant de télécharger
    $bundled = $WinSwCandidates | Where-Object { (Test-Path $_) -and ($_ -ne $WinSwExe) } | Select-Object -First 1
    if ($bundled) {
        Copy-Item (Resolve-Path $bundled).Path $WinSwExe -Force
        Write-Host "WinSW copié depuis : $bundled"
    } else {
        Write-Host "Téléchargement de WinSW..."
        try {
            Invoke-WebRequest "https://github.com/winsw/winsw/releases/latest/download/WinSW-x64.exe" `
                -OutFile $WinSwExe -UseBasicParsing -TimeoutSec 30
        } catch {
            Write-Error "Impossible de télécharger WinSW. Placer WinSW-x64.exe dans $InstallDir renommé pharmasmart-batch.exe."
        }
    }
}

# ── Désinstaller l'instance existante ────────────────────────────────────────
if (Get-Service -Name $ServiceName -ErrorAction SilentlyContinue) {
    Push-Location $InstallDir
    & ".\pharmasmart-batch.exe" stop  2>$null
    & ".\pharmasmart-batch.exe" uninstall
    Pop-Location
    Start-Sleep -Seconds 2
}

# ── XML WinSW ────────────────────────────────────────────────────────────────
# Le mot de passe est stocké dans le XML uniquement dans le répertoire protégé
# (Administrators + SYSTEM). L'alternative (variable d'env système) est moins
# sécurisée car visible de tous les processus de la machine.
$xmlPath = Join-Path $InstallDir "pharmasmart-batch.xml"

# ── Dépendance PostgreSQL (nom de service variable selon version, absent en Docker) ──
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Select-Object -First 1
$dependLine = if ($pgService) { "  <depend>$($pgService.Name)</depend>" } else { "" }
if ($pgService) { Write-Host "Service PostgreSQL détecté : $($pgService.Name) (dépendance ajoutée)" }
else { Write-Warning "Aucun service Windows PostgreSQL détecté — pas de dépendance ajoutée au service." }

Set-Content -Path $xmlPath -Encoding UTF8 -Value @"
<service>
  <id>$ServiceName</id>
  <name>PharmaSmart Batch</name>
  <description>PharmaSmart — Pipeline nocturne (SEMOIS, Classification ABC, Stock, Avoirs)</description>

  <executable>$JavaExe</executable>
  <arguments>-Xms$HeapMin -Xmx$HeapMax -Dspring.profiles.active=prod -Dpharma-smart.batch.active=true -Dfile.encoding=UTF-8 -jar "$jarDest"</arguments>

  <startmode>Automatic</startmode>
  <delayedAutoStart>true</delayedAutoStart>

  <onfailure action="restart" delay="30 sec"/>
  <onfailure action="restart" delay="60 sec"/>
  <onfailure action="none"/>

  <env name="PHARMA_DB_URL"      value="$DbUrl"/>
  <env name="PHARMA_DB_USER"     value="$DbUser"/>
  <env name="PHARMA_DB_PASSWORD" value="$DbPassword"/>
  <env name="PHARMA_DB_SCHEMA"   value="$DbSchema"/>

  <log mode="roll-by-size">
    <logpath>$InstallDir\logs</logpath>
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>5</keepFiles>
  </log>

  <stopTimeout>60 sec</stopTimeout>

$dependLine
</service>
"@

# Zéroïser la variable en mémoire dès que le fichier est écrit
$DbPassword = $null
[System.GC]::Collect()

# Appliquer les mêmes ACL restrictives sur le XML lui-même
$acl2 = Get-Acl $xmlPath
$acl2.SetAccessRuleProtection($true, $false)
foreach ($sid in $RestrictedSids) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $sid, "FullControl", "None", "None", "Allow")
    $acl2.AddAccessRule($rule)
}
Set-Acl $xmlPath $acl2

# ── Installation ─────────────────────────────────────────────────────────────
Push-Location $InstallDir
& ".\pharmasmart-batch.exe" install
Pop-Location

Write-Host "`nService '$ServiceName' installé dans $InstallDir." -ForegroundColor Green
Write-Host "Démarrer : Start-Service $ServiceName"
Write-Host "Journaux : $InstallDir\logs\"
