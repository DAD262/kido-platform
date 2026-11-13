# Guía para levantar KIDO completo

Esta guía deja funcionando todo el proyecto en tu PC: backend (microservicios, Keycloak, Kafka, bases de datos), frontend Angular y, sobre todo, el **acceso**: ingreso, registro de estudiantes, postulación y verificación de docentes.

Los comandos están pensados para **Windows + PowerShell** (los mismos sirven en macOS/Linux quitando `.cmd`).

---

## 1. Qué vas a levantar

```
Navegador ──▶ Angular :4200 ──proxy /api──▶ Gateway :18080 ──▶ microservicios (vía Eureka)
   │                                            │
   └──── login (tema «kido») ──▶ Keycloak :19090 ◀── kido-usuario-ms (cuenta de servicio)
```

| Pieza | Puerto en tu PC | Para qué |
|---|---|---|
| Frontend Angular | `4200` | La aplicación |
| API Gateway | `18080` | Única puerta del API; valida el JWT y el rol |
| Keycloak | `19090` | Contraseñas, sesiones y roles (realm `kido`) |
| Eureka | `18761` | Registro de servicios (debes ver todos en UP) |
| Config Server | `18889` | Configuración central (`infra/kido-config/config-repo`) |
| Kafka UI | `18085` | Eventos de pagos y SAGA |
| Prometheus / Grafana | `19091` / `13000` | Métricas |
| PostgreSQL | `15432` cursos · `15434` inscripciones · `15435` pagos · `15436` notificaciones · **`15437` usuarios** | Una base por servicio |

Microservicios: `kido-curso-ms` (2 instancias), `kido-inscripcion-ms`, `kido-pago-ms`, `kido-notificacion-ms` y el nuevo **`kido-usuario-ms`** (identidad).

## 2. Requisitos

- **Docker Desktop** en marcha, con al menos 6 GB de RAM asignados (Settings → Resources).
- **Node.js 20 o 22** (`node -v`).
- Opcional: Java 21 si quieres compilar o probar un servicio fuera de Docker.

## 3. Prepara `.env`

En la raíz de `kido-platform`:

```powershell
Copy-Item .env.example .env   # solo la primera vez
notepad .env
```

Revisa al menos:

| Variable | Valor sugerido en tu PC |
|---|---|
| `DB_PASS` | una contraseña cualquiera para las bases locales |
| `KEYCLOAK_ADMIN_PASSWORD` | la clave de la consola de Keycloak (usuario `admin`) |
| `KIDO_USUARIO_ADMIN_SECRET` | `CAMBIAR_SECRETO_CLIENTE_USUARIOS` (o tu propio secreto; ver 9.3) |
| `MERCADO_PAGO_*` | solo si vas a probar pagos reales de prueba (`docs/mercado-pago.md`) |

> Si ya tenías un `.env`, **agrega** `KIDO_USUARIO_ADMIN_SECRET`. Si no lo pones, se usa `CAMBIAR_SECRETO_CLIENTE_USUARIOS` en Keycloak y en el servicio, que coinciden.

## 4. Levanta el backend

### 4.1 Primera vez (o si quieres empezar limpio)

```powershell
docker compose up -d --build
```

La primera construcción tarda varios minutos (Maven descarga dependencias dentro de Docker). Después:

```powershell
docker compose ps                    # todo en "running" / "healthy"
docker compose logs -f kido-usuario-ms
```

Espera a ver `Started KidoUsuarioApplication`. Luego abre <http://localhost:18761>: deben aparecer `KIDO-GATEWAY`, `KIDO-CURSO-MS` (×2), `KIDO-INSCRIPCION-MS`, `KIDO-PAGO-MS`, `KIDO-NOTIFICACION-MS` y **`KIDO-USUARIO-MS`**.

### 4.2 Si ya tenías KIDO levantado antes de esta versión

Keycloak **solo importa el realm cuando no existe**. Como el realm cambió (cliente de servicio, política de contraseñas, nuevas cuentas demo, tema), recrea su contenedor para actualizar montajes y tema. Los clientes de un realm existente se actualizan con `scripts/sincronizar-keycloak-local.ps1`, no mediante la importación inicial:

```powershell
docker compose up -d --force-recreate kido-keycloak       # conserva usuarios; actualiza montajes y tema
docker compose restart kido-config                         # lee las rutas y configs nuevas
docker compose up -d --build kido-usuario-ms kido-gateway  # servicio nuevo + reglas del Gateway
```

