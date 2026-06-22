#Requires -RunAsAdministrator
<#
.SYNOPSIS
    Point d'entrée unique — installe, désinstalle, surveille et maintient
    tous les composants PharmaSmart (app, batch, backup).

.DESCRIPTION
    Lit sa configuration depuis config.json (ou config.default.json).
    Source de vérité unique : jvm.java_home, jvm.app.*, jvm.batch.*,
    database.*, server.port.

.PARAMÈTRES
    -Action     : install | uninstall | status | update-jvm | refresh-config
    -Target     : all | app | batch | backup
    -ConfigFile : chemin explicite vers config.json (optionnel)
    -AppDir     : répertoire du service app   (défaut : C:\PharmaSmartApp)
    -BatchDir   : répertoire du service batch (défaut : C:\PharmaSmartBatch)
    -KeepFiles  : (uninstall) conserver JARs et logs
    -NoRestart  : ne pas redémarrer les services après update-jvm/refresh-config

.EXEMPLES
    .\pharmasmart-setup.ps1 install
    .\pharmasmart-setup.ps1 install -Target app
    .\pharmasmart-setup.ps1 install -Target batch
    .\pharmasmart-setup.ps1 install -Target backup
    .\pharmasmart-setup.ps1 uninstall
    .\pharmasmart-setup.ps1 uninstall -Target batch -KeepFiles
    .\pharmasmart-setup.ps1 status
    .\pharmasmart-setup.ps1 update-jvm
    .\pharmasmart-setup.ps1 refresh-config
#>
param(
    [ValidateSet("install","uninstall","status","update-jvm","refresh-config")]
    [string]$Action = "install",
    [ValidateSet("all","app","batch","backup")]
    [string]$Target = "all",
    [string]$ConfigFile = "",
    [string]$AppDir     = "C:\PharmaSmartApp",
    [string]$BatchDir   = "C:\PharmaSmartBatch",
    [switch]$KeepFiles,
    [switch]$NoRestart
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent $ScriptDir

$APP_SVC_NAME   = "pharmasmart-app"
$BATCH_SVC_NAME = "pharmasmart-batch"
$BACKUP_TASKS   = @(
    "PharmaSmart_Backup_Dump",
    "PharmaSmart_Backup_Base",
    "PharmaSmart_Backup_Purge",
    "PharmaSmart_Backup_Check"
)

# ── Charger config.json ───────────────────────────────────────────────────────
function Load-Config {
    if ($ConfigFile -and (Test-Path $ConfigFile)) {
        Write-Host "Config : $ConfigFile"
        return (Get-Content $ConfigFile -Raw | ConvertFrom-Json)
    }
    $candidates = @(
        (Join-Path $ScriptDir "..\config.json"),
        (Join-Path $ScriptDir "..\config.default.json"),
        (Join-Path $env:ProgramData "PharmaSmart\config.json")
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) {
            Write-Host "Config : $(Resolve-Path $c)"
            return (Get-Content $c -Raw | ConvertFrom-Json)
        }
    }
    Write-Warning "config.json introuvable — valeurs par défaut utilisées."
    return [PSCustomObject]@{}
}

# ── Résoudre java.exe ─────────────────────────────────────────────────────────
# Priorité : 1) jvm.java_home dans config  2) JAVA_HOME env  3) PATH
function Resolve-Java {
    param($cfg)
    $jh = try { $cfg.jvm.java_home } catch { "" }
    if ($jh -and (Test-Path "$jh\bin\java.exe")) {
        Write-Host "Java (config java_home) : $jh\bin\java.exe"
        return "$jh\bin\java.exe"
    }
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        Write-Host "Java (JAVA_HOME) : $env:JAVA_HOME\bin\java.exe"
        return "$env:JAVA_HOME\bin\java.exe"
    }
    Write-Host "Java (PATH) : java"
    return "java"
}

