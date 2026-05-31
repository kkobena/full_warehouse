<#
.SYNOPSIS
    Installe le backend pharmaSmart-app comme service Windows (WinSW).
    Appelé par l'action personnalisée WiX PS_InstallBackendService.

    Toute la configuration est lue depuis config.json
    (C:\ProgramData\PharmaSmart\config.json) si le fichier existe.
    Priorité de lecture :
      1. config.json dans ProgramData\PharmaSmart  ← écrit par le MSI
      2. config.json à côté de l'exe (INSTALLDIR)
      3. Valeurs par défaut codées dans ce script

    Mot de passe DB :
      - Si config.json contient database.password non vide → injecté dans le XML
        (fichier protégé Administrators+SYSTEM par NTFS ACL)
      - Sinon → entrée absente du XML ; le service hérite de la variable
        système PHARMA_DB_PASSWORD définie par l'administrateur
#>
$ErrorActionPreference = 'Stop'

$ServiceName  = "pharmasmart-app"
$ResourcesDir = Split-Path -Parent $PSScriptRoot           # [INSTALLDIR]\resources
$SidecarDir   = Join-Path $ResourcesDir "sidecar"
$ServiceDir   = Join-Path $env:ProgramData "PharmaSmart\service"
$DataDir      = Join-Path $env:ProgramData "PharmaSmart"

# ── Lecture config.json ───────────────────────────────────────────────────────
# Valeurs par défaut (appliquées si la clé est absente/vide dans config.json)
$Port           = 9080
$HeapMin        = "2g"
$HeapMax        = "2g"
$DbUrl          = "jdbc:postgresql://localhost:5432/pharma_smart"
$DbUser         = "pharma_smart"
$DbSchema       = "pharma_smart"
$DbPassword     = ""      # vide = la variable système PHARMA_DB_PASSWORD est utilisée
$JavaHomeConfig = ""      # jvm.java_home depuis config (priorité sur la détection sidecar)

# Chercher config.json : ProgramData d'abord, puis dossier exe (INSTALLDIR)
$configCandidates = @(
    (Join-Path $DataDir "config.json"),
    (Join-Path (Split-Path -Parent $ResourcesDir) "config.json")
)
$configJson = $configCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($configJson) {
    Write-Host "Lecture config.json : $configJson"
    try {
        $cfg = Get-Content $configJson -Raw | ConvertFrom-Json

        if ($cfg.server -and $cfg.server.port)                          { $Port           = [int]$cfg.server.port }
        if ($cfg.jvm -and $cfg.jvm.java_home)                           { $JavaHomeConfig = $cfg.jvm.java_home }
        if ($cfg.jvm -and $cfg.jvm.app -and $cfg.jvm.app.heap_min)      { $HeapMin        = $cfg.jvm.app.heap_min }
        if ($cfg.jvm -and $cfg.jvm.app -and $cfg.jvm.app.heap_max)      { $HeapMax        = $cfg.jvm.app.heap_max }
        if ($cfg.database -and $cfg.database.url)                       { $DbUrl          = $cfg.database.url }
        if ($cfg.database -and $cfg.database.username)                  { $DbUser         = $cfg.database.username }
        if ($cfg.database -and $cfg.database.schema)                    { $DbSchema       = $cfg.database.schema }
        if ($cfg.database -and $cfg.database.password)                  { $DbPassword     = $cfg.database.password }
    } catch {
        Write-Warning "Erreur lors de la lecture de config.json : $_  — valeurs par défaut utilisées."
    }
} else {
    Write-Warning "config.json introuvable — valeurs par défaut utilisées."
}

Write-Host "Port : $Port | Heap : $HeapMin/$HeapMax | DB : $DbUrl | User : $DbUser | Schema : $DbSchema"
if ($DbPassword) {
    Write-Host "Mot de passe DB : lu depuis config.json (sera protégé par ACL)"
} else {
    Write-Host "Mot de passe DB : non défini dans config.json — la variable système PHARMA_DB_PASSWORD sera utilisée"
}

# ── 1. java.exe ──────────────────────────────────────────────────────────────
# Priorité : 1) jvm.java_home (config.json — inclut le sidecar si écrit par installer.nsh)
#            2) sidecar détecté localement  3) JAVA_HOME env  4) PATH
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

