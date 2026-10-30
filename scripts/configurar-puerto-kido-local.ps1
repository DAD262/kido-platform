param([string]$AccessToken)
$ErrorActionPreference='Stop'
$base='http://localhost:19090'
if(-not $AccessToken){
 $kc=(docker inspect kido-keycloak | ConvertFrom-Json)[0]
 if($LASTEXITCODE -ne 0){throw 'Keycloak local de Kido no está disponible.'}
 $values=@{}
 foreach($entry in $kc.Config.Env){$pair=$entry -split '=',2;$values[$pair[0]]=$pair[1]}
 try {
  $login=Invoke-RestMethod -Method Post -Uri "$base/realms/master/protocol/openid-connect/token" -Body @{client_id='admin-cli';grant_type='password';username=$values.KC_BOOTSTRAP_ADMIN_USERNAME;password=$values.KC_BOOTSTRAP_ADMIN_PASSWORD}
  $AccessToken=$login.access_token
 } catch {throw 'No se pudo autenticar al administrador de Keycloak. Actualiza únicamente el cliente kido-frontend: raíz http://localhost:4300, retorno http://localhost:4300/* y origen http://localhost:4300, o ejecuta este script con un AccessToken administrativo válido.'}
}
$headers=@{Authorization="Bearer $AccessToken"}
$clients=@(Invoke-RestMethod "$base/admin/realms/kido/clients?clientId=kido-frontend" -Headers $headers)
$client=$clients | Where-Object {$_.clientId -eq 'kido-frontend'} | Select-Object -First 1
if(-not $client){throw 'No existe el cliente kido-frontend en el realm kido.'}
# Solo actualiza las URLs de este cliente; no cambia secretos, usuarios ni roles.
$body=@{rootUrl='http://localhost:4300';baseUrl='http://localhost:4300/';redirectUris=@('http://localhost:4300/*');webOrigins=@('http://localhost:4300')} | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$base/admin/realms/kido/clients/$($client.id)" -Headers $headers -ContentType 'application/json' -Body $body | Out-Null
$actual=Invoke-RestMethod "$base/admin/realms/kido/clients/$($client.id)" -Headers $headers
if($actual.rootUrl -ne 'http://localhost:4300' -or @($actual.redirectUris) -contains 'http://localhost:4200/*'){throw 'La verificación del cliente Keycloak no coincide con el puerto de Kido.'}
Write-Output 'Cliente kido-frontend verificado: raíz y origen localhost:4300, retorno localhost:4300/*.'
