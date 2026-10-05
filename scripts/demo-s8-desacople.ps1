$ErrorActionPreference = "Stop"
$Base = "http://localhost:18080"
$Keycloak = "http://localhost:19090"

function Get-KidoToken([string]$User, [string]$Password) {
    $response = Invoke-RestMethod -Method Post `
        -Uri "$Keycloak/realms/kido/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{ client_id="kido-cli"; grant_type="password"; username=$User; password=$Password }
    return $response.access_token
}

Write-Host "=== S08 KIDO - PRUEBA DE DESACOPLE ===" -ForegroundColor Cyan
$admin = Get-KidoToken "admin" "Admin123!"
$headers = @{ Authorization = "Bearer $admin" }
$studentWarmup = Get-Random -Minimum 140001 -Maximum 180000
$student = Get-Random -Minimum 90001 -Maximum 140000

Write-Host "0) Preparando el consumer group con un evento de control..." -ForegroundColor Yellow
$warmup = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes" -Headers $headers `
    -ContentType "application/json" -Body (@{ estudianteId=$studentWarmup; cursoId=2 } | ConvertTo-Json)
Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($warmup.id)/simular-aprobacion" -Headers $headers | Out-Null
Start-Sleep -Seconds 4

Write-Host "1) Deteniendo SOLO kido-notificacion-ms..." -ForegroundColor Yellow
docker compose stop kido-notificacion-ms
Start-Sleep -Seconds 2

Write-Host "2) Creando y aprobando una orden con el consumidor APAGADO..." -ForegroundColor Yellow
$orden = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes" -Headers $headers `
    -ContentType "application/json" -Body (@{ estudianteId=$student; cursoId=2 } | ConvertTo-Json)
$aprobada = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($orden.id)/simular-aprobacion" -Headers $headers
$aprobada | ConvertTo-Json -Depth 6
Start-Sleep -Seconds 2

Write-Host "`nLa orden quedó APROBADA aunque notificaciones está apagado." -ForegroundColor Green
Write-Host "3) Evento publicado por kido-pago-ms:" -ForegroundColor Yellow
docker compose logs --tail=80 kido-pago-ms | Select-String "component=producer|pago.aprobado"

Write-Host "`n4) Lag del consumer group (debe existir mensaje pendiente):" -ForegroundColor Yellow
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-notificacion-ms --describe

Write-Host "`nAHORA toma captura de: orden APROBADA + Kafka UI/Consumers con lag pendiente." -ForegroundColor Magenta
Read-Host "Presiona ENTER cuando ya tomaste la captura y quieras encender el consumidor"

Write-Host "5) Iniciando kido-notificacion-ms..." -ForegroundColor Yellow
docker compose start kido-notificacion-ms
Start-Sleep -Seconds 8

Write-Host "`n6) Log del consumidor procesando el evento pendiente:" -ForegroundColor Yellow
docker compose logs --tail=120 kido-notificacion-ms | Select-String "component=consumer|component=processor|pago.aprobado"

Write-Host "`n7) Lag final (debe volver a 0):" -ForegroundColor Yellow
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-notificacion-ms --describe

Write-Host "`nPrueba de desacople completada. Orden: $($orden.id), estudiante: $student" -ForegroundColor Green
