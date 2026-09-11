$ErrorActionPreference = "Stop"
$gateway = "http://localhost:18080"

Write-Host "1. Configuracion centralizada" -ForegroundColor Cyan
Invoke-RestMethod "http://localhost:18888/kido-curso-ms/prod" | ConvertTo-Json -Depth 8

Write-Host "2. Cursos iniciales" -ForegroundColor Cyan
Invoke-RestMethod "$gateway/api/v1/cursos" | ConvertTo-Json -Depth 8

Write-Host "3. Crear curso valido" -ForegroundColor Cyan
$curso = @{
  titulo = "Fotografia desde cero"
  descripcion = "Curso gratuito de prueba para S5"
  tipo = "GRATUITO"
  precio = 0
  docenteId = 1
  categoriaId = 1
  estado = "BORRADOR"
} | ConvertTo-Json
$creado = Invoke-RestMethod "$gateway/api/v1/cursos" -Method Post -ContentType "application/json" -Body $curso
$creado | ConvertTo-Json -Depth 8

Write-Host "4. Actualizar el curso creado" -ForegroundColor Cyan
$actualizado = @{
  titulo = "Fotografia practica"
  descripcion = "Curso gratuito actualizado durante la demostracion"
  tipo = "GRATUITO"
  precio = 0
  docenteId = 1
  categoriaId = 1
  estado = "BORRADOR"
} | ConvertTo-Json
Invoke-RestMethod "$gateway/api/v1/cursos/$($creado.id)" -Method Put -ContentType "application/json" -Body $actualizado | ConvertTo-Json -Depth 8

Write-Host "5. Caso invalido esperado: GRATUITO con precio 20" -ForegroundColor Cyan
$invalido = @{
  titulo = "Curso invalido"
  descripcion = "Debe responder 400"
  tipo = "GRATUITO"
  precio = 20
  docenteId = 1
  categoriaId = 1
  estado = "BORRADOR"
} | ConvertTo-Json
try {
  Invoke-RestMethod "$gateway/api/v1/cursos" -Method Post -ContentType "application/json" -Body $invalido
} catch {
  Write-Host "Respuesta controlada: HTTP $([int]$_.Exception.Response.StatusCode)" -ForegroundColor Yellow
}

Write-Host "6. Caso no encontrado esperado: ID inexistente" -ForegroundColor Cyan
try {
  Invoke-RestMethod "$gateway/api/v1/cursos/999999"
} catch {
  Write-Host "Respuesta controlada: HTTP $([int]$_.Exception.Response.StatusCode)" -ForegroundColor Yellow
}

Write-Host "7. Balanceo entre instancias" -ForegroundColor Cyan
1..8 | ForEach-Object {
  $respuesta = Invoke-WebRequest "$gateway/api/v1/cursos"
  Write-Host "Peticion $_ -> $($respuesta.Headers['X-Instance-ID'])"
}

Write-Host "8. Crear inscripcion con detalle" -ForegroundColor Cyan
$estudianteDemo = Get-Random -Minimum 2000 -Maximum 999999
$inscripcion = @{
  estudianteId = $estudianteDemo
  cursoId = 1
  tipoAcceso = "GRATUITO"
  leccionIds = @(1,2,3)
} | ConvertTo-Json
Invoke-RestMethod "$gateway/api/v1/inscripciones" -Method Post -ContentType "application/json" -Body $inscripcion | ConvertTo-Json -Depth 8

Write-Host "9. Eliminar el curso de demostracion" -ForegroundColor Cyan
Invoke-RestMethod "$gateway/api/v1/cursos/$($creado.id)" -Method Delete

Write-Host "Demo finalizada" -ForegroundColor Green
