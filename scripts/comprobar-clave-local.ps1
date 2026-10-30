param([string]$Email='adanyana@gmail.com')
$ErrorActionPreference='Stop'
$resultPath=Join-Path $PSScriptRoot '../.design-review/resultado-acceso.json'
try {
    Write-Host 'KIDO: comprobar acceso sin cambiar la contrasena' -ForegroundColor Cyan
    Write-Host "Cuenta: $Email"
    $secure=Read-Host 'Escribe tu contrasena actual (no se muestra ni se guarda)' -AsSecureString
    $plain=(New-Object System.Net.NetworkCredential('', $secure)).Password
    $login=Invoke-RestMethod -Method Post 'http://localhost:19090/realms/kido/protocol/openid-connect/token' -TimeoutSec 15 -ContentType 'application/x-www-form-urlencoded' -Body @{client_id='kido-cli';grant_type='password';username=$Email;password=$plain}
    if (-not $login.access_token) { throw 'No se recibio una sesion valida.' }
    @{ok=$true;timestamp=(Get-Date -Format o)} | ConvertTo-Json | Set-Content $resultPath -Encoding utf8
    Write-Host 'La contrasena es correcta. Tu cuenta puede ingresar. No se cambio ningun dato.' -ForegroundColor Green
} catch {
    $reason='connection_error'
    if ($_.ErrorDetails.Message) { try { $reason=($_.ErrorDetails.Message | ConvertFrom-Json).error_description } catch {} }
    @{ok=$false;reason=$reason;timestamp=(Get-Date -Format o)} | ConvertTo-Json | Set-Content $resultPath -Encoding utf8
    Write-Host 'Keycloak no acepto el acceso. No se cambio tu contrasena.' -ForegroundColor Yellow
    Write-Host $reason
} finally {
    $plain=$null; $secure=$null; $login=$null
    Read-Host 'Pulsa Enter para cerrar' | Out-Null
}