> Keycloak usa PostgreSQL persistente. Recrear su contenedor conserva las cuentas y no vuelve a importar un realm existente. Para actualizar clientes y permisos sin borrar usuarios, ejecuta `scripts/sincronizar-keycloak-local.ps1` con las credenciales administrativas vigentes.

## 5. Levanta el frontend

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

Abre <http://localhost:4200>. El proxy de Angular manda `/api` al Gateway, así no hay problemas de CORS.

---

## 6. Acceso: cómo está separado y definido

### 6.1 Quién hace qué

| Responsabilidad | Dónde | Archivo clave |
|---|---|---|
| Guardar y comprobar contraseñas, sesiones, roles | **Keycloak** | `infra/keycloak/kido-realm.json` |
| Pantalla de correo y contraseña con la marca KIDO | **Keycloak, tema `kido`** | `infra/keycloak/themes/kido/login/` |
| Elegir cómo entrar, crear cuenta, postular como docente | **Angular** | `pages/login`, `pages/registro`, `pages/postular-docente` |
| Crear cuentas, perfil (`/me`), verificar docentes | **kido-usuario-ms** | `services/kido-usuario-ms` |
| Exigir sesión y rol en cada ruta del API | **Gateway** (y cada servicio) | `infra/kido-gateway/.../SecurityConfig.java` |

KIDO nunca ve ni guarda contraseñas: el formulario de contraseña es de Keycloak y la app solo recibe un token (PKCE).

### 6.2 Roles: cómo se obtiene cada uno

| Rol | Cómo se obtiene | Qué habilita |
|---|---|---|
| `ESTUDIANTE` | Automático al registrarse (y rol por defecto del realm) | Comprar cursos, aprender, certificados |
| `DOCENTE` | **Solo** cuando administración aprueba una postulación | Crear cursos, ver alumnos, saldo y retiros |
| `ADMIN` | Solo desde la consola de Keycloak | Aprobar cursos, pagos, retiros y docentes |

El navegador **nunca elige un rol**. Registrarse «como docente» crea una cuenta ESTUDIANTE + una postulación PENDIENTE.

### 6.3 Ingreso

1. `/login` (Angular) → **Ingresar con mi correo**.
2. Keycloak muestra su formulario (tema `kido`): correo o usuario + contraseña.
3. Vuelve a `/login`, que te lleva a tu panel según tu rol: Administración, Enseñar o Mi aprendizaje.

Protecciones activas: política de contraseña (8+ caracteres, mayúscula, minúscula, número; no puede ser el usuario ni el correo) y bloqueo temporal tras 5 intentos fallidos.

### 6.4 Registro de estudiante (1 paso)

`/registro` → **Quiero aprender** → nombres, apellidos, correo, contraseña (con medidor y reglas en vivo) y términos → **Crear mi cuenta** → **Ingresar ahora** (el correo ya viene escrito en Keycloak).

### 6.5 Registro de docente (3 pasos) y verificación

| Paso | Qué se pide | Por qué |
|---|---|---|
| 1. Tu cuenta | Nombres, apellidos, correo, contraseña | Igual que un estudiante |
| 2. Perfil profesional | **DNI** (8 dígitos), **celular** (+51, 9 dígitos), **grado académico** (técnico → doctor), **qué enseñará**, **institución**, **años de experiencia**, enlace LinkedIn/portafolio/ORCID (opcional), **presentación** (80–1200 caracteres) | Identificar a la persona y su trayectoria |
| 3. Verificación | **PDF de sustento** (CV, título, constancia o colegiatura; máx. 5 MB, se comprueba que sea un PDF real) y **declaración jurada** | Evidencia que administración revisa |

Estados de la postulación:

```
PENDIENTE ──aprobar──▶ APROBADA  (Keycloak asigna DOCENTE)
    │
    ├──observar (con comentario)──▶ OBSERVADA ──la persona corrige y reenvía──▶ PENDIENTE
    └──rechazar (con motivo)──────▶ RECHAZADA ──puede volver a postular──────▶ PENDIENTE
```

- **Administración** revisa en `/admin` → pestaña **Docentes y cuentas**: ve los datos, el PDF incrustado y debe marcar 3 verificaciones antes de poder aprobar.
- **El postulante** sigue el estado en `/postular-docente` (menú de su avatar → «Enseñar en KIDO»). Cuando lo aprueban, pulsa **Activar mi panel docente**: la app renueva el token y aparece el panel de docente sin cerrar sesión.
- Un estudiante que ya tiene cuenta también puede postular desde `/postular-docente`.