# ── 2. JAR sidecar ───────────────────────────────────────────────────────────
$jar = Get-ChildItem (Join-Path $SidecarDir "pharmaSmart-app-*.jar") -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jar) {
    Write-Error "JAR introuvable dans $SidecarDir. Installation du service abandonnée."
}
$JarPath = $jar.FullName
Write-Host "JAR : $JarPath"

# ── 3. Répertoire de service dans ProgramData (protégé) ──────────────────────
New-Item -ItemType Directory -Force -Path $ServiceDir | Out-Null

$acl = Get-Acl $ServiceDir
$acl.SetAccessRuleProtection($true, $false)
foreach ($principal in @("BUILTIN\Administrators", "NT AUTHORITY\SYSTEM")) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $principal, "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
    $acl.AddAccessRule($rule)
}
Set-Acl $ServiceDir $acl

# ── 4. WinSW ─────────────────────────────────────────────────────────────────
$WinSwExe = Join-Path $ServiceDir "pharmasmart-app.exe"
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
            Write-Warning "Placer WinSW-x64.exe dans $ServiceDir renommé en pharmasmart-app.exe, puis relancer ce script."
            exit 1
        }
    }
}

# ── 5. Désinstaller l'instance précédente si elle existe ─────────────────────
$existing = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Service existant — arrêt et désinstallation..."
    Push-Location $ServiceDir
    & ".\pharmasmart-app.exe" stop  2>$null
    & ".\pharmasmart-app.exe" uninstall
    Pop-Location
    Start-Sleep -Seconds 2
}

# ── 6. Construire les entrées <env> pour la base de données ──────────────────
# PHARMA_DB_URL, PHARMA_DB_USER, PHARMA_DB_SCHEMA : toujours injectés depuis config.json
# PHARMA_DB_PASSWORD : injecté seulement si non vide dans config.json ;
#   sinon le service hérite de la variable d'environnement système
$envDbEntries = @"
  <env name="PHARMA_DB_URL"    value="$DbUrl"/>
  <env name="PHARMA_DB_USER"   value="$DbUser"/>
  <env name="PHARMA_DB_SCHEMA" value="$DbSchema"/>
"@
if ($DbPassword) {
    $envDbEntries += "  <env name=`"PHARMA_DB_PASSWORD`" value=`"$DbPassword`"/>`n"
}

# ── 7. XML WinSW ─────────────────────────────────────────────────────────────
$XmlPath = Join-Path $ServiceDir "pharmasmart-app.xml"
$logsDir = Join-Path $DataDir "logs"

Set-Content -Path $XmlPath -Encoding UTF8 -Value @"
<service>
  <id>$ServiceName</id>
  <name>PharmaSmart Application</name>
  <description>PharmaSmart — Gestion pharmaceutique, API web (port $Port)</description>

  <executable>$JavaExe</executable>
  <arguments>-Xms$HeapMin -Xmx$HeapMax -Dspring.profiles.active=prod,standalone -Dfile.encoding=UTF-8 -Djava.awt.headless=true -jar "$JarPath" --server.port=$Port</arguments>

  <startmode>Automatic</startmode>
  <delayedAutoStart>true</delayedAutoStart>

  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="30 sec"/>
  <onfailure action="none"/>

$envDbEntries
  <logmode>rotate</logmode>
  <logpath>$logsDir</logpath>
  <log mode="rotate">
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>5</keepFiles>
  </log>

  <stopTimeout>30 sec</stopTimeout>

  <depend>postgresql-x64-18</depend>
</service>
"@

# Zéroïser le mot de passe de la mémoire
$DbPassword = $null
[System.GC]::Collect()

# ACL restrictives sur le XML (peut contenir le mot de passe)
$acl = Get-Acl $XmlPath
$acl.SetAccessRuleProtection($true, $false)
foreach ($principal in @("BUILTIN\Administrators", "NT AUTHORITY\SYSTEM")) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $principal, "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
    $acl.AddAccessRule($rule)
}
Set-Acl $XmlPath $acl

# ── 8. Installation ───────────────────────────────────────────────────────────
Push-Location $ServiceDir
& ".\pharmasmart-app.exe" install
$code = $LASTEXITCODE
Pop-Location

if ($code -ne 0) {
    Write-Error "Échec de l'installation du service (code $code)."
}
Write-Host "Service '$ServiceName' installé (démarrage automatique différé)."
