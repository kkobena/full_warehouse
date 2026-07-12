param(
    [Parameter(Mandatory=$true)]
    [string]$ConfigFile
)

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# ── Defaults générés au moment du build depuis application-prod.yml ──────────
# db-defaults.json est produit par pharmaSmart-app/src/build/generate-db-defaults.ps1
# lors de chaque build Maven. Les valeurs reflètent exactement les defaults Spring Boot.
$d = @{ host="localhost"; port="5432"; dbName="pharma_smart"; user="pharma_smart"; password=""; schema="pharma_smart"; serverPort="9080" }
$defaultsFile = Join-Path $PSScriptRoot "db-defaults.json"
if (Test-Path $defaultsFile) {
    $j = Get-Content $defaultsFile -Raw | ConvertFrom-Json
    foreach ($k in @("host","port","dbName","user","schema")) {
        if ($j.PSObject.Properties[$k] -and $j.$k) { $d[$k] = $j.$k }
    }
    # password peut être vide — on lit explicitement
    if ($j.PSObject.Properties["password"]) { $d["password"] = $j.password }
}

# ── Si config.json a déjà des valeurs (réinstallation), les proposer ─────────
$config = Get-Content $ConfigFile -Raw | ConvertFrom-Json
$existingUrl = if ($config.PSObject.Properties["database"] -and $config.database.url) { $config.database.url } else { "" }
if ($existingUrl -match 'jdbc:postgresql://([^:/]+):(\d+)/([^\s?]+)') {
    $d["host"] = $Matches[1]; $d["port"] = $Matches[2]; $d["dbName"] = $Matches[3]
}
if ($config.PSObject.Properties["database"]) {
    if ($config.database.username) { $d["user"]   = $config.database.username }
    if ($config.database.schema)   { $d["schema"] = $config.database.schema }
}
if ($config.PSObject.Properties["server"] -and $config.server.PSObject.Properties["port"]) {
    $sp = [string]$config.server.port
    if ($sp -and $sp -ne "0") { $d["serverPort"] = $sp }
}

# ── Formulaire ────────────────────────────────────────────────────────────────
$form = New-Object System.Windows.Forms.Form
$form.Text            = "PharmaSmart — Configuration"
$form.StartPosition   = [System.Windows.Forms.FormStartPosition]::CenterScreen
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox     = $false
$form.MinimizeBox     = $false
$form.TopMost         = $true

$y = 18; $lblW = 150; $txtW = 210; $xTxt = $lblW + 25

function New-Row {
    param([string]$Label, [string]$Value, [bool]$Masked = $false)
    $lbl = New-Object System.Windows.Forms.Label
    $lbl.Text = $Label; $lbl.Location = New-Object System.Drawing.Point(15, ($script:y+3))
    $lbl.Size = New-Object System.Drawing.Size($lblW, 20)
    $lbl.TextAlign = [System.Drawing.ContentAlignment]::MiddleRight
    $form.Controls.Add($lbl)
    $txt = New-Object System.Windows.Forms.TextBox
    $txt.Text = $Value; $txt.Location = New-Object System.Drawing.Point($xTxt, $script:y)
    $txt.Size = New-Object System.Drawing.Size($txtW, 22)
    if ($Masked) { $txt.UseSystemPasswordChar = $true }
    $form.Controls.Add($txt)
    $script:y += 32; return $txt
}

$txtHost     = New-Row "Hôte PostgreSQL :" $d["host"]
$txtPort     = New-Row "Port PostgreSQL :" $d["port"]
$txtDbName   = New-Row "Base de données :" $d["dbName"]
$txtUser     = New-Row "Utilisateur :"     $d["user"]
$txtPassword = New-Row "Mot de passe :"    $d["password"] $true
$txtSchema   = New-Row "Schéma :"          $d["schema"]

# Séparateur visuel avant le port serveur
$y += 6
$sepSrv = New-Object System.Windows.Forms.Label
$sepSrv.BorderStyle = [System.Windows.Forms.BorderStyle]::Fixed3D
$sepSrv.Location = New-Object System.Drawing.Point(15, $y)
$sepSrv.Size = New-Object System.Drawing.Size(($lblW + $txtW + 20), 2)
$form.Controls.Add($sepSrv)
$y += 10

$txtServerPort = New-Row "Port serveur (app) :" $d["serverPort"]

$y += 4
$note = New-Object System.Windows.Forms.Label
$note.Text = "Schéma et mot de passe : optionnels. Port serveur : port d'écoute HTTP (défaut 9080)."
$note.Location = New-Object System.Drawing.Point($xTxt, $y)
$note.Size = New-Object System.Drawing.Size($txtW, 18)
$note.ForeColor = [System.Drawing.Color]::Gray
$form.Controls.Add($note)
$y += 26

$sep = New-Object System.Windows.Forms.Label
$sep.BorderStyle = [System.Windows.Forms.BorderStyle]::Fixed3D
$sep.Location = New-Object System.Drawing.Point(15, $y)
$sep.Size = New-Object System.Drawing.Size(($lblW + $txtW + 20), 2)
$form.Controls.Add($sep)
$y += 14

$btnOK = New-Object System.Windows.Forms.Button
$btnOK.Text = "Valider"; $btnOK.Location = New-Object System.Drawing.Point(215, $y)
$btnOK.Size = New-Object System.Drawing.Size(85, 28)
$btnOK.DialogResult = [System.Windows.Forms.DialogResult]::OK
$form.Controls.Add($btnOK); $form.AcceptButton = $btnOK

