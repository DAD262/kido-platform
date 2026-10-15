param(
    [long]$CursoId = 2,
    [string]$AdminUsuario = "admin",
    [string]$AdminPassword = $env:KIDO_DEMO_ADMIN_PASSWORD,
    [int]$TimeoutSegundos = 50,
    [switch]$PausarCapturas
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
function Captura([string]$Nombre) {
    Write-Host "`nCAPTURA: $Nombre | $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | $env:USERNAME" -ForegroundColor Cyan
    Write-Host "Mantén visible el reloj de Windows y tu perfil. No recortes la pantalla."
    if ($PausarCapturas) {
        [void](Read-Host "Toma la captura y pulsa Enter para continuar")
        # Pauses may outlast the JWT lifetime. Authenticate again without displaying the token.
        Renovar-Sesion
    }
}
function Consulta-Sql([string]$Servicio, [string]$BaseDatos, [string]$Sql) {
    # Uses the database container's own user setting; no password is printed or stored.
    $resultado = @($Sql | docker compose exec -T $Servicio sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At')
    if ($LASTEXITCODE -ne 0) { throw "Falló la comprobación PostgreSQL en $Servicio ($BaseDatos)" }
    return ($resultado -join "`n").Trim()
}
function Venta-Orden([long]$Id) {
    return Consulta-Sql "postgres-pago" "kido_pago_db" "SELECT id || '|' || estado || '|' || monto FROM movimientos_saldo WHERE orden_id=$Id AND tipo='VENTA' ORDER BY id;"
}
function Verificar-Inscripcion([long]$OrdenId, [int]$Cantidad) {
    $n = Consulta-Sql "postgres-inscripcion" "kido_inscripcion_db" "SELECT count(*) FROM inscripciones WHERE orden_compra_id=$OrdenId;"
    if ($n -ne [string]$Cantidad) { throw "Orden $OrdenId : se esperaban $Cantidad inscripciones, se obtuvo $n" }
    Write-Host "PostgreSQL: orden=$OrdenId | inscripciones=$n" -ForegroundColor Green
}
function Log-Nuevo([string]$Servicio, [string]$Desde, [string]$Patron) {
    for ($i=0; $i -lt $TimeoutSegundos; $i++) {
        $lineas = @(docker compose logs --no-color --since $Desde $Servicio)
        if ($LASTEXITCODE -ne 0) { throw "No se pudieron consultar logs de $Servicio" }
        $coincidencias = @($lineas | Select-String $Patron)
        if ($coincidencias.Count -gt 0) { $coincidencias | ForEach-Object { Write-Host $_ }; return }
        Start-Sleep -Seconds 1
    }
    throw "No llegó el log esperado de $Servicio : $Patron"
}
function Renovar-Sesion {
    $sesion = Invoke-RestMethod -Method Post -Uri "$Keycloak/realms/kido/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{client_id="kido-cli";grant_type="password";username=$AdminUsuario;password=$AdminPassword}
    $script:Headers = @{ Authorization="Bearer $($sesion.access_token)" }
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
Renovar-Sesion

$Estudiante = Get-Random -Minimum 200000 -Maximum 899999
$InicioPrueba = (Get-Date).ToUniversalTime().ToString("o")
Write-Host "Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | Usuario Windows: $env:USERNAME"
docker compose ps
if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar Docker Compose" }
$Indices = Consulta-Sql "postgres-inscripcion" "kido_inscripcion_db" "SELECT indexname || ': ' || indexdef FROM pg_indexes WHERE tablename='inscripciones' AND indexdef LIKE '%UNIQUE%';"
Write-Host $Indices
if ($Indices -notmatch 'uk_inscripciones_orden_compra') { throw "Falta el índice UNIQUE de orden_compra_id" }
Captura "01 - Servicios y restricciones UNIQUE reales"
Write-Host "[1/5] Aprobación normal: estudiante=$Estudiante, curso=$CursoId" -ForegroundColor Yellow
$Normal = Nueva-Orden $Estudiante
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Normal.id)/simular-aprobacion" -Headers $Headers | Out-Null
$EstadoNormal = Esperar-Orden $Normal.id {param($x) $x.estado -eq "APROBADA" -and $x.inscripcionSincronizada} $TimeoutSegundos
$Inscripciones = @(Invoke-RestMethod -Method Get -Uri "$Base/api/v1/inscripciones/estudiante/$Estudiante" -Headers $Headers)
$Cantidad = @($Inscripciones | Where-Object { $_.cursoId -eq $CursoId }).Count
if ($Cantidad -ne 1) { throw "Se esperaba UNA inscripción para estudiante=$Estudiante, curso=$CursoId. Obtenido=$Cantidad" }
Write-Host "OK: orden APROBADA, inscripción sincronizada y UNA inscripción" -ForegroundColor Green

Verificar-Inscripcion $Normal.id 1
$VentaNormal = Venta-Orden $Normal.id
if ($VentaNormal -notmatch '^\d+\|PENDIENTE\|') { throw "La orden normal no tiene una única VENTA PENDIENTE: $VentaNormal" }
$EstadoNormal | Format-List
Write-Host "Movimiento de venta: $VentaNormal"
Captura "02 - Compra normal APROBADA e inscripción sincronizada"
Write-Host "[2/5] Republícar evento normal, no debe duplicar la inscripción" -ForegroundColor Yellow
$DesdeNormal = (Get-Date).ToUniversalTime().ToString("o")
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Normal.id)/republicar-evento" -Headers $Headers | Out-Null
Log-Nuevo "kido-inscripcion-ms" $DesdeNormal "ordenId=$($Normal.id)\s+status=ignored"
$Inscripciones2 = @(Invoke-RestMethod -Method Get -Uri "$Base/api/v1/inscripciones/estudiante/$Estudiante" -Headers $Headers)
$Cantidad2 = @($Inscripciones2 | Where-Object { $_.cursoId -eq $CursoId }).Count
if ($Cantidad2 -ne 1) { throw "FALLÓ IDEMPOTENCIA: inscripciones=$Cantidad2" }
$LogNormal = docker compose logs --tail=250 kido-inscripcion-ms
if (-not ($LogNormal | Select-String "ordenId=$($Normal.id).*status=ignored")) {
    throw "No se encontró el log del mensaje duplicado ignorado. Revisa docker compose logs kido-inscripcion-ms"
}
Write-Host "OK: duplicado ignorado; no se creó otra inscripción" -ForegroundColor Green

Verificar-Inscripcion $Normal.id 1
if ((Venta-Orden $Normal.id) -ne $VentaNormal) { throw "La repetición alteró el movimiento de venta normal" }
Captura "03 - Duplicado ignorado, una inscripción y mismo movimiento"
Write-Host "[3/5] Fallo de negocio S09 -> compensación" -ForegroundColor Yellow
$Fallida = Nueva-Orden 999999
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Fallida.id)/simular-aprobacion" -Headers $Headers | Out-Null
$EstadoFallido = Esperar-Orden $Fallida.id {param($x) $x.estado -eq "REEMBOLSADA"} $TimeoutSegundos
if ($EstadoFallido.inscripcionSincronizada) { throw "Compensación inválida: inscripcionSincronizada=true" }
Write-Host "OK: compra aprobada y luego REEMBOLSADA por fallo de inscripción" -ForegroundColor Green

