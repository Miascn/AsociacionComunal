param(
    [string]$Version = "1.0.0",
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\dist"),
    [string]$PreviousManifest = ""
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
$portablePath = Join-Path $OutputDirectory "$applicationName-$Version-portable.exe"
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

$portableStaging = Join-Path $env:TEMP "$applicationName-portable-build"
if (Test-Path $portableStaging) { Remove-Item -LiteralPath $portableStaging -Recurse -Force }
New-Item -ItemType Directory -Path $portableStaging -Force | Out-Null
$payloadZip = Join-Path $portableStaging "payload.zip"
Compress-Archive -Path $applicationImage -DestinationPath $payloadZip -CompressionLevel Optimal
Copy-Item -LiteralPath (Join-Path $repositoryRoot "scripts\portable-bootstrap.ps1") -Destination $portableStaging

$sevenZip = Join-Path $repositoryRoot ".tools\7zip\extra\x64\7za.exe"
$sfxModule = Join-Path $repositoryRoot ".tools\7zip\sdk\bin\7zSD.sfx"
if (-not (Test-Path $sevenZip) -or -not (Test-Path $sfxModule)) {
    throw "Faltan las herramientas 7-Zip SFX en .tools\7zip."
}
$portableArchive = Join-Path $portableStaging "portable.7z"
$sfxConfig = Join-Path $portableStaging "sfx-config.txt"
& $sevenZip a -t7z $portableArchive (Join-Path $portableStaging "portable-bootstrap.ps1") $payloadZip -mx=9
if ($LASTEXITCODE -ne 0) { throw "7-Zip no pudo comprimir el ejecutable portatil." }
$configuration = @"
;!@Install@!UTF-8!
Title="Asociacion Comunal QA"
BeginPrompt="Abriendo Asociacion Comunal QA..."
RunProgram="powershell.exe -NoProfile -ExecutionPolicy Bypass -File portable-bootstrap.ps1"
GUIMode="2"
;!@InstallEnd@!
"@
[IO.File]::WriteAllText($sfxConfig, $configuration, [Text.UTF8Encoding]::new($false))

if (Test-Path $portablePath) { Remove-Item -LiteralPath $portablePath -Force }
$output = [IO.File]::Open($portablePath, [IO.FileMode]::CreateNew)
try {
    foreach ($part in @($sfxModule, $sfxConfig, $portableArchive)) {
        $input = [IO.File]::OpenRead($part)
        try { $input.CopyTo($output) } finally { $input.Dispose() }
    }
} finally { $output.Dispose() }

Write-Host "Ejecutable portatil: $portablePath"
Write-Host "Actualizacion incremental: $deltaPath"
Write-Host "Manifiesto: $manifestPath"
Write-Host "Ejecutable: $(Join-Path $applicationImage "$applicationName.exe")"
