param(
    [string]$Version = "1.0.0",
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\dist"),
    [string]$PreviousManifest = "",
    [string]$LegacyImage = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
$frontendPom = Join-Path $repositoryRoot "frontend\pom.xml"
$backendPom = Join-Path $repositoryRoot "backend\pom.xml"
$frontendTarget = Join-Path $repositoryRoot "frontend\target"
$packageInput = Join-Path $frontendTarget "package-input"
$applicationJar = Join-Path $frontendTarget "AsociacionComunal-1.0-SNAPSHOT.jar"
$stagingDirectory = Join-Path $frontendTarget "jpackage-input"
$applicationName = "AsociacionComunalQA"
$applicationImage = Join-Path $OutputDirectory $applicationName
$deltaPath = Join-Path $OutputDirectory "$applicationName-$Version-delta.zip"
$manifestPath = Join-Path $OutputDirectory "update-manifest.json"
$zipPath = Join-Path $OutputDirectory "$applicationName-$Version-win64.zip"
$mavenRepository = Join-Path $env:USERPROFILE ".m2\repository"
$localQaConfig = Join-Path $repositoryRoot "qa-local.properties"

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) {
    $localMaven = Join-Path $repositoryRoot "apache-maven-3.9.16\bin\mvn.cmd"
    if (Test-Path $localMaven) {
        $maven = Get-Item $localMaven
    } else {
        throw "No se encontro Maven. Instale Maven o conserve apache-maven-3.9.16 en la raiz local."
    }
}
$mavenPath = if ($maven.PSObject.Properties.Name -contains "Source") {
    $maven.Source
} else {
    $maven.FullName
}

$javaCommand = Get-Command java.exe -ErrorAction Stop
$jdkHome = Split-Path (Split-Path $javaCommand.Source)
$jpackage = Join-Path $jdkHome "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) {
    throw "El JDK activo no incluye jpackage: $jdkHome"
}

& $mavenPath "-Dmaven.repo.local=$mavenRepository" -f $backendPom clean install -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion del backend." }

& $mavenPath "-Dmaven.repo.local=$mavenRepository" -f $frontendPom clean package
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion o las pruebas del frontend." }

if (Test-Path $stagingDirectory) { Remove-Item -LiteralPath $stagingDirectory -Recurse -Force }
New-Item -ItemType Directory -Path $stagingDirectory | Out-Null
Copy-Item -LiteralPath $applicationJar -Destination $stagingDirectory
Copy-Item -Path (Join-Path $packageInput "lib") -Destination $stagingDirectory -Recurse

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
if (Test-Path $applicationImage) { Remove-Item -LiteralPath $applicationImage -Recurse -Force }
if (Test-Path $deltaPath) { Remove-Item -LiteralPath $deltaPath -Force }

& $jpackage `
    --type app-image `
    --name $applicationName `
    --app-version $Version `
    --description "Sistema para Administracion de Asociacion Comunal - QA" `
    --vendor "Asociacion Comunal Team" `
    --input $stagingDirectory `
    --main-jar (Split-Path $applicationJar -Leaf) `
    --main-class app.Launcher `
    --dest $OutputDirectory

if ($LASTEXITCODE -ne 0) { throw "jpackage no pudo generar la aplicacion de Windows." }

if ($LegacyImage) {
    $legacyRoot = (Resolve-Path $LegacyImage).Path
    $legacyExecutable = Join-Path $legacyRoot "$applicationName.exe"
    $legacyRuntime = Join-Path $legacyRoot "runtime"
    if (-not (Test-Path $legacyExecutable) -or -not (Test-Path $legacyRuntime)) {
        throw "La imagen base no contiene el ejecutable y runtime esperados: $legacyRoot"
    }
    Copy-Item -LiteralPath $legacyExecutable -Destination $applicationImage -Force
    Remove-Item -LiteralPath (Join-Path $applicationImage "runtime") -Recurse -Force
    Copy-Item -LiteralPath $legacyRuntime -Destination $applicationImage -Recurse
    Write-Host "Lanzador y runtime conservados desde: $legacyRoot"
}

$qaConfigDirectory = Join-Path $applicationImage "config"
$qaConfigDestination = Join-Path $qaConfigDirectory "qa.properties"
New-Item -ItemType Directory -Path $qaConfigDirectory -Force | Out-Null
if (Test-Path $localQaConfig) {
    Copy-Item -LiteralPath $localQaConfig -Destination $qaConfigDestination
} elseif ($env:QA_API_URL -and $env:QA_API_TOKEN) {
    [System.IO.File]::WriteAllLines($qaConfigDestination, @(
        "api.url=$($env:QA_API_URL)",
        "api.token=$($env:QA_API_TOKEN)"
    ))
} else {
    throw "Falta qa-local.properties o los secretos QA_API_URL y QA_API_TOKEN."
}

$deltaRoot = Join-Path $frontendTarget "delta"
if (Test-Path $deltaRoot) { Remove-Item -LiteralPath $deltaRoot -Recurse -Force }
New-Item -ItemType Directory -Path (Join-Path $deltaRoot "app") -Force | Out-Null

$previousHashes = @{}
if ($PreviousManifest -and (Test-Path $PreviousManifest)) {
    $previous = Get-Content -LiteralPath $PreviousManifest -Raw | ConvertFrom-Json
    foreach ($file in $previous.files) { $previousHashes[$file.path] = $file.sha256 }
}

$files = @()
Get-ChildItem -LiteralPath (Join-Path $applicationImage "app") -File -Recurse | ForEach-Object {
    $relative = $_.FullName.Substring($applicationImage.Length + 1).Replace('\', '/')
    $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $files += [ordered]@{ path = $relative; sha256 = $hash; size = $_.Length }
    if (-not $previousHashes.ContainsKey($relative) -or $previousHashes[$relative] -ne $hash) {
        $destination = Join-Path $deltaRoot $relative
        New-Item -ItemType Directory -Path (Split-Path $destination) -Force | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $destination
    }
}

Compress-Archive -Path (Join-Path $deltaRoot "app") -DestinationPath $deltaPath -CompressionLevel Optimal
$deltaHash = (Get-FileHash -LiteralPath $deltaPath -Algorithm SHA256).Hash.ToLowerInvariant()
$manifest = [ordered]@{
    version = $Version
    sha256 = $deltaHash
    size = (Get-Item -LiteralPath $deltaPath).Length
    files = $files
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding utf8

if (Test-Path $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
Compress-Archive -Path $applicationImage -DestinationPath $zipPath -CompressionLevel Optimal
$legacyHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $OutputDirectory "version.txt"), $Version, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $OutputDirectory "sha256.txt"), $legacyHash, [Text.UTF8Encoding]::new($false))

Write-Host "Paquete QA: $zipPath"
Write-Host "Actualizacion incremental: $deltaPath"
Write-Host "Manifiesto: $manifestPath"
Write-Host "Ejecutable: $(Join-Path $applicationImage "$applicationName.exe")"
