param(
    [string]$ApiUrl = "https://myesha-peroneal-unimpulsively.ngrok-free.dev"
)

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$bytes = New-Object byte[] 32
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($bytes)
} finally {
    $generator.Dispose()
}
$token = -join ($bytes | ForEach-Object { $_.ToString("x2") })

[System.IO.File]::WriteAllLines((Join-Path $repositoryRoot "qa-local.properties"), @(
    "api.url=$ApiUrl",
    "api.token=$token"
))
[System.IO.File]::WriteAllText((Join-Path $repositoryRoot "qa-token.upload"), $token)
$token = $null

Write-Host "Configuracion QA local creada sin mostrar el token."
