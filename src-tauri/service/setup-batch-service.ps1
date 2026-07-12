<#
.SYNOPSIS
    Installe le pipeline pharmaSmart-batch comme service Windows (WinSW).
    Appelé par installer.nsh juste après setup-backend-service.ps1, en
    réutilisant la même config.json (mêmes identifiants DB que l'application).

    Toute la configuration est lue depuis config.json
    (C:\ProgramData\PharmaSmart\config.json) si le fichier existe.
    Priorité de lecture :
      1. config.json dans ProgramData\PharmaSmart  ← écrit par l'installeur
      2. config.json à côté de l'exe (INSTALLDIR)
      3. Valeurs par défaut codées dans ce script

    Mot de passe DB :
      - Si config.json contient database.password non vide → injecté dans le XML
        (fichier protégé Administrators+SYSTEM par NTFS ACL)
      - Sinon → entrée absente du XML ; le service hérite de la variable
        système PHARMA_DB_PASSWORD définie par l'administrateur
#>
param(
    [string]$NsisJavaExe = "",
    [string]$NsisJarPath = "",
    [string]$DataDir     = ""
)

$ErrorActionPreference = 'Stop'

# Identités "Administrators" / "SYSTEM" résolues via leur SID bien connu plutôt
# que par leur nom : NTAccount.Translate() (utilisé quand on passe une chaîne à
# FileSystemAccessRule) peut échouer avec IdentityNotMappedException
# ("Impossible de traduire certaines ou toutes les références d'identité") dans
# certains contextes d'installation (LSA restreinte, session SYSTEM précoce...).
$RestrictedSids = @(
    (New-Object System.Security.Principal.SecurityIdentifier([System.Security.Principal.WellKnownSidType]::BuiltinAdministratorsSid, $null)),
    (New-Object System.Security.Principal.SecurityIdentifier([System.Security.Principal.WellKnownSidType]::LocalSystemSid, $null))
)

$ServiceName  = "pharmasmart-batch"
$ResourcesDir = Split-Path -Parent $PSScriptRoot           # [INSTALLDIR]\resources
$SidecarDir   = Join-Path $ResourcesDir "sidecar"
if (-not $DataDir) { $DataDir = Join-Path $env:ProgramData "PharmaSmart" }
$ServiceDir   = Join-Path $DataDir "batch"

# ── Lecture config.json ───────────────────────────────────────────────────────
$HeapMin        = "128m"
$HeapMax        = "512m"
$DbUrl          = "jdbc:postgresql://localhost:5432/pharma_smart"
$DbUser         = "pharma_smart"
$DbSchema       = "pharma_smart"
$DbPassword     = ""      # vide = la variable système PHARMA_DB_PASSWORD est utilisée
$JavaHomeConfig = ""      # jvm.java_home depuis config (priorité sur la détection sidecar)

$configCandidates = @(
    (Join-Path $DataDir "config.json"),
    (Join-Path $ResourcesDir "config.json")
)
$configJson = $configCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($configJson) {
    Write-Host "Lecture config.json : $configJson"
    try {
        $cfg = Get-Content $configJson -Raw | ConvertFrom-Json

        if ($cfg.jvm -and $cfg.jvm.java_home)                             { $JavaHomeConfig = $cfg.jvm.java_home }
        if ($cfg.jvm -and $cfg.jvm.batch -and $cfg.jvm.batch.heap_min)    { $HeapMin        = $cfg.jvm.batch.heap_min }
        if ($cfg.jvm -and $cfg.jvm.batch -and $cfg.jvm.batch.heap_max)    { $HeapMax        = $cfg.jvm.batch.heap_max }
        if ($cfg.database -and $cfg.database.url)                        { $DbUrl          = $cfg.database.url }
        if ($cfg.database -and $cfg.database.username)                   { $DbUser         = $cfg.database.username }
        if ($cfg.database -and $cfg.database.schema)                     { $DbSchema       = $cfg.database.schema }
        if ($cfg.database -and $cfg.database.password)                   { $DbPassword     = $cfg.database.password }
    } catch {
        Write-Warning "Erreur lors de la lecture de config.json : $_  — valeurs par défaut utilisées."
    }
} else {
    Write-Warning "config.json introuvable — valeurs par défaut utilisées."
}

Write-Host "Heap : $HeapMin/$HeapMax | DB : $DbUrl | User : $DbUser | Schema : $DbSchema"
if ($DbPassword) {
    Write-Host "Mot de passe DB : lu depuis config.json (sera protégé par ACL)"
} else {
    Write-Host "Mot de passe DB : non défini dans config.json — la variable système PHARMA_DB_PASSWORD sera utilisée"
}

# ── 1. java.exe ──────────────────────────────────────────────────────────────
# Priorité : 1) paramètre -NsisJavaExe (partagé avec le service app)
#            2) jvm.java_home dans config.json  3) sidecar  4) JAVA_HOME  5) PATH
$JavaExe = $NsisJavaExe
if (-not $JavaExe) {
    if ($JavaHomeConfig -and (Test-Path "$JavaHomeConfig\bin\java.exe")) {
        $JavaExe = "$JavaHomeConfig\bin\java.exe"
        Write-Host "Java (config.json java_home) : $JavaExe"
    } else {
        $bundledJava = Join-Path $SidecarDir "jre\bin\java.exe"
        if (Test-Path $bundledJava) {
            $JavaExe = $bundledJava
        } elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
            $JavaExe = "$env:JAVA_HOME\bin\java.exe"
        } else {
            $JavaExe = "java"
        }
        Write-Host "Java : $JavaExe"
    }
} else {
    Write-Host "Java (paramètre NSIS) : $JavaExe"
}