$btnSkip = New-Object System.Windows.Forms.Button
$btnSkip.Text = "Passer"; $btnSkip.Location = New-Object System.Drawing.Point(310, $y)
$btnSkip.Size = New-Object System.Drawing.Size(85, 28)
$btnSkip.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
$form.Controls.Add($btnSkip); $form.CancelButton = $btnSkip

$y += 46
$form.ClientSize = New-Object System.Drawing.Size(($lblW + $txtW + 40), $y)

# ── Validation : tous les champs sauf schéma sont obligatoires ────────────────
$btnOK.Add_Click({
    $portSrvVal = $txtServerPort.Text.Trim()
    $invalid = ($txtHost.Text   -match '^\s*$') -or
               ($txtPort.Text   -match '^\s*$') -or
               ($txtDbName.Text -match '^\s*$') -or
               ($txtUser.Text   -match '^\s*$') -or
               ($portSrvVal -notmatch '^\d+$')
    if ($invalid) {
        [System.Windows.Forms.MessageBox]::Show(
            "Hôte, port PostgreSQL, base de données, utilisateur et port serveur sont obligatoires.`nLe port serveur doit être un nombre (ex : 9080).`nSchéma et mot de passe sont optionnels.",
            "Champs manquants ou invalides",
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Warning) | Out-Null
        $form.DialogResult = [System.Windows.Forms.DialogResult]::None
    }
})

$result = $form.ShowDialog()

# ── Écriture — uniquement si l'utilisateur a validé avec des valeurs non vides ─
if ($result -eq [System.Windows.Forms.DialogResult]::OK) {
    $h      = $txtHost.Text.Trim()
    $p      = $txtPort.Text.Trim()
    $name   = $txtDbName.Text.Trim()
    $user   = $txtUser.Text.Trim()
    $pass   = $txtPassword.Text        # pas de trim
    $schema = if ($txtSchema.Text.Trim()) { $txtSchema.Text.Trim() } else { $name }

    $portSrv = [int]$txtServerPort.Text.Trim()

    # Vérification finale (ne devrait pas arriver — la validation btnOK l'a déjà fait)
    if ($h -and $p -and $name -and $user) {
        $jdbcUrl = "jdbc:postgresql://${h}:${p}/${name}"

        if (-not $config.PSObject.Properties["server"]) {
            $config | Add-Member -NotePropertyName "server" -NotePropertyValue ([PSCustomObject]@{ port = 9080 })
        }
        $config.server.port = $portSrv

        if (-not $config.PSObject.Properties["database"]) {
            $config | Add-Member -NotePropertyName "database" -NotePropertyValue (
                [PSCustomObject]@{ url=""; username=""; password=""; schema="" }
            )
        }
        $config.database.url      = $jdbcUrl
        $config.database.username = $user
        $config.database.password = $pass
        $config.database.schema   = $schema

        # Marquer la configuration comme complète pour que Tauri démarre
        # le backend directement au prochain lancement sans afficher le wizard.
        if (-not $config.PSObject.Properties["setup_complete"]) {
            $config | Add-Member -NotePropertyName "setup_complete" -NotePropertyValue $true
        } else {
            $config.setup_complete = $true
        }

        # Synchroniser backup.host / port / db / user
        if ($config.PSObject.Properties["backup"]) {
            $config.backup.host = $h
            $config.backup.port = [int]$p
            $config.backup.db   = $name
            $config.backup.user = $user
        }

        $json      = $config | ConvertTo-Json -Depth 10
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)

        # Écriture principale (sans BOM — serde_json rejette le BOM UTF-8)
        [System.IO.File]::WriteAllText($ConfigFile, $json, $utf8NoBom)
        Write-Host "config.json mis à jour : $ConfigFile"

        # ── Miroir vers toutes les copies lues par config_search_dirs() ──────
        # L'app Rust lit dans cet ordre : exe-dir → PROGRAMDATA → APPDATA.
        # Si $ConfigFile n'est pas dans l'exe-dir, la copie exe-dir garde l'ancienne
        # valeur (priorité 1) et le changement n'est pas visible.
        $mirrorCandidates = @()

        # Exe-dir : script déployé à $INSTDIR\resources\installer-hooks\ → remonter 2 niveaux
        $exeDir = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
        if ($exeDir) { $mirrorCandidates += Join-Path $exeDir "config.json" }

        # PROGRAMDATA et APPDATA
        if ($env:PROGRAMDATA) { $mirrorCandidates += Join-Path $env:PROGRAMDATA "PharmaSmart\config.json" }
        if ($env:APPDATA)     { $mirrorCandidates += Join-Path $env:APPDATA     "PharmaSmart\config.json" }

        foreach ($mirror in $mirrorCandidates) {
            if ($mirror -eq $ConfigFile) { continue }          # déjà écrit
            $mirrorDir = Split-Path $mirror
            if (-not (Test-Path $mirrorDir)) { continue }      # répertoire absent
            try {
                [System.IO.File]::WriteAllText($mirror, $json, $utf8NoBom)
                Write-Host "config.json mirrored : $mirror"
            } catch {
                Write-Warning "Impossible de mettre à jour $mirror : $_"
            }
        }
    }
} else {
    Write-Host "Configuration ignorée — Spring Boot utilisera ses propres defaults (application-prod.yml)"
}
