param(
    [Parameter(Mandatory=$true)][string]$ProdYml,
    [Parameter(Mandatory=$true)][string]$BaseYml,
    [Parameter(Mandatory=$true)][string]$OutFile,
    [string]$NshOutFile = ""
)

# Defaults de secours (au cas où le parsing YAML échouerait)
$h      = "localhost"
$port   = "5432"
$dbName = "pharma_smart"
$user   = "pharma_smart"
$pass   = ""
$schema = "pharma_smart"

$prod = Get-Content $ProdYml -Raw -ErrorAction SilentlyContinue
$base = Get-Content $BaseYml -Raw -ErrorAction SilentlyContinue

if ($prod) {
    # url: ${PHARMA_DB_URL:jdbc:postgresql://host:port/dbname}
    if ($prod -match 'url:\s+\$\{PHARMA_DB_URL:jdbc:postgresql://([^:/]+):(\d+)/([^}]+)\}') {
        $h      = $Matches[1].Trim()
        $port   = $Matches[2].Trim()
        $dbName = $Matches[3].Trim()
    }
    # username: ${PHARMA_DB_USER:value}
    if ($prod -match 'username:\s+\$\{PHARMA_DB_USER:([^}]+)\}') {
        $user = $Matches[1].Trim()
    }
    # password: ${PHARMA_DB_PASSWORD:value}  (peut être vide)
    if ($prod -match 'password:\s+\$\{PHARMA_DB_PASSWORD:([^}]*)\}') {
        $pass = $Matches[1]
    }
}
if ($base -and $base -match 'schemas:\s+\$\{PHARMA_DB_SCHEMA:([^}]+)\}') {
    $schema = $Matches[1].Trim()
}

# Créer le répertoire de sortie si nécessaire
$outDir = Split-Path $OutFile -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }

@"
{
  "host":     "$h",
  "port":     "$port",
  "dbName":   "$dbName",
  "user":     "$user",
  "password": "$pass",
  "schema":   "$schema"
}
"@ | Set-Content $OutFile -Encoding UTF8

Write-Host "[generate-db-defaults] $OutFile : host=$h port=$port db=$dbName user=$user schema=$schema"

# Générer db-defaults.nsh si demandé (ou en déduisant le chemin depuis OutFile)
if (-not $NshOutFile) {
    $NshOutFile = Join-Path (Split-Path $OutFile -Parent) "db-defaults.nsh"
}
@"
; Generated at build time from application-prod.yml — do not edit manually.
; To regenerate: run Maven build on pharmaSmart-app (Windows).
!define DB_DEFAULT_HOST   "$h"
!define DB_DEFAULT_PORT   "$port"
!define DB_DEFAULT_NAME   "$dbName"
!define DB_DEFAULT_USER   "$user"
!define DB_DEFAULT_SCHEMA "$schema"
"@ | Set-Content $NshOutFile -Encoding UTF8
Write-Host "[generate-db-defaults] $NshOutFile written (password excluded from .nsh for security)"