Verificar-Inscripcion $Fallida.id 0
$VentaCompensada = Venta-Orden $Fallida.id
if ($VentaCompensada -notmatch '^\d+\|REVERTIDO\|') { throw "La compensación no revirtió una única venta: $VentaCompensada" }
Log-Nuevo "kido-inscripcion-ms" $InicioPrueba "eventType=inscripcion.fallida ordenId=$($Fallida.id) status=published"
Log-Nuevo "kido-pago-ms" $InicioPrueba "eventType=inscripcion.fallida ordenId=$($Fallida.id) status=compensated"
$EstadoFallido | Format-List
Write-Host "Movimiento de venta: $VentaCompensada"
Captura "04 - Fallo publicado, consumido y saldo REVERTIDO"
Write-Host "[4/5] Repetir el MISMO evento histórico: compensación idempotente" -ForegroundColor Yellow
$DesdeCompensacion = (Get-Date).ToUniversalTime().ToString("o")
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($Fallida.id)/republicar-evento" -Headers $Headers | Out-Null
Log-Nuevo "kido-pago-ms" $DesdeCompensacion "eventType=inscripcion.fallida ordenId=$($Fallida.id) status=ignored"
$Verificacion = Invoke-RestMethod -Method Get -Uri "$Base/api/v1/pagos/ordenes/$($Fallida.id)" -Headers $Headers
if ($Verificacion.estado -ne "REEMBOLSADA") { throw "FALLÓ IDEMPOTENCIA DE COMPENSACIÓN: estado=$($Verificacion.estado)" }
$LogCompensacion = docker compose logs --tail=300 kido-pago-ms
if (-not ($LogCompensacion | Select-String "ordenId=$($Fallida.id).*status=ignored")) {
    throw "No se encontró el log de compensación duplicada ignorada"
}
Write-Host "OK: compensación repetida ignorada" -ForegroundColor Green

Verificar-Inscripcion $Fallida.id 0
if ((Venta-Orden $Fallida.id) -ne $VentaCompensada) { throw "La repetición alteró la venta compensada" }
Write-Host "Movimiento antes y después: $VentaCompensada"
Captura "05 - Compensación duplicada ignorada, mismo movimiento REVERTIDO"
Write-Host "[5/5] Consumer groups de Kafka" -ForegroundColor Yellow
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-inscripcion-saga-ms --describe
if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar el grupo de inscripción" }
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-pago-saga-ms --describe
if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar el grupo de pago" }
Captura "06 - Consumer groups (revisar lag en todas las particiones)"
Write-Host "`n=== PRUEBA S09 FINALIZADA CON ÉXITO ===" -ForegroundColor Green
Write-Host "Orden normal: $($Normal.id) | Orden compensada: $($Fallida.id)"
Write-Host "Captura Kafka UI + logs (con usuario, fecha y hora visibles)."
Write-Host "Esta prueba repite pago.aprobado y genera otra solicitud de compensación del mismo negocio."
Write-Host "Para repetir además el MISMO JSON inscripcion.fallida: sigue docs/s09-guia-capturas.md."


$DirectorioEvidencia = Join-Path $Repo "docs/evidencias-s09"
New-Item -ItemType Directory -Path $DirectorioEvidencia -Force | Out-Null
$Resumen = [ordered]@{
    inicioUtc=$InicioPrueba; finUtc=(Get-Date).ToUniversalTime().ToString("o")
    usuarioWindows=$env:USERNAME; cursoId=$CursoId; estudianteNormal=$Estudiante
    ordenNormal=$Normal.id; ordenCompensada=$Fallida.id
    ventaNormal=$VentaNormal; ventaCompensada=$VentaCompensada
    pruebas="Compra normal, duplicado de pago, fallo de inscripción y compensación repetida"
    pendiente="Capturar Kafka UI y repetir directamente el JSON inscripcion.fallida desde Kafka UI"
}
$Resumen | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $DirectorioEvidencia "resumen-$(Get-Date -Format 'yyyyMMdd-HHmmss').json")
Write-Host "Resumen sin tokens guardado en docs/evidencias-s09."
