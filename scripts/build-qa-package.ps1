param(
    [string]$Version = "1.0.0",
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\dist")
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$frontendPom = Join-Path $repositoryRoot "frontend\pom.xml"
$backendPom = Join-Path $repositoryRoot "backend\pom.xml"
$frontendTarget = Join-Path $repositoryRoot "frontend\target"
$packageInput = Join-Path $frontendTarget "package-input"
$applicationJar = Join-Path $frontendTarget "AsociacionComunal-1.0-SNAPSHOT.jar"
$stagingDirectory = Join-Path $frontendTarget "jpackage-input"
$applicationName = "AsociacionComunalQA"
$applicationImage = Join-Path $OutputDirectory $applicationName
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
if (Test-Path $zipPath) { Remove-Item -LiteralPath $zipPath -Force }

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

Compress-Archive -Path $applicationImage -DestinationPath $zipPath -CompressionLevel Optimal

Write-Host "Paquete QA generado: $zipPath"
Write-Host "Ejecutable: $(Join-Path $applicationImage "$applicationName.exe")"