### 6.6 Del usuario de Keycloak al número de KIDO

Cursos, inscripciones y pagos identifican a las personas con un número (`docenteId`, `estudianteId`). Antes había que escribirlo a mano; ahora `GET /api/v1/usuarios/me` lo devuelve:

- Cuentas nuevas: número desde 2001.
- Cuentas demo: `docente` = **1** y `estudiante` = **1001**, para que coincidan con los cursos e inscripciones sembrados.
- Cuentas creadas a mano en la consola de Keycloak: se les crea el perfil la primera vez que entran.

### 6.7 Endpoints de identidad

| Método y ruta | Acceso |
|---|---|
| `POST /api/v1/usuarios/registro` (multipart: `datos` JSON + `documento` PDF) | público |
| `GET /api/v1/usuarios/registro/disponibilidad?email=` | público |
| `GET /api/v1/usuarios/me` | con sesión |
| `POST /api/v1/usuarios/me/solicitud-docente` · `GET …/documento` | con sesión |
| `GET /api/v1/usuarios/admin/solicitudes[?estado=]` · `/resumen` · `/{id}` · `/{id}/documento` | ADMIN |
| `POST /api/v1/usuarios/admin/solicitudes/{id}/aprobar` · `/observar` · `/rechazar` | ADMIN |

Más detalle en `services/kido-usuario-ms/README.md`.

---

## 7. Recorrido para la sustentación

Cuentas demo (también aparecen en `/login` → «Cuentas de demostración», con botón **Usar**):

| Usuario | Contraseña | Qué muestra |
|---|---|---|
| `estudiante` | `Estudiante123!` | Mi aprendizaje (estudiante n.º 1001) |
| `docente` | `Docente123!` | Panel docente con cursos (docente n.º 1) |
| `postulante` | `Postulante123!` | Postulación docente **pendiente** con su PDF |
| `admin` | `Admin123!` | Administración |

1. **Registro de estudiante.** `/registro` → Quiero aprender → crea una cuenta → Ingresar ahora → llegas a Mi aprendizaje sin vincular nada.
2. **Registro de docente.** En otra ventana de incógnito: `/registro` → Quiero enseñar → completa los 3 pasos con un PDF cualquiera → «Postulación enviada».
3. **Revisión.** Entra como `admin` → Administración → **Docentes y cuentas** (el contador muestra las pendientes) → abre la postulación de `postulante` → revisa el PDF, marca las 3 verificaciones → **Aprobar y dar rol docente**.
4. **Activación.** Entra como `postulante` → avatar → «Enseñar en KIDO» → **Activar mi panel docente** → ya puede crear cursos.
5. **Corrección.** Con la postulación del paso 2: **Pedir corrección** con un comentario → el postulante lo ve en `/postular-docente`, corrige, reemplaza el PDF y reenvía.
6. **Seguridad.** Intenta abrir `/docente` con una cuenta estudiante: la app explica que debe postular. Intenta 5 contraseñas erradas: Keycloak bloquea un minuto.

## 8. Comprobaciones rápidas desde la terminal

```powershell
# Token del estudiante demo (cliente kido-cli, solo para pruebas)
$t = (Invoke-RestMethod -Method Post http://localhost:19090/realms/kido/protocol/openid-connect/token `
  -Body @{grant_type='password'; client_id='kido-cli'; username='estudiante'; password='Estudiante123!'}).access_token

# Perfil y número de estudiante (debe decir id 1001)
Invoke-RestMethod http://localhost:18080/api/v1/usuarios/me -Headers @{Authorization="Bearer $t"}

# ¿Un correo está libre?
Invoke-RestMethod "http://localhost:18080/api/v1/usuarios/registro/disponibilidad?email=nuevo@correo.pe"

# Registro de estudiante por API (archivo registro.json con el cuerpo de abajo)
curl.exe -F "datos=@registro.json;type=application/json" http://localhost:18080/api/v1/usuarios/registro
```

`registro.json`:

```json
{ "tipo": "ESTUDIANTE", "nombres": "Ana", "apellidos": "Torres", "email": "ana.torres@correo.pe",
  "password": "Aprender2026", "aceptaTerminos": true, "docente": null }
