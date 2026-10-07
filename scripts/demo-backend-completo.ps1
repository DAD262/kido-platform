param(
  [string]$Gateway = "http://localhost:18080",
  [string]$Token = ""
)
$headers = @{}
if ($Token) { $headers.Authorization = "Bearer $Token" }

Write-Host "=== KIDO backend pre-frontend: comprobaciones ===" -ForegroundColor Cyan
Write-Host "1) Cursos publicados"
Invoke-RestMethod "$Gateway/api/v1/cursos/publicados" -Headers $headers

Write-Host "`n2) Para probar contenido, configure un curso existente:" -ForegroundColor Yellow
Write-Host 'PUT /api/v1/cursos/{cursoId}/configuracion'
Write-Host '{"progresoMinimo":80,"asistenciaMinima":75,"certificadoHabilitado":true,"certificadoCosto":20.00}'

Write-Host "`n3) Flujo docente:" -ForegroundColor Yellow
Write-Host 'crear modulo -> crear leccion -> subir material multipart -> enviar a revision'
Write-Host 'POST /api/v1/cursos/{id}/modulos'
Write-Host 'POST /api/v1/modulos/{id}/lecciones'
Write-Host 'POST /api/v1/lecciones/{id}/materiales/upload'
Write-Host 'POST /api/v1/cursos/{id}/enviar-revision'

Write-Host "`n4) Flujo administrador:" -ForegroundColor Yellow
Write-Host 'POST /api/v1/cursos/{id}/aprobar  |  POST /api/v1/cursos/{id}/rechazar'

Write-Host "`n5) Flujo estudiante/docente:" -ForegroundColor Yellow
Write-Host 'completar lecciones -> registrar asistencias -> consultar estado academico -> solicitar certificado'
Write-Host 'GET /api/v1/inscripciones/{id}/estado-academico'
Write-Host 'POST /api/v1/inscripciones/{id}/certificado'
Write-Host 'GET /api/v1/inscripciones/{id}/certificado/pdf'

Write-Host "`n6) Si el certificado es pagado:" -ForegroundColor Yellow
Write-Host 'POST /api/v1/pagos/certificados/ordenes'
Write-Host 'POST /api/v1/pagos/certificados/ordenes/{id}/mercado-pago'
Write-Host 'POST /api/v1/pagos/certificados/ordenes/{id}/confirmar-mercado-pago?paymentId=...'
