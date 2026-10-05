$ErrorActionPreference = "Stop"
$Keycloak = "http://localhost:19090"
$Pago = "http://localhost:18083"

function Get-KidoToken([string]$User, [string]$Password) {
    $response = Invoke-RestMethod -Method Post `
        -Uri "$Keycloak/realms/kido/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{ client_id="kido-cli"; grant_type="password"; username=$User; password=$Password }
    return $response.access_token
}

function Get-Status([string]$Uri, [string]$Token = "") {
    try {
        $headers = @{}
        if ($Token) { $headers.Authorization = "Bearer $Token" }
        $response = Invoke-WebRequest -UseBasicParsing -Method Get -Uri $Uri -Headers $headers
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

Write-Host "=== TAREA S7 - RESOURCE SERVER KIDO PAGO ===" -ForegroundColor Cyan
$student = Get-KidoToken "estudiante" "Estudiante123!"
$teacher = Get-KidoToken "docente" "Docente123!"
$noToken = Get-Status "$Pago/api/v1/pagos/ordenes"
$badToken = Get-Status "$Pago/api/v1/pagos/ordenes" "token-invalido"
$studentDenied = Get-Status "$Pago/api/v1/pagos/docentes/1/saldo" $student
$teacherAllowed = Get-Status "$Pago/api/v1/pagos/docentes/1/saldo" $teacher

Write-Host "Pago directo sin token................ $noToken (esperado 401)"
Write-Host "Pago directo con token invalido....... $badToken (esperado 401)"
Write-Host "Saldo docente como ESTUDIANTE......... $studentDenied (esperado 403)"
Write-Host "Saldo docente como DOCENTE............ $teacherAllowed (esperado 200)"

if ($noToken -ne 401 -or $badToken -ne 401 -or $studentDenied -ne 403 -or $teacherAllowed -ne 200) {
    throw "Falló una o más verificaciones de seguridad directa del servicio de pagos."
}
Write-Host "Todas las verificaciones S7 pasaron." -ForegroundColor Green
