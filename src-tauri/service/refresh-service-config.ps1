#Requires -RunAsAdministrator
<#
.SYNOPSIS
    Relit config.json et met à jour la configuration du service pharmasmart-app
    sans désinstaller/réinstaller.

.DESCRIPTION
    Régénère le XML WinSW depuis config.json (port, heap, credentials DB),
    puis redémarre le service.
    À utiliser après toute modification de config.json.

.PARAMÈTRES
    -Restart  : $true (défaut) — redémarre le service après la mise à jour
    -DataDir  : Chemin vers le répertoire ProgramData\PharmaSmart
                (défaut : C:\ProgramData\PharmaSmart)

.EXEMPLES
    .\refresh-service-config.ps1
    .\refresh-service-config.ps1 -Restart:$false   # mettre à jour sans redémarrer
#>
param(
    [switch]$Restart = $true,
    [string]$DataDir = "$env:ProgramData\PharmaSmart"
)

$ErrorActionPreference = 'Stop'

$ServiceName = "pharmasmart-app"
$ServiceDir  = Join-Path $DataDir "service"
$XmlPath     = Join-Path $ServiceDir "pharmasmart-app.xml"

# ── Le service doit exister ───────────────────────────────────────────────────
if (-not (Get-Service -Name $ServiceName -ErrorAction SilentlyContinue)) {
    Write-Error "Service '$ServiceName' introuvable. Exécuter setup-backend-service.ps1 d'abord."
}

# ── Lecture du XML actuel pour récupérer java.exe et le chemin du JAR ────────
if (-not (Test-Path $XmlPath)) {
    Write-Error "XML WinSW introuvable : $XmlPath"
}
[xml]$xml = Get-Content $XmlPath -Encoding UTF8

$JavaExe = $xml.service.executable
$JarPath = ($xml.service.arguments -split ' ' | Where-Object { $_ -like '*.jar' })[0].Trim('"')

if (-not (Test-Path $JarPath)) {
    Write-Error "JAR introuvable : $JarPath  (chemin lu depuis le XML actuel)"
}

# ── Lecture config.json ───────────────────────────────────────────────────────
$Port           = 9080
$HeapMin        = "2g"
$HeapMax        = "2g"
$DbUrl          = "jdbc:postgresql://localhost:5432/pharma_smart"
$DbUser         = "pharma_smart"
$DbSchema       = "warehouse"
$DbPassword     = ""
$JavaHomeConfig = ""

$configCandidates = @(
    (Join-Path $DataDir "config.json"),
    (Join-Path (Split-Path -Parent $ServiceDir) "config.json")
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
        Write-Warning "Erreur lecture config.json : $_  — valeurs actuelles conservées."
    }
} else {
    Write-Warning "config.json introuvable — valeurs actuelles conservées."
}

# Si java_home dans config → remplace le chemin lu depuis l'XML
if ($JavaHomeConfig -and (Test-Path "$JavaHomeConfig\bin\java.exe")) {
    $JavaExe = "$JavaHomeConfig\bin\java.exe"
    Write-Host "Java (config.json java_home) : $JavaExe"
}

Write-Host "Port : $Port | Heap : $HeapMin/$HeapMax | DB URL : $DbUrl | User : $DbUser | Schema : $DbSchema"

# ── Construire les entrées <env> ──────────────────────────────────────────────
$envDbEntries = @"
  <env name="PHARMA_DB_URL"    value="$DbUrl"/>
  <env name="PHARMA_DB_USER"   value="$DbUser"/>
  <env name="PHARMA_DB_SCHEMA" value="$DbSchema"/>
"@
if ($DbPassword) {
    $envDbEntries += "  <env name=`"PHARMA_DB_PASSWORD`" value=`"$DbPassword`"/>`n"
    Write-Host "Mot de passe DB : mis à jour depuis config.json"
} else {
    Write-Host "Mot de passe DB : non défini dans config.json — variable système PHARMA_DB_PASSWORD utilisée"
}

# ── Réécrire le XML ───────────────────────────────────────────────────────────
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

# Zéroïser le mot de passe
$DbPassword = $null
[System.GC]::Collect()

# ACL restrictives sur le XML
$acl = Get-Acl $XmlPath
$acl.SetAccessRuleProtection($true, $false)
foreach ($principal in @("BUILTIN\Administrators", "NT AUTHORITY\SYSTEM")) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $principal, "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
    $acl.AddAccessRule($rule)
}
Set-Acl $XmlPath $acl

Write-Host "XML mis à jour : $XmlPath" -ForegroundColor Green

# ── Redémarrage ───────────────────────────────────────────────────────────────
if ($Restart) {
    Write-Host "Redémarrage du service '$ServiceName'..." -NoNewline
    Restart-Service -Name $ServiceName -Force
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host "Redémarrage différé — les modifications seront prises en compte au prochain démarrage du service." -ForegroundColor Yellow
}
