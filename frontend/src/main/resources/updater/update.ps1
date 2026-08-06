param([string]$InstallDir, [string]$Archive, [long]$ProcessId)
$ErrorActionPreference = 'Stop'
Wait-Process -Id $ProcessId -ErrorAction SilentlyContinue
$staging = Join-Path ([IO.Path]::GetTempPath()) ('asociacion-update-' + [guid]::NewGuid())
$backup = Join-Path ([IO.Path]::GetTempPath()) ('asociacion-backup-' + [guid]::NewGuid())
try {
    Expand-Archive -LiteralPath $Archive -DestinationPath $staging -Force
    $payload = Join-Path $staging 'app'
    if (-not (Test-Path $payload)) { throw 'Paquete incremental invalido.' }
    New-Item -ItemType Directory -Path $backup -Force | Out-Null
    Get-ChildItem -LiteralPath $payload -File -Recurse | ForEach-Object {
        $relative = $_.FullName.Substring($payload.Length).TrimStart('\')
        $target = Join-Path $InstallDir $relative
        $saved = Join-Path $backup $relative
        if (Test-Path -LiteralPath $target) {
            New-Item -ItemType Directory -Path (Split-Path $saved) -Force | Out-Null
            Copy-Item -LiteralPath $target -Destination $saved -Force
        }
        New-Item -ItemType Directory -Path (Split-Path $target) -Force | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $target -Force
    }
    Start-Process -FilePath (Join-Path $InstallDir 'AsociacionComunalQA.exe')
} catch {
    Get-ChildItem -LiteralPath $backup -File -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
        $relative = $_.FullName.Substring($backup.Length).TrimStart('\')
        $target = Join-Path $InstallDir $relative
        New-Item -ItemType Directory -Path (Split-Path $target) -Force | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $target -Force
    }
    throw
} finally {
    Remove-Item -LiteralPath $backup -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $staging -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $Archive -Force -ErrorAction SilentlyContinue
}
