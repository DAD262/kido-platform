$ErrorActionPreference = 'Continue'
Write-Host 'KIDO - Diagnóstico local' -ForegroundColor Cyan
Write-Host 'Directorio actual:' (Get-Location).Path
Write-Host ''

if (-not (Test-Path '.env')) {
  Write-Warning 'Falta .env. Copia .env.example y completa credenciales locales.'
}

Write-Host 'Servicios configurados:'
docker compose config --services
Write-Host ''

Write-Host 'Estado de contenedores:'
docker compose ps -a
Write-Host ''

Write-Host 'Puertos del servidor:'
foreach ($puerto in @(4300, 18080, 19090, 18889)) {
  $listener = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue
  if ($listener) {
    Write-Host "$puerto : ESCUCHANDO"
  } else {
    Write-Host "$puerto : SIN LISTENER"
  }
}

Write-Host ''
Write-Host 'Recomendación: si Angular carga pero las APIs fallan, verifica Gateway y proxy.conf.json.'
Write-Host 'Este script no modifica datos ni reinicia contenedores.'