# ── Trouver un JAR ────────────────────────────────────────────────────────────
function Find-Jar {
    param([string]$Pattern, [string]$Module)
    # 1. ZIP layout : JAR au même niveau que service/ (dossier parent du script)
    $jar = Get-ChildItem (Join-Path $ScriptDir "..\$Pattern") -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
    if ($jar) { return $jar.FullName }
    # 2. Maven target/ (dev depuis le repo)
    $jar = Get-ChildItem (Join-Path $RepoRoot "$Module\target\$Pattern") -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
    if ($jar) { return $jar.FullName }
    return $null
}

# ── Copier ou télécharger WinSW ───────────────────────────────────────────────
function Get-WinSW {
    param([string]$TargetDir, [string]$SvcName)
    $dest = Join-Path $TargetDir "$SvcName.exe"
    if (Test-Path $dest) { return $dest }
    $src = @(
        (Join-Path $ScriptDir "WinSW.exe"),
        (Join-Path $ScriptDir "..\WinSW.exe")
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($src) { Copy-Item $src $dest -Force; return $dest }
    Write-Host "Téléchargement de WinSW..."
    Invoke-WebRequest "https://github.com/winsw/winsw/releases/latest/download/WinSW-x64.exe" `
        -OutFile $dest -UseBasicParsing -TimeoutSec 30
    return $dest
}

# ── Appliquer ACL restrictive (Administrators + SYSTEM uniquement) ─────────────
function Set-RestrictedAcl {
    param([string]$Path, [bool]$Container = $true)
    $inherit = if ($Container) { "ContainerInherit,ObjectInherit" } else { "None" }
    $acl = Get-Acl $Path
    $acl.SetAccessRuleProtection($true, $false)
    foreach ($p in @("BUILTIN\Administrators","NT AUTHORITY\SYSTEM")) {
        $acl.AddAccessRule((New-Object System.Security.AccessControl.FileSystemAccessRule(
            $p, "FullControl", $inherit, "None", "Allow")))
    }
    Set-Acl $Path $acl
}

# ── Prompt mot de passe sécurisé ──────────────────────────────────────────────
function Get-DbPassword {
    param([string]$User)
    $sec  = Read-Host "Mot de passe PostgreSQL (utilisateur '$User')" -AsSecureString
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try   { return [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr) }
    finally { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

# ── Installer un service (app ou batch) ───────────────────────────────────────
function Install-Svc {
    param(
        [string]$Name, [string]$Dir, [string]$JarPath, [string]$JavaExe,
        [string]$XmlTemplate, [string]$HeapMin, [string]$HeapMax,
        [string]$DbUrl, [string]$DbUser, [string]$DbPassword, [string]$DbSchema,
        [string]$Port = ""
    )
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $Dir        | Out-Null
    New-Item -ItemType Directory -Force -Path "$Dir\logs" | Out-Null
    Set-RestrictedAcl $Dir

    $jarDest = Join-Path $Dir "$Name.jar"
    Copy-Item $JarPath $jarDest -Force
    Write-Host "  JAR : $jarDest"

    $xml = (Get-Content $XmlTemplate -Raw) `
        -replace 'PLACEHOLDER_JAVA',        $JavaExe `
        -replace 'PLACEHOLDER_HEAP_MIN',    $HeapMin `
        -replace 'PLACEHOLDER_HEAP_MAX',    $HeapMax `
        -replace 'PLACEHOLDER_DB_URL',      $DbUrl `
        -replace 'PLACEHOLDER_DB_USER',     $DbUser `
        -replace 'PLACEHOLDER_DB_PASSWORD', $DbPassword `
        -replace 'PLACEHOLDER_DB_SCHEMA',   $DbSchema
    if ($Port) { $xml = $xml -replace 'PLACEHOLDER_PORT', $Port }
    $xmlPath = Join-Path $Dir "$Name.xml"
    Set-Content $xmlPath $xml -Encoding UTF8
    Set-RestrictedAcl $xmlPath $false

    Get-WinSW $Dir $Name | Out-Null
    $winswExe = Join-Path $Dir "$Name.exe"
    if (-not (Test-Path $winswExe)) {
        throw "Impossible d'installer '$Name' : binaire WinSW absent ($winswExe)."
    }

    if (Get-Service -Name $Name -ErrorAction SilentlyContinue) {
        Write-Host "  Remplacement de l'instance existante..."
        Push-Location $Dir
        try {
            & ".\$Name.exe" stop 2>$null
            & ".\$Name.exe" uninstall
        } finally {
            Pop-Location
        }
        Start-Sleep -Seconds 2
    }
    Push-Location $Dir
    try {
        & ".\$Name.exe" install
        if ($LASTEXITCODE -ne 0) {
            throw "WinSW 'install' a retourné le code $LASTEXITCODE pour '$Name'."
        }
    } finally {
        Pop-Location
    }
    Write-Host "  OK : '$Name' installé dans $Dir" -ForegroundColor Green
}

# ── Désinstaller un service ────────────────────────────────────────────────────
function Uninstall-Svc {
    param([string]$Name, [string]$Dir)
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    $svc = Get-Service -Name $Name -ErrorAction SilentlyContinue
    if ($svc) {
        $exe = Join-Path $Dir "$Name.exe"
        if (Test-Path $exe) {
            Push-Location $Dir
            & ".\$Name.exe" stop 2>$null
            & ".\$Name.exe" uninstall
            Pop-Location
        } else {
            Stop-Service $Name -Force -ErrorAction SilentlyContinue
            & sc.exe delete $Name
        }
        Write-Host "  '$Name' désinstallé." -ForegroundColor Green
    } else {
        Write-Host "  '$Name' non trouvé — ignoré."
    }
    if (-not $KeepFiles -and (Test-Path $Dir)) {
        Remove-Item $Dir -Recurse -Force
        Write-Host "  Répertoire $Dir supprimé."
    }
}

# ── Patcher heap JVM dans un XML WinSW existant ───────────────────────────────
function Patch-Heap {
    param([string]$XmlPath, [string]$SvcName, [string]$HeapMin, [string]$HeapMax)
    if (-not (Test-Path $XmlPath)) { Write-Warning "XML introuvable : $XmlPath — $SvcName ignoré."; return $false }
    $before = Get-Content $XmlPath -Raw -Encoding UTF8
    $after  = $before `
        -replace '(?<=-Xms)\S+(?=\s)',      $HeapMin `
        -replace '(?<=-Xmx)\S+(?=[\s"])',   $HeapMax
    if ($before -eq $after) { Write-Host "  $SvcName : déjà à $HeapMin/$HeapMax." -ForegroundColor Yellow; return $false }
    Set-Content $XmlPath $after -Encoding UTF8
    Write-Host "  $SvcName : heap → -Xms$HeapMin -Xmx$HeapMax" -ForegroundColor Green
    return $true
}

# ── Afficher l'état de tous les composants ────────────────────────────────────
function Show-Status {
    Write-Host "`n=== Services Windows ===" -ForegroundColor Cyan
    foreach ($name in @($APP_SVC_NAME, $BATCH_SVC_NAME)) {
        $svc = Get-Service -Name $name -ErrorAction SilentlyContinue
        if ($svc) {
            $color = if ($svc.Status -eq "Running") { "Green" } else { "Yellow" }
            Write-Host "  $name : $($svc.Status)" -ForegroundColor $color
        } else {
            Write-Host "  $name : non installé" -ForegroundColor Gray
        }
    }
    Write-Host "`n=== Tâches planifiées backup ===" -ForegroundColor Cyan
    foreach ($t in $BACKUP_TASKS) {
        $task = Get-ScheduledTask -TaskName $t -ErrorAction SilentlyContinue
        if ($task) { Write-Host "  $t : $($task.State)" }
        else        { Write-Host "  $t : non enregistrée" -ForegroundColor Gray }
    }
    Write-Host "`n=== Configuration active ===" -ForegroundColor Cyan
    $cfg = Load-Config
    Write-Host "  Java      : $(Resolve-Java $cfg)"
    $appHeap   = try { "$($cfg.jvm.app.heap_min)/$($cfg.jvm.app.heap_max)" }   catch { "2g/2g (défaut)" }
    $batchHeap = try { "$($cfg.jvm.batch.heap_min)/$($cfg.jvm.batch.heap_max)" } catch { "128m/512m (défaut)" }
    $port      = try { $cfg.server.port } catch { 9080 }
    $dbUrl     = try { $cfg.database.url } catch { "(non défini)" }
    Write-Host "  App heap  : $appHeap | port $port"
    Write-Host "  Batch heap: $batchHeap"
    Write-Host "  DB URL    : $dbUrl"
}

# ════════════════════════════════════════════════════════════════════════════════
# CHARGEMENT DE LA CONFIG ET DES VALEURS EFFECTIVES
# ════════════════════════════════════════════════════════════════════════════════
$cfg = Load-Config

$JavaExe      = Resolve-Java $cfg
$Port         = try { [string]$cfg.server.port }            catch { "9080" }
$DbUrl        = try { $cfg.database.url }                   catch { "jdbc:postgresql://localhost:5432/pharma_smart" }
$DbUser       = try { $cfg.database.username }              catch { "pharma_smart" }
$DbSchema     = try { $cfg.database.schema }                catch { "pharma_smart" }
$AppHeapMin   = try { $cfg.jvm.app.heap_min }               catch { "2g" }
$AppHeapMax   = try { $cfg.jvm.app.heap_max }               catch { "2g" }
$BatchHeapMin = try { $cfg.jvm.batch.heap_min }             catch { "128m" }
$BatchHeapMax = try { $cfg.jvm.batch.heap_max }             catch { "512m" }
if (-not $Port)         { $Port         = "9080" }
if (-not $DbUrl)        { $DbUrl        = "jdbc:postgresql://localhost:5432/pharma_smart" }
if (-not $DbUser)       { $DbUser       = "pharma_smart" }
if (-not $DbSchema)     { $DbSchema     = "pharma_smart" }
if (-not $AppHeapMin)   { $AppHeapMin   = "1g" }
if (-not $AppHeapMax)   { $AppHeapMax   = "2g" }
if (-not $BatchHeapMin) { $BatchHeapMin = "128m" }
if (-not $BatchHeapMax) { $BatchHeapMax = "512m" }

# ════════════════════════════════════════════════════════════════════════════════
# ACTION : status
# ════════════════════════════════════════════════════════════════════════════════
if ($Action -eq "status") {
    Show-Status
    exit 0
}

# ════════════════════════════════════════════════════════════════════════════════
# ACTION : update-jvm
# ════════════════════════════════════════════════════════════════════════════════
if ($Action -eq "update-jvm") {
    Write-Host "`n=== Mise à jour heap JVM ===" -ForegroundColor Cyan
    $changed = $false
    if ($Target -in "all","app") {
        $c = Patch-Heap (Join-Path $AppDir "pharmasmart-app.xml") $APP_SVC_NAME $AppHeapMin $AppHeapMax
        if ($c) { $changed = $true }
    }
    if ($Target -in "all","batch") {
        $c = Patch-Heap (Join-Path $BatchDir "pharmasmart-batch.xml") $BATCH_SVC_NAME $BatchHeapMin $BatchHeapMax
        if ($c) { $changed = $true }
    }
    if ($changed -and -not $NoRestart) {
        if ($Target -in "all","app")   { Restart-Service $APP_SVC_NAME   -Force -ErrorAction SilentlyContinue }
        if ($Target -in "all","batch") { Restart-Service $BATCH_SVC_NAME -Force -ErrorAction SilentlyContinue }
        Write-Host "Services redémarrés." -ForegroundColor Green
    } elseif (-not $changed) {
        Write-Host "Aucun changement nécessaire."
    } else {
        Write-Host "Changements appliqués — redémarrage différé (-NoRestart)." -ForegroundColor Yellow
    }
    exit 0
}

# ════════════════════════════════════════════════════════════════════════════════
# ACTION : uninstall
# ════════════════════════════════════════════════════════════════════════════════
if ($Action -eq "uninstall") {
    # Batch en premier (dépend de app pour la DB)
    if ($Target -in "all","batch") { Uninstall-Svc $BATCH_SVC_NAME $BatchDir }
    if ($Target -in "all","app")   { Uninstall-Svc $APP_SVC_NAME   $AppDir }
    if ($Target -in "all","backup") {
        Write-Host "`n=== Tâches backup ===" -ForegroundColor Cyan
        $rm = @(
            (Join-Path $ScriptDir "..\src-backup\scripts\remove-backup-tasks.ps1"),
            (Join-Path $ScriptDir "remove-backup-tasks.ps1")
        ) | Where-Object { Test-Path $_ } | Select-Object -First 1
        if ($rm) {
            & $rm
        } else {
            foreach ($t in $BACKUP_TASKS) {
                Unregister-ScheduledTask -TaskName $t -Confirm:$false -ErrorAction SilentlyContinue
                Write-Host "  $t supprimée."
            }
        }
    }
    Write-Host "`nDésinstallation terminée." -ForegroundColor Yellow
    exit 0
}

# ════════════════════════════════════════════════════════════════════════════════
# ACTION : refresh-config
# Relit config.json et met à jour les XMLs WinSW sans réinstallation
# ════════════════════════════════════════════════════════════════════════════════
if ($Action -eq "refresh-config") {
    if ($Target -in "all","app") {
        $xmlPath = Join-Path $AppDir "pharmasmart-app.xml"
        if (-not (Test-Path $xmlPath)) {
            Write-Warning "XML app introuvable ($xmlPath) — utiliser 'install -Target app' d'abord."
        } else {
            [xml]$xmlDoc = Get-Content $xmlPath -Encoding UTF8
            $JavaExeInXml = $xmlDoc.service.executable
            $JarPath = ($xmlDoc.service.arguments -split ' ' | Where-Object { $_ -like '*.jar' })[0].Trim('"')
            if (-not (Test-Path $JarPath)) { Write-Warning "JAR introuvable : $JarPath" }

            # Utilise java_home depuis config si disponible, sinon conserve celui du XML
            $effectiveJava = if ($JavaExe -ne "java") { $JavaExe } else { $JavaExeInXml }

            $DbPassword = try { $cfg.database.password } catch { "" }
            if (-not $DbPassword) { $DbPassword = Get-DbPassword $DbUser }

            $envDbEntries = "  <env name=`"PHARMA_DB_URL`"    value=`"$DbUrl`"/>`n" +
                            "  <env name=`"PHARMA_DB_USER`"   value=`"$DbUser`"/>`n" +
                            "  <env name=`"PHARMA_DB_SCHEMA`" value=`"$DbSchema`"/>`n"
            if ($DbPassword) { $envDbEntries += "  <env name=`"PHARMA_DB_PASSWORD`" value=`"$DbPassword`"/>`n" }

            $logsDir = Join-Path (Split-Path -Parent $AppDir) "logs"
            Set-Content -Path $xmlPath -Encoding UTF8 -Value @"
<service>
  <id>$APP_SVC_NAME</id>
  <name>PharmaSmart Application</name>
  <description>PharmaSmart — Gestion pharmaceutique, API web (port $Port)</description>
  <executable>$effectiveJava</executable>
  <arguments>-Xms$AppHeapMin -Xmx$AppHeapMax -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 -Djava.awt.headless=true -jar "$JarPath" --server.port=$Port</arguments>
  <startmode>Automatic</startmode>
  <delayedAutoStart>true</delayedAutoStart>
  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>
  <onfailure action="none"/>
$envDbEntries
  <logmode>rotate</logmode>
  <logpath>$logsDir</logpath>
  <log mode="rotate"><sizeThreshold>10240</sizeThreshold><keepFiles>5</keepFiles></log>
  <stopTimeout>30 sec</stopTimeout>
  <depend>postgresql-x64-18</depend>
</service>
"@
            $DbPassword = $null; [System.GC]::Collect()
            Set-RestrictedAcl $xmlPath $false
            Write-Host "  App : XML mis à jour." -ForegroundColor Green
            if (-not $NoRestart) { Restart-Service $APP_SVC_NAME -Force -ErrorAction SilentlyContinue }
        }
    }
    if ($Target -in "all","batch") {
        Write-Host "  Batch : relancer 'install -Target batch' pour régénérer le XML batch." -ForegroundColor Yellow
    }
    exit 0
}

