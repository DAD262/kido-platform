$ErrorActionPreference='Stop'
$info=(docker inspect kido-keycloak | ConvertFrom-Json)[0]
$network=@($info.NetworkSettings.Networks.PSObject.Properties.Name)[0]
$credentialFile=Join-Path $env:TEMP 'kido-recovery-credential.xml'
$oldUser=if (Test-Path $credentialFile) { (Import-Clixml $credentialFile).UserName } else { $null }
$clientId='kido-recovery-service-'+[guid]::NewGuid().ToString('N').Substring(0,8)
$secret=[guid]::NewGuid().ToString('N')+'Aa1!'
(New-Object System.Management.Automation.PSCredential($clientId,(ConvertTo-SecureString $secret -AsPlainText -Force))) | Export-Clixml (Join-Path $env:TEMP 'kido-recovery-service.xml')
$restore=@{}
$dockerArgs=@('run','--rm','--network',$network)
foreach ($entry in $info.Config.Env) {
    $pair=$entry -split '=',2
    if ($pair[0] -match '^KC_DB($|_)') {
        $restore[$pair[0]]=[Environment]::GetEnvironmentVariable($pair[0],'Process')
        [Environment]::SetEnvironmentVariable($pair[0],$pair[1],'Process')
        $dockerArgs+=@('--env',$pair[0])
    }
}
$restore['KIDO_RECOVERY_SECRET']=$env:KIDO_RECOVERY_SECRET
$env:KIDO_RECOVERY_SECRET=$secret
$dockerArgs+=@('--env','KIDO_RECOVERY_SECRET',$info.Config.Image,'bootstrap-admin','service','--client-id',$clientId,'--client-secret:env','KIDO_RECOVERY_SECRET','--no-prompt')
try {
    docker stop kido-keycloak | Out-Null
    try { docker @dockerArgs; if ($LASTEXITCODE -ne 0) { throw 'Fallo la creacion del servicio temporal.' } }
    finally { docker start kido-keycloak | Out-Null }
    $login=$null
    for ($attempt=0;$attempt -lt 30;$attempt++) {
        try { $login=Invoke-RestMethod -Method Post 'http://localhost:19090/realms/master/protocol/openid-connect/token' -TimeoutSec 3 -ContentType 'application/x-www-form-urlencoded' -Body @{client_id=$clientId;client_secret=$secret;grant_type='client_credentials'}; break }
        catch { Start-Sleep -Seconds 2 }
    }
    if (-not $login) { throw 'No se pudo autenticar el servicio temporal.' }
    & (Join-Path $PSScriptRoot 'sincronizar-keycloak-local.ps1') -AccessToken $login.access_token
    $headers=@{Authorization="Bearer $($login.access_token)"}
    if ($oldUser -like 'kido-recovery-*') {
        $users=@(Invoke-RestMethod "http://localhost:19090/admin/realms/master/users?username=$oldUser&exact=true" -Headers $headers -TimeoutSec 10)
        foreach ($user in $users) { if ($user.username -eq $oldUser) { Invoke-RestMethod -Method Delete "http://localhost:19090/admin/realms/master/users/$($user.id)" -Headers $headers -TimeoutSec 10 } }
        Remove-Item -LiteralPath $credentialFile
    }
    $clients=@(Invoke-RestMethod "http://localhost:19090/admin/realms/master/clients?clientId=$clientId" -Headers $headers -TimeoutSec 10)
    if ($clients.Count -ne 1) { throw 'No se encontro el cliente temporal.' }
    Invoke-RestMethod -Method Delete "http://localhost:19090/admin/realms/master/clients/$($clients[0].id)" -Headers $headers -TimeoutSec 10
    Remove-Item -LiteralPath (Join-Path $env:TEMP 'kido-recovery-service.xml')
    Write-Host 'Sincronizacion completada; usuario y servicio temporales eliminados.'
} finally { foreach ($name in $restore.Keys) { [Environment]::SetEnvironmentVariable($name,$restore[$name],'Process') } }
