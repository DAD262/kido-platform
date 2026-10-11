param([switch]$SinDocker)
$ErrorActionPreference="Stop"
$Root=Split-Path -Parent $PSScriptRoot
Set-Location $Root
$Modulos=@("infra/kido-config","infra/kido-eureka","infra/kido-gateway",
           "services/kido-curso-ms","services/kido-inscripcion-ms",
           "services/kido-pago-ms","services/kido-notificacion-ms")
foreach($m in $Modulos){
    Write-Host "Verificando compilación y tests de $m" -ForegroundColor Cyan
    Push-Location (Join-Path $Root $m)
    try {
        & .\mvnw.cmd -B -ntp clean verify
        if($LASTEXITCODE -ne 0){throw "Maven falló en $m con código $LASTEXITCODE"}
    } finally { Pop-Location }
}
if(-not $SinDocker){
    Write-Host "Validando Docker Compose..." -ForegroundColor Cyan
    docker compose config --quiet
    if($LASTEXITCODE -ne 0){throw "Docker Compose inválido"}
    Write-Host "Levanta contenedores y luego ejecuta .\scripts\demo-s9-saga.ps1"
}
Write-Host "Compilación y pruebas verificadas" -ForegroundColor Green