# ════════════════════════════════════════════════════════════════════════════════
# ACTION : install
# ════════════════════════════════════════════════════════════════════════════════
# Mot de passe : depuis config.json si présent, sinon prompt sécurisé
$DbPassword = try { $cfg.database.password } catch { "" }
if (-not $DbPassword -and ($Target -in "all","app","batch")) {
    $DbPassword = Get-DbPassword $DbUser
}

Write-Host "`n=== Installation PharmaSmart ===" -ForegroundColor Cyan
Write-Host "  Java    : $JavaExe"
Write-Host "  App     : heap $AppHeapMin/$AppHeapMax | port $Port"
Write-Host "  Batch   : heap $BatchHeapMin/$BatchHeapMax"
Write-Host "  DB URL  : $DbUrl | Schema : $DbSchema"

if ($Target -in "all","app") {
    $jar = Find-Jar "pharmaSmart-app-*.jar" "pharmaSmart-app"
    if (-not $jar) { Write-Error "JAR app introuvable. Exécuter : mvnw.cmd clean package -DskipTests -Pprod" }
    Install-Svc `
        -Name        $APP_SVC_NAME `
        -Dir         $AppDir `
        -JarPath     $jar `
        -JavaExe     $JavaExe `
        -XmlTemplate (Join-Path $ScriptDir "pharmasmart-app.xml") `
        -HeapMin     $AppHeapMin `
        -HeapMax     $AppHeapMax `
        -DbUrl       $DbUrl `
        -DbUser      $DbUser `
        -DbPassword  $DbPassword `
        -DbSchema    $DbSchema `
        -Port        $Port
}

