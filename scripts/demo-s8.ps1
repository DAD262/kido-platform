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

Write-Host "=== S08 KIDO - FLUJO NORMAL CON KAFKA ===" -ForegroundColor Cyan
$admin = Get-KidoToken "admin" "Admin123!"
$headers = @{ Authorization = "Bearer $admin" }
$curso = Invoke-RestMethod -Method Get -Uri "$Base/api/v1/cursos/2"
$student = Get-Random -Minimum 20000 -Maximum 90000

Write-Host "1) Creando orden para estudiante $student..." -ForegroundColor Yellow
$orden = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes" -Headers $headers `
    -ContentType "application/json" -Body (@{ estudianteId=$student; cursoId=2 } | ConvertTo-Json)
$orden | ConvertTo-Json -Depth 6

Write-Host "`n2) Aprobando la orden; kido-pago-ms publicará pago.aprobado DESPUÉS del commit..." -ForegroundColor Yellow
$aprobada = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/pagos/ordenes/$($orden.id)/simular-aprobacion" -Headers $headers
$aprobada | ConvertTo-Json -Depth 6
Start-Sleep -Seconds 3

Write-Host "`n3) Notificaciones generadas por el consumidor Kafka..." -ForegroundColor Yellow
$notificaciones = Invoke-RestMethod -Method Get -Uri "$Base/api/v1/notificaciones/usuario/$student" -Headers $headers
$notificaciones | ConvertTo-Json -Depth 8

Write-Host "`n4) Evidencia rápida de logs:" -ForegroundColor Yellow
docker compose logs --tail=80 kido-pago-ms | Select-String "component=producer|pago.aprobado"
docker compose logs --tail=80 kido-notificacion-ms | Select-String "component=consumer|component=processor|pago.aprobado"

Write-Host "`n5) Consumer group / lag:" -ForegroundColor Yellow
docker exec kido-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kido-kafka:9092 --group kido-notificacion-ms --describe

Write-Host "`nFlujo S08 terminado. Abre http://localhost:18085 para la captura de Kafka UI." -ForegroundColor Green
Write-Host "Orden usada: $($orden.id) | Estudiante: $student | Docente: $($curso.docenteId)" -ForegroundColor Green
