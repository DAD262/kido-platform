$ErrorActionPreference='Stop'
$base='http://localhost:4200'
function Check([string]$Path,$Headers=@{},[int]$Expected=200) {
    try { $r=Invoke-WebRequest -UseBasicParsing ($base+$Path) -Headers $Headers -TimeoutSec 15; $status=[int]$r.StatusCode }
    catch { if ($_.Exception.Response) { $status=[int]$_.Exception.Response.StatusCode } else { throw } }
    if ($status -ne $Expected) { throw "$Path HTTP $status; esperado $Expected" }
    Write-Host "OK $status $Path"
}
Check '/'
Check '/api/v1/cursos/publicados'
Check '/api/v1/usuarios/me' @{} 401
foreach ($account in @(
    @{Name='estudiante';Password='Estudiante123!';Paths=@('/usuarios/me','/usuarios/me/deseados','/inscripciones/estudiante/1001','/pagos/ordenes/estudiante/1001','/notificaciones/usuario/1001')},
    @{Name='docente';Password='Docente123!';Paths=@('/usuarios/me','/cursos/docente/1','/pagos/docentes/1/saldo')},
    @{Name='admin';Password='Admin123!';Paths=@('/usuarios/me','/usuarios/admin/solicitudes/resumen','/reembolsos')}
)) {
    $token=Invoke-RestMethod -Method Post 'http://localhost:19090/realms/kido/protocol/openid-connect/token' -ContentType 'application/x-www-form-urlencoded' -TimeoutSec 15 -Body @{client_id='kido-cli';grant_type='password';username=$account.Name;password=$account.Password}
    $headers=@{Authorization="Bearer $($token.access_token)"}
    Write-Host "Cuenta: $($account.Name)"
    foreach ($path in $account.Paths) { Check ('/api/v1'+$path) $headers }
    if ($account.Name -eq 'estudiante') { Check '/api/v1/usuarios/admin/solicitudes/resumen' $headers 403 }
}
Write-Host 'Validacion local completada.'
