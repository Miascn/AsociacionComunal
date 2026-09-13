<#
.SYNOPSIS
    Inicia el servidor backend ApiServer y configura el reenvío de puertos para dispositivos móviles físicos.
#>

$ErrorActionPreference = "Stop"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "    Iniciando Servidor Backend - Asociacion Comunal" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Buscar adb.exe
$adbPath = $null
if (Get-Command "adb" -ErrorAction SilentlyContinue) {
    $adbPath = "adb"
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe") {
    $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
}

if ($adbPath) {
    Write-Host "`n[1/3] Verificando dispositivos móviles Android (USB)..." -ForegroundColor Yellow
    $devices = & $adbPath devices
    if ($devices -match "(?m)^([a-zA-Z0-9]+)\s+device$") {
        Write-Host " -> Dispositivo Android detectado. Configurando 'adb reverse tcp:8080 tcp:8080'..." -ForegroundColor Green
        & $adbPath reverse tcp:8080 tcp:8080
        Write-Host " -> EXITO: El celular puede conectarse directamente a: http://localhost:8080" -ForegroundColor Green
    } else {
        Write-Host " -> No se detectó celular conectado por USB. Si te conectas por Wi-Fi, usa la IP local de tu PC." -ForegroundColor Gray
    }
} else {
    Write-Host "`n[1/3] adb no encontrado en PATH ni en Android SDK." -ForegroundColor Gray
}

# 2. Obtener IP local
Write-Host "`n[2/3] Identificando dirección IP de la red local..." -ForegroundColor Yellow
$ipAddresses = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | 
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } | 
    Select-Object -ExpandProperty IPAddress

foreach ($ip in $ipAddresses) {
    Write-Host " -> Para conexión por Wi-Fi usa en la app: http://$($ip):8080" -ForegroundColor Cyan
}

# 3. Iniciar ApiServer
Write-Host "`n[3/3] Arrancando ApiServer en puerto 8080..." -ForegroundColor Yellow
$rootPath = Split-Path -Parent $PSScriptRoot
$apiPath = Join-Path $rootPath "api"
$mvnPath = Join-Path $rootPath "apache-maven-3.9.16\bin\mvn.cmd"

if (-not (Test-Path $mvnPath)) {
    $mvnPath = "mvn"
}

Set-Location $apiPath
& $mvnPath exec:java "-Dexec.mainClass=sv.asociacion.ApiServer"
