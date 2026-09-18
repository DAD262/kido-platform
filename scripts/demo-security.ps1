$ErrorActionPreference = "Stop"
$Keycloak = "http://localhost:19090"
$Gateway = "http://localhost:18080"

function Get-KidoToken([string]$User, [string]$Password) {
    $response = Invoke-RestMethod -Method Post `
        -Uri "$Keycloak/realms/kido/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{ client_id="kido-cli"; grant_type="password"; username=$User; password=$Password }
    return $response.access_token
}

function Get-Status([string]$Method, [string]$Uri, [string]$Token = "", [string]$Body = "") {
    try {
        $headers = @{}
        if ($Token) { $headers.Authorization = "Bearer $Token" }
        if ($Body) {
            $r = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $Uri -Headers $headers -ContentType "application/json" -Body $Body
        } else {
            $r = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $Uri -Headers $headers
        }
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

Write-Host "=== KIDO - DEMO KEYCLOAK/JWT ===" -ForegroundColor Cyan
$student = Get-KidoToken "estudiante" "Estudiante123!"
$teacher = Get-KidoToken "docente" "Docente123!"
$admin = Get-KidoToken "admin" "Admin123!"
Write-Host "Tokens obtenidos: ESTUDIANTE, DOCENTE y ADMIN" -ForegroundColor Green

$publicCourses = Get-Status "GET" "$Gateway/api/v1/cursos"
$privateNoToken = Get-Status "GET" "$Gateway/api/v1/notificaciones"
$studentNotifications = Get-Status "GET" "$Gateway/api/v1/notificaciones" $student
$studentCannotCreateCourse = Get-Status "POST" "$Gateway/api/v1/cursos" $student '{"titulo":"NO DEBE CREARSE","descripcion":"prueba","precio":0,"tipo":"GRATUITO","docenteId":1,"categoriaId":1}'
$teacherBalance = Get-Status "GET" "$Gateway/api/v1/pagos/docentes/1/saldo" $teacher
$adminRefunds = Get-Status "GET" "$Gateway/api/v1/reembolsos" $admin

Write-Host "GET cursos sin token................ $publicCourses (esperado 200)"
Write-Host "GET notificaciones sin token......... $privateNoToken (esperado 401)"
Write-Host "GET notificaciones ESTUDIANTE........ $studentNotifications (esperado 200)"
Write-Host "POST curso como ESTUDIANTE............ $studentCannotCreateCourse (esperado 403)"
Write-Host "GET saldo como DOCENTE................ $teacherBalance (esperado 200)"
Write-Host "GET reembolsos como ADMIN............. $adminRefunds (esperado 200)"
Write-Host "=== FIN DEMO SEGURIDAD ===" -ForegroundColor Cyan