if ($Target -in "all","batch") {
    $jar = Find-Jar "pharmaSmart-batch-*.jar" "pharmaSmart-batch"
    if (-not $jar) { Write-Error "JAR batch introuvable. Exécuter : mvnw.cmd clean package -DskipTests -pl pharmaSmart-batch" }
    Install-Svc `
        -Name        $BATCH_SVC_NAME `
        -Dir         $BatchDir `
        -JarPath     $jar `
        -JavaExe     $JavaExe `
        -XmlTemplate (Join-Path $ScriptDir "pharmasmart-batch.xml") `
        -HeapMin     $BatchHeapMin `
        -HeapMax     $BatchHeapMax `
        -DbUrl       $DbUrl `
        -DbUser      $DbUser `
        -DbPassword  $DbPassword `
        -DbSchema    $DbSchema
}

$DbPassword = $null; [System.GC]::Collect()

if ($Target -in "all","backup") {
    Write-Host "`n=== Tâches planifiées backup ===" -ForegroundColor Cyan
    $setupScript = @(
        (Join-Path $RepoRoot "src-backup\scripts\setup-backup-tasks.ps1"),
        (Join-Path $ScriptDir "setup-backup-tasks.ps1")
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($setupScript) {
        $backupExe = @(
            (Join-Path $ScriptDir "..\pharmasmart-backup.exe"),
            (Join-Path $RepoRoot "src-backup\target\release\pharmasmart-backup.exe")
        ) | Where-Object { Test-Path $_ } | Select-Object -First 1
        if ($backupExe) { & $setupScript -ExePath $backupExe }
        else            { & $setupScript }
    } else {
        Write-Warning "setup-backup-tasks.ps1 introuvable — tâches backup non enregistrées."
    }
}

Write-Host "`n=== Installation terminée ===" -ForegroundColor Green
Write-Host @"

Services installés (démarrage automatique différé) :
  - $APP_SVC_NAME   → $AppDir  (port $Port)
  - $BATCH_SVC_NAME → $BatchDir

Démarrer :
  Start-Service $APP_SVC_NAME
  Start-Service $BATCH_SVC_NAME

État :
  .\pharmasmart-setup.ps1 status

Mettre à jour les heaps JVM :
  .\pharmasmart-setup.ps1 update-jvm   (relit config.json)
"@ -ForegroundColor Yellow