# ── 2. JAR sidecar ───────────────────────────────────────────────────────────
$JarPath = $NsisJarPath
if (-not $JarPath) {
    $jar = Get-ChildItem (Join-Path $SidecarDir "pharmaSmart-batch-*.jar") -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jar) {
        Write-Error "JAR batch introuvable dans $SidecarDir. Installation du service abandonnée."
    }
    $JarPath = $jar.FullName
}
Write-Host "JAR : $JarPath"

# ── 3. Répertoire de service dans ProgramData (protégé) ──────────────────────
New-Item -ItemType Directory -Force -Path $ServiceDir | Out-Null
# NOTE : logs WinSW dans $ServiceDir\logs (batch\logs), PAS $DataDir\logs — ce
# dernier est déjà utilisé par le service pharmasmart-app (setup-backend-service.ps1).
# Partager le même dossier entre les deux services mélange leurs sorties
# stdout/stderr et rend le diagnostic impossible (cf. incident installation 0.2.4).
New-Item -ItemType Directory -Force -Path (Join-Path $ServiceDir "logs") | Out-Null

$acl = Get-Acl $ServiceDir
$acl.SetAccessRuleProtection($true, $false)
foreach ($sid in $RestrictedSids) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $sid, "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
    $acl.AddAccessRule($rule)
}
Set-Acl $ServiceDir $acl

# ── 4. WinSW ─────────────────────────────────────────────────────────────────
# Réutilise le même binaire WinSW que le service app (bundlé comme resource unique).
$WinSwExe = Join-Path $ServiceDir "pharmasmart-batch.exe"
$WinSwSrc = Join-Path $PSScriptRoot "WinSW.exe"

if (-not (Test-Path $WinSwExe)) {
    if (Test-Path $WinSwSrc) {
        Copy-Item $WinSwSrc $WinSwExe
    } else {
        Write-Host "Téléchargement de WinSW..."
        try {
            $url = "https://github.com/winsw/winsw/releases/latest/download/WinSW-x64.exe"
            Invoke-WebRequest -Uri $url -OutFile $WinSwExe -UseBasicParsing -TimeoutSec 30
            Write-Host "WinSW téléchargé."
        } catch {
            Write-Warning "Impossible de télécharger WinSW : $_"
            Write-Warning "Placer WinSW-x64.exe dans $ServiceDir renommé en pharmasmart-batch.exe, puis relancer ce script."
            exit 1
        }
    }
}

