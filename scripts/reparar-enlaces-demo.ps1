# Repara SOLO las tres cuentas demo tras cambiar de realm Keycloak.
# Valida cada identidad mediante login y conserva los ids de cursos e inscripciones.
$ErrorActionPreference = 'Stop'
$info = (docker inspect kido-postgres-usuario | ConvertFrom-Json)[0]
$dbUser = (($info.Config.Env | Where-Object { $_ -like 'POSTGRES_USER=*' }) -split '=',2)[1]
if (-not $dbUser) { throw 'No se pudo determinar el usuario PostgreSQL.' }
$backup = docker exec kido-postgres-usuario psql -U $dbUser -d kido_usuario_db -t -A -c "SELECT coalesce(json_agg(p), '[]') FROM (SELECT id, username, keycloak_id FROM perfiles WHERE (id=1 AND username='docente') OR (id=1001 AND username='estudiante') OR (id=900 AND username='admin')) p;"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo respaldar los enlaces anteriores.' }
$backupPath = Join-Path $env:TEMP ('kido-enlaces-demo-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.json')
$backup | Set-Content $backupPath -Encoding utf8
$sql = @('BEGIN;')
foreach ($demo in @(
    @{ Name='estudiante'; Password='Estudiante123!'; Id=1001 },
    @{ Name='docente'; Password='Docente123!'; Id=1 },
    @{ Name='admin'; Password='Admin123!'; Id=900 }
)) {
    $token = Invoke-RestMethod -Method Post -Uri 'http://localhost:19090/realms/kido/protocol/openid-connect/token' -ContentType 'application/x-www-form-urlencoded' -Body @{client_id='kido-cli';grant_type='password';username=$demo.Name;password=$demo.Password}
    $payload = $token.access_token.Split('.')[1].Replace('-', '+').Replace('_', '/')
    $payload = $payload.PadRight([int]([Math]::Ceiling($payload.Length / 4.0) * 4), '=')
    $claims = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) | ConvertFrom-Json
    if ($claims.preferred_username -ne $demo.Name -or $claims.sub -notmatch '^[a-zA-Z0-9-]{1,64}$') { throw 'Identidad demo inesperada.' }
    $sql += "UPDATE perfiles SET keycloak_id='$($claims.sub)', actualizado_en=now() WHERE id=$($demo.Id) AND username='$($demo.Name)';"
}
$sql += 'COMMIT;'
($sql -join "`n") | docker exec -i kido-postgres-usuario psql -v ON_ERROR_STOP=1 -U $dbUser -d kido_usuario_db
if ($LASTEXITCODE -ne 0) { throw 'No se pudo actualizar los enlaces demo.' }
Write-Host "Enlaces demo reparados. Respaldo: $backupPath"
