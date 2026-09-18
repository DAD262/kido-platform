# Seguridad con Keycloak

Kido utiliza **Keycloak** como proveedor de identidad y **JWT Bearer** en el API Gateway.
El Gateway valida la firma del token contra el JWKS del realm `kido` y convierte los roles del claim `realm_access.roles` en autoridades Spring Security.

## Componentes

- Keycloak: `http://localhost:19090`
- Realm: `kido`
- Cliente de demo: `kido-cli`
- Gateway protegido: `http://localhost:18080`

Keycloak es infraestructura de identidad, por lo que **no se registra en Eureka**.

## Roles

| Rol | Acceso principal |
|---|---|
| `ESTUDIANTE` | inscripciones, órdenes de pago, solicitudes de reembolso y notificaciones |
| `DOCENTE` | administración de cursos, consulta de ventas/saldo y solicitud de retiro |
| `ADMIN` | operaciones administrativas, retiros, reembolsos y soporte |

El catálogo `GET /api/v1/cursos/**` continúa público. El webhook de Mercado Pago también queda público para permitir callbacks externos. Las rutas internas `/internal/v1/**` no están publicadas por Gateway.

## Usuarios de demostración

Solo para el entorno académico local:

- `estudiante` / `Estudiante123!`
- `docente` / `Docente123!`
- `admin` / `Admin123!`

Cambiar estas contraseñas antes de cualquier despliegue real.

## Obtener token

```powershell
$token = (Invoke-RestMethod -Method Post `
  -Uri "http://localhost:19090/realms/kido/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{client_id="kido-cli";grant_type="password";username="estudiante";password="Estudiante123!"}).access_token
```

Usar el token:

```powershell
Invoke-RestMethod -Headers @{Authorization="Bearer $token"} `
  http://localhost:18080/api/v1/notificaciones
```

## Prueba automática

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-security.ps1
```

La demo verifica respuestas `200`, `401` y `403` para demostrar autenticación y autorización por rol.
