# kido-usuario-ms · Identidad de KIDO

Registro de cuentas (estudiante o docente), perfil de la sesión (`/me`) y verificación de docentes.
Keycloak sigue siendo el dueño de **credenciales y roles**; este servicio guarda lo que Keycloak no debe
guardar: el perfil académico, el **id numérico** que usan cursos, inscripciones y pagos, y las
postulaciones docentes con su PDF de sustento.

| Dato | Dónde vive |
|---|---|
| Correo, contraseña, roles (ADMIN / DOCENTE / ESTUDIANTE) | Keycloak, realm `kido` |
| Id numérico (estudianteId / docenteId), nombres, tipo de registro | `perfiles` |
| DNI, celular, grado, institución, experiencia, presentación, estado de la revisión | `solicitudes_docente` |
| PDF de sustento (≤ 5 MB, se valida la firma `%PDF-`) | `documentos_solicitud` |

## Endpoints (vía Gateway `http://localhost:18080`)

| Método y ruta | Acceso | Para qué |
|---|---|---|
| `POST /api/v1/usuarios/registro` | público | `multipart/form-data`: parte `datos` (JSON) y, si es docente, parte `documento` (PDF). Crea la cuenta en Keycloak con rol ESTUDIANTE. |
| `GET /api/v1/usuarios/registro/disponibilidad?email=` | público | ¿El correo está libre? (con límite por IP) |
| `GET /api/v1/usuarios/me` | sesión | Perfil + id numérico + roles + estado de su postulación docente |
| `POST /api/v1/usuarios/me/solicitud-docente` | sesión | Un estudiante postula como docente, o corrige una postulación observada |
| `GET /api/v1/usuarios/me/solicitud-docente/documento` | sesión | Su propio PDF |
| `GET /api/v1/usuarios/admin/solicitudes?estado=ACTIVAS\|TODAS\|PENDIENTE…` | ADMIN | Bandeja de postulaciones |
| `GET /api/v1/usuarios/admin/solicitudes/resumen` | ADMIN | Contadores por estado |
| `GET /api/v1/usuarios/admin/solicitudes/{id}/documento` | ADMIN | PDF en línea |
| `POST /api/v1/usuarios/admin/solicitudes/{id}/aprobar` | ADMIN | Asigna el rol **DOCENTE** en Keycloak |
| `POST /api/v1/usuarios/admin/solicitudes/{id}/observar` | ADMIN | Pide corrección (comentario obligatorio) |
| `POST /api/v1/usuarios/admin/solicitudes/{id}/rechazar` | ADMIN | Rechaza (comentario obligatorio) |

Errores: JSON con `message` en español y, si aplica, `campos` (`{"dni": "El DNI tiene 8 dígitos."}`) para marcar el campo exacto en el formulario.

## Cómo habla con Keycloak

Usa el cliente confidencial `kido-usuario-admin` (cuenta de servicio, `client_credentials`) definido en
`infra/keycloak/kido-realm.json`, con los roles mínimos de `realm-management`: `manage-users`,
`view-users`, `query-users` y `view-realm`. El secreto llega por `KIDO_USUARIO_ADMIN_SECRET`.

Registro con compensación: se valida todo (incluido el PDF) → se crea el usuario en Keycloak → se le
asigna ESTUDIANTE → se guarda el perfil en una transacción. Si el último paso falla, se borra el usuario
de Keycloak para no dejar cuentas huérfanas.

## Ejecutar solo este servicio (perfil dev)

```powershell
docker compose up -d postgres-usuario kido-config kido-eureka kido-keycloak
cd services/kido-usuario-ms
$env:DB_USER="kido"; $env:DB_PASS="tu_password"; $env:KIDO_USUARIO_ADMIN_SECRET="CAMBIAR_SECRETO_CLIENTE_USUARIOS"
.\mvnw.cmd spring-boot:run      # puerto 8085, Swagger en /swagger-ui.html
```

Pruebas: `.\mvnw.cmd test` (reglas de registro y limitador de intentos).

Migraciones Flyway: `V1` esquema, `V2` perfiles de las cuentas demo (docente = 1, estudiante = 1001, como
los datos semilla de cursos e inscripciones), `V3` una postulación de muestra (`postulante`) para aprobarla en vivo.