```

Pruebas automáticas: `services/kido-usuario-ms` → `.\mvnw.cmd test`; `frontend` → `npm.cmd test`.

## 9. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| El login de Keycloak se ve con el diseño antiguo o sin estilos | El realm se importó antes de este cambio | Paso 4.2: `docker compose up -d --force-recreate kido-keycloak`. Recarga con Ctrl+F5. |
| «El servicio de cuentas no está disponible» / 503 al registrarse | `kido-usuario-ms` caído o aún iniciando | `docker compose logs kido-usuario-ms`. Revisa que esté en Eureka. |
| 503 «Keycloak rechazó al cliente kido-usuario-admin (HTTP 401)» | El secreto de Keycloak y el del servicio no coinciden | Mismo `KIDO_USUARIO_ADMIN_SECRET` para ambos; luego paso 4.2. O en la consola: Clients → `kido-usuario-admin` → Credentials → copia el secreto a `.env` y `docker compose up -d kido-usuario-ms`. |
| 503 «…no tiene permisos en Keycloak…» | La cuenta de servicio perdió sus roles | Consola → Clients → `kido-usuario-admin` → Service account roles → asigna `realm-management`: manage-users, view-users, query-users, view-realm. |
| «Ya existe una cuenta con ese correo» | El correo ya está en Keycloak | Ingresa con él o usa otro. |
| La contraseña no se acepta al registrarse | Política del realm | 8+ caracteres, mayúscula, minúscula y número; sin incluir tu correo. |
| Aprobé al docente pero sigue viendo el panel de estudiante | Su token aún no trae el rol | En `/postular-docente` pulsa **Activar mi panel docente**, o cierra sesión y vuelve a entrar. |
| «No pudimos cargar tu perfil» en Mi aprendizaje | `/me` no responde | Reintentar; mientras tanto el aviso permite un número manual (solo demo). |
| El PDF se rechaza | Pesa más de 5 MB o no es un PDF real | Exporta el documento a PDF; una imagen renombrada a `.pdf` no pasa. |
| Pantalla en blanco en `localhost:4200` | Keycloak no responde al iniciar | La app entra en modo público; levanta Keycloak y recarga. |
| Desde otra PC de la red no inicia sesión | `redirectUris` solo incluye localhost | Agrega `http://TU-IP:4200/*` al cliente `kido-frontend` en Keycloak. |

## 10. Qué cambió en esta versión

**Nuevo servicio `services/kido-usuario-ms`:** registro con compensación (si falla la base, se borra la cuenta creada en Keycloak), perfil `/me`, postulaciones docentes con PDF, bandeja de revisión, límite de registros por IP, validación real de PDF (firma `%PDF-`), mensajes de error por campo. Migraciones Flyway con perfiles demo y una postulación de muestra.

**Keycloak (`infra/keycloak`):**
- `kido-realm.json`: cliente confidencial `kido-usuario-admin` con cuenta de servicio y permisos mínimos; rol por defecto ESTUDIANTE; política de contraseñas; protección contra fuerza bruta; registro nativo desactivado (vive en la app); PKCE S256 en `kido-frontend`; cuentas demo con nombres reales y la nueva `postulante`.
- Tema `kido` reescrito sobre `base` (sin PatternFly): panel de marca, formulario limpio, «Ver/Ocultar» contraseña, enlaces a registro de estudiante y postulación docente, modo oscuro automático, textos en español e inglés.

**Infraestructura:** `compose.yml` (Postgres `15437`, servicio, secreto compartido), rutas y reglas del Gateway para `/api/v1/usuarios/**`, límites multipart del Gateway, Prometheus y `scripts/build-all.ps1`.

**Frontend:**
- `/login` rediseñado (marco de marca, ingreso, opciones de registro y cuentas demo con «Usar»).
- `/registro` con elección estudiante/docente y asistente de 3 pasos para docentes.
- `/postular-docente` con seguimiento del estado, corrección y activación del rol.
- Administración → **Docentes y cuentas** con bandeja, visor de PDF, lista de verificación y decisiones.
- Paneles sin «vincular número»: el id llega de `/me`. Logo KIDO nuevo (barra, pie, favicon y Keycloak).

## 11. Límites conocidos (para mencionarlos con honestidad)

- Los servicios de cursos, inscripciones y pagos todavía **confían en el número** (`estudianteId` / `docenteId`) que envía el frontend. Ahora ese número sale de `/me` y no se escribe a mano, pero el siguiente paso sería que cada servicio lo derive del JWT en el servidor.
- No hay verificación de correo por email (no hay SMTP configurado): las cuentas nacen con el correo marcado como verificado.
- El límite de registros por IP vive en memoria; con varias réplicas conviene moverlo al Gateway o a Redis.
- El PDF se guarda en PostgreSQL (suficiente para la demo; en producción iría a un almacenamiento de objetos).
