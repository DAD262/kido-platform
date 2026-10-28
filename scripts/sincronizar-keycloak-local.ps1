param([string]$AdminUser, [string]$AdminPassword, [string]$AccessToken)
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:19090'
function ContainerEnv([string]$Name) {
    $info = (docker inspect $Name | ConvertFrom-Json)[0]
    if ($LASTEXITCODE -ne 0) { throw "No se pudo inspeccionar $Name" }
    $values = @{}
    foreach ($entry in $info.Config.Env) {
        $pair = $entry -split '=', 2
        $values[$pair[0]] = $pair[1]
    }
    return $values
}
$kc = ContainerEnv 'kido-keycloak'
$serviceName = docker compose ps -q kido-usuario-ms
$service = ContainerEnv $serviceName
if (-not $AccessToken) { $login = Invoke-RestMethod -Method Post -Uri "$base/realms/master/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body @{
    client_id = 'admin-cli'; grant_type = 'password'
    username = $(if ($AdminUser) { $AdminUser } else { $kc.KC_BOOTSTRAP_ADMIN_USERNAME })
    password = $(if ($AdminPassword) { $AdminPassword } else { $kc.KC_BOOTSTRAP_ADMIN_PASSWORD })
}
$AccessToken = $login.access_token }
$headers = @{ Authorization = "Bearer $AccessToken" }
function Admin([string]$Method, [string]$Path, $Body = $null) {
    $args = @{ Method = $Method; Uri = ("$base/admin/realms/kido/$Path").TrimEnd('/'); Headers = $headers; TimeoutSec = 15 }
    if ($null -ne $Body) {
        $args.ContentType = 'application/json; charset=utf-8'
        $args.Body = [System.Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $Body -Depth 30 -Compress))
    }
    $response = Invoke-RestMethod @args
    if ($response -is [Array]) { foreach ($item in $response) { Write-Output $item } } else { Write-Output $response }
}
$realm = Get-Content (Join-Path $PSScriptRoot '../infra/keycloak/kido-realm.json') -Raw -Encoding utf8 | ConvertFrom-Json
# Correccion autorizada: identificadores de acceso y permisos sobre la cuenta propia.
Admin 'Put' '' @{loginWithEmailAllowed=$true;editUsernameAllowed=$true;registrationEmailAsUsername=$false} | Out-Null
$accountClient = @(Admin 'Get' 'clients?clientId=account')[0]
$personalRoles = @('manage-account','view-profile') | ForEach-Object { Admin 'Get' "clients/$($accountClient.id)/roles/$_" }
Admin 'Post' 'roles/default-roles-kido/composites' @($personalRoles) | Out-Null
$personas = @(Admin 'Get' 'users?max=100') | Where-Object { $_.username -notlike 'service-account-*' -and -not $_.serviceAccountClientId }
foreach ($persona in $personas) {
    Admin 'Post' "users/$($persona.id)/role-mappings/clients/$($accountClient.id)" @($personalRoles) | Out-Null
}
$rolesScope = @(Admin 'Get' 'client-scopes') | Where-Object { $_.name -eq 'roles' } | Select-Object -First 1
if (-not $rolesScope) { throw 'No existe el scope roles de Keycloak.' }
foreach ($clientId in @('kido-cli','kido-frontend','account-console')) {
    $cliente = @(Admin 'Get' "clients?clientId=$clientId")[0]
    Admin 'Put' "clients/$($cliente.id)/default-client-scopes/$($rolesScope.id)" | Out-Null
}
Write-Host 'Acceso por usuario/correo y permisos personales sincronizados.'
$existingRoles = @(Admin 'Get' 'roles')
foreach ($role in $realm.roles.realm | Where-Object { $_.name -eq 'SUPERVISOR' }) {
    if (-not ($existingRoles | Where-Object { $_.name -eq $role.name })) {
        Admin 'Post' 'roles' @{name=$role.name;description=$role.description} | Out-Null
        Write-Host "Rol creado: $($role.name)"
    }
}
foreach ($client in $realm.clients) {
    if ($client.clientId -eq 'kido-usuario-admin') {
        $client.secret = $service.KIDO_USUARIO_ADMIN_SECRET
        if (-not $client.secret) { throw 'Falta KIDO_USUARIO_ADMIN_SECRET en el servicio activo.' }
    }
    $existing = @(Admin 'Get' "clients?clientId=$($client.clientId)" | Where-Object { $_ -and $_.id })
    if ($existing.Count) { Admin 'Put' "clients/$($existing[0].id)" $client | Out-Null }
    else { Admin 'Post' 'clients' $client | Out-Null }
    Write-Host "Cliente sincronizado: $($client.clientId)"
}
foreach ($clientId in @('kido-cli','kido-frontend','account-console')) {
    $cliente = @(Admin 'Get' "clients?clientId=$clientId")[0]
    Admin 'Put' "clients/$($cliente.id)/default-client-scopes/$($rolesScope.id)" | Out-Null
}
$client = @(Admin 'Get' 'clients?clientId=kido-usuario-admin')[0]
$account = Admin 'Get' "clients/$($client.id)/service-account-user"
$management = @(Admin 'Get' 'clients?clientId=realm-management')[0]
$roles = @('manage-users', 'view-users', 'query-users', 'view-realm') | ForEach-Object {
    Admin 'Get' "clients/$($management.id)/roles/$_"
}
Admin 'Post' "users/$($account.id)/role-mappings/clients/$($management.id)" @($roles) | Out-Null
Write-Host 'Permisos de registro y verificacion sincronizados. Se conservaron las cuentas existentes.'
