$ErrorActionPreference = "Stop"
$projects = @(
  "infra/kido-config",
  "infra/kido-eureka",
  "infra/kido-gateway",
  "services/kido-curso-ms",
  "services/kido-inscripcion-ms",
  "services/kido-pago-ms",
  "services/kido-notificacion-ms"
)
foreach ($project in $projects) {
  Write-Host "Construyendo $project" -ForegroundColor Cyan
  Push-Location $project
  .\mvnw.cmd -q -DskipTests clean package
  if ($LASTEXITCODE -ne 0) { throw "Falló Maven en $project" }
  Pop-Location
}
Write-Host "Todos los módulos compilaron." -ForegroundColor Green
