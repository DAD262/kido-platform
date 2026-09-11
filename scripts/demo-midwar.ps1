$ErrorActionPreference = "Stop"
$base = "http://localhost:18080"

Write-Host "1. Verificando curso pagado..." -ForegroundColor Cyan
$curso = Invoke-RestMethod "$base/api/v1/cursos/2"
$curso | ConvertTo-Json -Depth 6

$student = Get-Random -Minimum 1000 -Maximum 9999
Write-Host "`n2. Creando orden para estudiante $student..." -ForegroundColor Cyan
$orden = Invoke-RestMethod -Method Post -Uri "$base/api/v1/pagos/ordenes" -ContentType "application/json" -Body (@{
  estudianteId = $student
  cursoId = 2
} | ConvertTo-Json)
$orden | ConvertTo-Json -Depth 6

Write-Host "`n3. Aprobando pago en modo demo local..." -ForegroundColor Cyan
$aprobada = Invoke-RestMethod -Method Post -Uri "$base/api/v1/pagos/ordenes/$($orden.id)/simular-aprobacion"
$aprobada | ConvertTo-Json -Depth 6

Write-Host "`n4. Verificando inscripción COMPRA..." -ForegroundColor Cyan
$inscripciones = Invoke-RestMethod "$base/api/v1/inscripciones"
$inscripciones | Where-Object { $_.estudianteId -eq $student -and $_.cursoId -eq 2 } | ConvertTo-Json -Depth 8

Write-Host "`n5. Saldo del docente (debe estar PENDIENTE 7 días)..." -ForegroundColor Cyan
Invoke-RestMethod "$base/api/v1/pagos/docentes/$($curso.docenteId)/saldo" | ConvertTo-Json -Depth 6

Write-Host "`n6. Notificaciones del estudiante..." -ForegroundColor Cyan
Invoke-RestMethod "$base/api/v1/notificaciones/usuario/$student" | ConvertTo-Json -Depth 6

Write-Host "`nDemo Midwar terminada." -ForegroundColor Green
