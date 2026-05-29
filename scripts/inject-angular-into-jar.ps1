<#
  Injects Angular static files into a Spring Boot fat JAR.
  Spring Boot fat JAR structure: BOOT-INF/classes/static/ holds static resources.
  The JAR is a ZIP file - we open it in Update mode and add/replace entries.
#>
param(
    [Parameter(Mandatory=$true)][string]$JarPath,
    [Parameter(Mandatory=$true)][string]$StaticDir
)

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$jar  = [System.IO.Path]::GetFullPath($JarPath)
$root = [System.IO.Path]::GetFullPath($StaticDir)

if (-not (Test-Path $jar))  { Write-Error "JAR not found: $jar";       exit 1 }
if (-not (Test-Path $root)) { Write-Error "Static dir not found: $root"; exit 1 }

$files = Get-ChildItem $root -Recurse -File
if ($files.Count -eq 0) {
    Write-Host "inject-angular: no files in $root - nothing to inject."
    exit 0
}

$zip = [System.IO.Compression.ZipFile]::Open($jar, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $count = 0
    foreach ($f in $files) {
        $rel   = $f.FullName.Substring($root.Length).TrimStart([char]'\', [char]'/').Replace('\', '/')
        $entry = "BOOT-INF/classes/static/$rel"

        $existing = $zip.Entries | Where-Object { $_.FullName -eq $entry } | Select-Object -First 1
        if ($null -ne $existing) { $existing.Delete() }

        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, $f.FullName, $entry,
            [System.IO.Compression.CompressionLevel]::Fastest) | Out-Null
        $count++
    }
    $leaf = Split-Path $jar -Leaf
    Write-Host "inject-angular: $count files injected into $leaf"
} catch {
    $errMsg = $_.ToString()
    Write-Error "inject-angular: injection failed - $errMsg"
    exit 1
} finally {
    $zip.Dispose()
}