# ── 5. Désinstaller l'instance précédente si elle existe ─────────────────────
$existing = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Service existant — arrêt et désinstallation..."
    Push-Location $ServiceDir
    & ".\pharmasmart-batch.exe" stop  2>$null
    & ".\pharmasmart-batch.exe" uninstall
    Pop-Location
    Start-Sleep -Seconds 2
}

# ── 6. Construire les entrées <env> pour la base de données ──────────────────
$envDbEntries = @"
  <env name="PHARMA_DB_URL"    value="$DbUrl"/>
  <env name="PHARMA_DB_USER"   value="$DbUser"/>
  <env name="PHARMA_DB_SCHEMA" value="$DbSchema"/>
"@
if ($DbPassword) {
    $envDbEntries += "  <env name=`"PHARMA_DB_PASSWORD`" value=`"$DbPassword`"/>`n"
}

# ── 6bis. Dépendance PostgreSQL ───────────────────────────────────────────────
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Select-Object -First 1
$dependLine = if ($pgService) { "  <depend>$($pgService.Name)</depend>" } else { "" }
if ($pgService) { Write-Host "Service PostgreSQL détecté : $($pgService.Name) (dépendance ajoutée)" }
else { Write-Warning "Aucun service Windows PostgreSQL détecté — pas de dépendance ajoutée au service." }

# ── 7. XML WinSW ─────────────────────────────────────────────────────────────
$XmlPath = Join-Path $ServiceDir "pharmasmart-batch.xml"
$logsDir = Join-Path $ServiceDir "logs"

Set-Content -Path $XmlPath -Encoding UTF8 -Value @"
<service>
  <id>$ServiceName</id>
  <name>PharmaSmart Batch</name>
  <description>PharmaSmart — Pipeline nocturne (SEMOIS, Classification ABC, Stock, Avoirs)</description>

  <executable>$JavaExe</executable>
  <arguments>-Xms$HeapMin -Xmx$HeapMax -Dspring.profiles.active=prod -Dpharma-smart.batch.active=true -Dfile.encoding=UTF-8 -jar "$JarPath"</arguments>

  <startmode>Automatic</startmode>
  <delayedAutoStart>true</delayedAutoStart>

  <onfailure action="restart" delay="30 sec"/>
  <onfailure action="restart" delay="60 sec"/>
  <onfailure action="none"/>

$envDbEntries
  <log mode="roll-by-size">
    <logpath>$logsDir</logpath>
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>5</keepFiles>
  </log>

  <stopTimeout>60 sec</stopTimeout>

$dependLine
</service>
"@

# Zéroïser le mot de passe de la mémoire
$DbPassword = $null
[System.GC]::Collect()

# ACL restrictives sur le XML (peut contenir le mot de passe)
# NOTE : $XmlPath est un FICHIER, pas un dossier — les indicateurs d'héritage
# (ContainerInherit/ObjectInherit) n'ont de sens que pour un dossier et
# FileSecurity.AddAccessRule() lève une ArgumentException ("Aucun indicateur ne
# peut être défini") si on les passe sur un fichier. Doit rester "None","None".
$acl = Get-Acl $XmlPath
$acl.SetAccessRuleProtection($true, $false)
foreach ($sid in $RestrictedSids) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $sid, "FullControl", "None", "None", "Allow")
    $acl.AddAccessRule($rule)
}
Set-Acl $XmlPath $acl

# ── 8. Installation ───────────────────────────────────────────────────────────
Push-Location $ServiceDir
& ".\pharmasmart-batch.exe" install
$code = $LASTEXITCODE
Pop-Location

if ($code -ne 0) {
    Write-Error "Échec de l'installation du service (code $code)."
}
Write-Host "Service '$ServiceName' installé (démarrage automatique différé)."
