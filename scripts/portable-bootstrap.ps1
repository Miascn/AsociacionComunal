$ErrorActionPreference = 'Stop'
$installDir = Join-Path $env:LOCALAPPDATA 'MiasCN\AsociacionComunalQA'
$executable = Join-Path $installDir 'AsociacionComunalQA.exe'

if (-not (Test-Path -LiteralPath $executable)) {
    $parent = Split-Path $installDir
    $staging = Join-Path $parent ('AsociacionComunalQA-' + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
    try {
        Expand-Archive -LiteralPath (Join-Path $PSScriptRoot 'payload.zip') -DestinationPath $staging -Force
        $payload = Join-Path $staging 'AsociacionComunalQA'
        if (-not (Test-Path -LiteralPath (Join-Path $payload 'AsociacionComunalQA.exe'))) {
            throw 'El paquete portatil no contiene una aplicacion valida.'
        }
        Move-Item -LiteralPath $payload -Destination $installDir
    } finally {
        Remove-Item -LiteralPath $staging -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if ($env:QA_PORTABLE_VERIFY -eq '1') {
    if (-not (Test-Path -LiteralPath $executable)) { throw 'La aplicacion portatil no se preparo correctamente.' }
    exit 0
}

Start-Process -FilePath $executable
