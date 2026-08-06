param([string]$InstallDir, [string]$Archive, [long]$ProcessId)
$ErrorActionPreference = 'Stop'
Wait-Process -Id $ProcessId -ErrorAction SilentlyContinue
$parent = Split-Path $InstallDir
$staging = Join-Path $parent ('.asociacion-update-' + [guid]::NewGuid())
$backup = $InstallDir + '.backup'
Expand-Archive -LiteralPath $Archive -DestinationPath $staging -Force
$newApp = Join-Path $staging 'AsociacionComunalQA'
if (-not (Test-Path (Join-Path $newApp 'AsociacionComunalQA.exe'))) { throw 'Paquete inválido.' }
if (Test-Path $backup) { Remove-Item -LiteralPath $backup -Recurse -Force }
Move-Item -LiteralPath $InstallDir -Destination $backup
Move-Item -LiteralPath $newApp -Destination $InstallDir
Start-Process -FilePath (Join-Path $InstallDir 'AsociacionComunalQA.exe')
Remove-Item -LiteralPath $backup -Recurse -Force
Remove-Item -LiteralPath $staging -Recurse -Force
Remove-Item -LiteralPath $Archive -Force
