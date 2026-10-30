# Configura un vendedor de PRUEBA sin enviar sus credenciales al chat.
$ErrorActionPreference = 'Stop'
$workspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$envPath = Join-Path $workspace '.env'
if (-not (Test-Path -LiteralPath $envPath)) { throw 'Primero crea .env a partir de .env.example.' }
Write-Host 'Mercado Pago - configuracion local de PRUEBAS para KIDO'
Write-Host 'Usa credenciales de prueba o las de un vendedor de prueba; no una cuenta vendedora real.'
$confirmacion = Read-Host 'Es una credencial exclusivamente de pruebas? Escribe SI'
if ($confirmacion.Trim().ToUpperInvariant() -ne 'SI') { throw 'No se modifico la configuracion.' }
$seguro = Read-Host 'Access Token (se oculta al escribir)' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
try { $tokenLocal = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero).Trim() }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero); $seguro.Dispose() }
if ($tokenLocal -notmatch '^(TEST-|APP_USR-)[A-Za-z0-9_-]+$') { throw 'Formato de Access Token no valido.' }
$notificacion = (Read-Host 'URL HTTPS publica del webhook (Enter para configurarla despues)').Trim()
if ($notificacion -and ($notificacion -notmatch '^https://[^\s]+/api/v1/pagos/webhooks/mercado-pago(?:\?[^\s]*)?$')) { throw 'La URL debe ser HTTPS y apuntar al webhook de KIDO.' }
$secretoLocal = ''
if ($notificacion) {
    $firma = Read-Host 'Clave secreta del webhook (se oculta al escribir)' -AsSecureString
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($firma)
    try { $secretoLocal = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero).Trim() }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero); $firma.Dispose() }
    if ($secretoLocal -notmatch '^[A-Za-z0-9_-]+$') { throw 'Clave secreta de webhook no valida.' }
}
$contenido = [IO.File]::ReadAllText($envPath)
$valores = @{
    MERCADO_PAGO_ACCESS_TOKEN = $tokenLocal
    MERCADO_PAGO_NOTIFICATION_URL = $notificacion
    MERCADO_PAGO_WEBHOOK_SECRET = $secretoLocal
    MERCADO_PAGO_RETURN_URL = 'http://localhost:4300/pago/resultado'
    MERCADO_PAGO_AUTO_RETURN = 'false'
    KIDO_PAGOS_PERMITIR_SIMULACION = 'false'
}
foreach ($nombre in $valores.Keys) {
    $patron = '(?m)^' + [regex]::Escape($nombre) + '=.*$'
    $linea = $nombre + '=' + $valores[$nombre]
    if ([regex]::IsMatch($contenido, $patron)) {
        $contenido = [regex]::Replace($contenido, $patron, [System.Text.RegularExpressions.MatchEvaluator]{ param($coincidencia) $linea })
    } else { $contenido += "`r`n$linea`r`n" }
}
[IO.File]::WriteAllText($envPath, $contenido, [Text.UTF8Encoding]::new($false))
$tokenLocal = $null; $secretoLocal = $null; $contenido = $null; $valores.Clear()
Write-Host 'Configuracion guardada en .env. No se realizo ningun cobro.'
Write-Host 'Para aplicarla ejecuta desde el proyecto: docker compose up -d --no-deps kido-pago-ms'
