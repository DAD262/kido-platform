param(
    [long]$CursoId = 2,
    [string]$AdminUsuario = "admin",
    [string]$AdminPassword = $env:KIDO_DEMO_ADMIN_PASSWORD,
    [int]$TimeoutSegundos = 50
)
$ErrorActionPreference = "Stop"
$Repo = Split-Path -Parent $PSScriptRoot
Set-Location $Repo
$Base = "http://localhost:18080"
$Keycloak = "http://localhost:19090"

function Valor-Env([string]$Clave) {
    $archivo = Join-Path $Repo ".env"
    if (-not (Test-Path $archivo)) { throw "Falta .env (copia .env.example y configura credenciales locales)" }
    $linea = Get-Content $archivo | Where-Object { $_ -match "^\s*$Clave\s*=" } | Select-Object -Last 1
    if (-not $linea) { return "" }
    return (($linea -split "=",2)[1]).Trim().Trim('"').Trim("'")
}
function Esperar-Orden([long]$Id, [scriptblock]$Condicion, [int]$Segundos = 50) {
    for ($i=0; $i -lt $Segundos; $i++) {
        $orden = Invoke-RestMethod -Method Get -Uri "$Base/api/v1/pagos/ordenes/$Id" -Headers $Headers
        if (& $Condicion $orden) { return $orden }
        Start-Sleep -Seconds 1
    }
    throw "TIMEOUT: la orden $Id no llegó al estado requerido en $Segundos segundos"
}
function Nueva-Orden([long]$Estudiante) {
    return Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes" -Headers $Headers `
        -ContentType "application/json" -Body (@{ estudianteId=$Estudiante; cursoId=$CursoId } | ConvertTo-Json)
}

if ((Valor-Env "KIDO_PAGOS_PERMITIR_SIMULACION").ToLower() -ne "true") {
    throw "Activa KIDO_PAGOS_PERMITIR_SIMULACION=true SOLO en .env local y ejecuta docker compose up -d --build"
}
$EstudianteFallo = Valor-Env "KIDO_SAGA_ESTUDIANTE_FALLO_DEMO"
if ($EstudianteFallo -ne "999999") {
    throw "Para esta demo define KIDO_SAGA_ESTUDIANTE_FALLO_DEMO=999999 en .env y recrea kido-inscripcion-ms"
}
if (-not $AdminPassword) { $AdminPassword = "Admin123!" } # Only the imported TEST realm (never production).

Write-Host "=== KIDO S09 | Saga coreografiada, duplicados, compensación ===" -ForegroundColor Cyan
$login = Invoke-RestMethod -Method Post -Uri "$Keycloak/realms/kido/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{client_id="kido-cli";grant_type="password";username=$AdminUsuario;password=$AdminPassword}
$Headers = @{ Authorization="Bearer $($login.access_token)" }

$Estudiante = Get-Random -Minimum 200000 -Maximum 899999
Write-Host "[1/5] Aprobación normal: estudiante=$Estudiante, curso=$CursoId" -ForegroundColor Yellow
$Normal = Nueva-Orden $Estudiante
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Normal.id)/simular-aprobacion" -Headers $Headers | Out-Null
$EstadoNormal = Esperar-Orden $Normal.id {param($x) $x.estado -eq "APROBADA" -and $x.inscripcionSincronizada} $TimeoutSegundos
$Inscripciones = @(Invoke-RestMethod -Method Get -Uri "$Base/api/v1/inscripciones/estudiante/$Estudiante" -Headers $Headers)
$Cantidad = @($Inscripciones | Where-Object { $_.cursoId -eq $CursoId }).Count
if ($Cantidad -ne 1) { throw "Se esperaba UNA inscripción para estudiante=$Estudiante, curso=$CursoId. Obtenido=$Cantidad" }
Write-Host "OK: orden APROBADA, inscripción sincronizada y UNA inscripción" -ForegroundColor Green

Write-Host "[2/5] Republícar evento normal, no debe duplicar la inscripción" -ForegroundColor Yellow
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Normal.id)/republicar-evento" -Headers $Headers | Out-Null
Start-Sleep -Seconds 5
$Inscripciones2 = @(Invoke-RestMethod -Method Get -Uri "$Base/api/v1/inscripciones/estudiante/$Estudiante" -Headers $Headers)
$Cantidad2 = @($Inscripciones2 | Where-Object { $_.cursoId -eq $CursoId }).Count
if ($Cantidad2 -ne 1) { throw "FALLÓ IDEMPOTENCIA: inscripciones=$Cantidad2" }
$LogNormal = docker compose logs --tail=250 kido-inscripcion-ms
if (-not ($LogNormal | Select-String "ordenId=$($Normal.id).*status=ignored")) {
    throw "No se encontró el log del mensaje duplicado ignorado. Revisa docker compose logs kido-inscripcion-ms"
}
Write-Host "OK: duplicado ignorado; no se creó otra inscripción" -ForegroundColor Green

Write-Host "[3/5] Fallo de negocio S09 -> compensación" -ForegroundColor Yellow
$Fallida = Nueva-Orden 999999
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Fallida.id)/simular-aprobacion" -Headers $Headers | Out-Null
$EstadoFallido = Esperar-Orden $Fallida.id {param($x) $x.estado -eq "REEMBOLSADA"} $TimeoutSegundos
if ($EstadoFallido.inscripcionSincronizada) { throw "Compensación inválida: inscripcionSincronizada=true" }
Write-Host "OK: compra aprobada y luego REEMBOLSADA por fallo de inscripción" -ForegroundColor Green

Write-Host "[4/5] Repetir el MISMO evento histórico: compensación idempotente" -ForegroundColor Yellow
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Fallida.id)/republicar-evento" -Headers $Headers | Out-Null
Start-Sleep -Seconds 6
$Verificacion = Invoke-RestMethod -Method Get -Uri "$Base/api/v1/pagos/ordenes/$($Fallida.id)" -Headers $Headers
if ($Verificacion.estado -ne "REEMBOLSADA") { throw "FALLÓ IDEMPOTENCIA DE COMPENSACIÓN: estado=$($Verificacion.estado)" }
$LogCompensacion = docker compose logs --tail=300 kido-pago-ms
if (-not ($LogCompensacion | Select-String "ordenId=$($Fallida.id).*status=ignored")) {
    throw "No se encontró el log de compensación duplicada ignorada"
}
Write-Host "OK: compensación repetida ignorada" -ForegroundColor Green

Write-Host "[5/5] Consumer groups de Kafka" -ForegroundColor Yellow
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-inscripcion-saga-ms --describe
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-pago-saga-ms --describe
Write-Host "`n=== PRUEBA S09 FINALIZADA CON ÉXITO ===" -ForegroundColor Green
Write-Host "Orden normal: $($Normal.id) | Orden compensada: $($Fallida.id)"
Write-Host "Captura Kafka UI + logs (con usuario, fecha y hora visibles)."
