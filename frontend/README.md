# KIDO · Frontend

Angular 20 (componentes standalone y señales) + Keycloak 26 (`keycloak-js`, PKCE) contra el API Gateway.
La guía completa para levantar todo el proyecto está en [`../docs/guias/GUIA_LEVANTAR_KIDO.md`](../docs/guias/GUIA_LEVANTAR_KIDO.md).

## Ejecutar

```powershell
cd frontend
npm.cmd install
npm.cmd start        # http://localhost:4300 (proxy /api → http://localhost:18080)
```

`npm.cmd run build` genera `dist/`. `npm.cmd test` ejecuta las pruebas unitarias.

## Acceso y cuentas (separado por responsabilidades)

| Ruta | Qué hace | Quién la resuelve |
|---|---|---|
| `/login` | Elige cómo entrar: ingresar, crear cuenta o usar una cuenta demo | Angular |
| (Keycloak) | Formulario de correo y contraseña con el tema `kido` | Keycloak `:19090` |
| `/registro` | Elegir **estudiante** (1 paso) o **docente** (3 pasos: cuenta, perfil profesional, verificación con PDF) | Angular → `kido-usuario-ms` |
| `/postular-docente` | Un estudiante con cuenta postula, ve el estado o corrige lo observado | Angular → `kido-usuario-ms` |
| `/admin` → «Docentes y cuentas» | Aprobar, pedir corrección o rechazar postulaciones (con visor del PDF) | Angular → `kido-usuario-ms` → Keycloak |

El número de estudiante / docente ya no se escribe a mano: `AccountContext` lo obtiene de `GET /api/v1/usuarios/me`.

## Estructura

```
src/app/
  core/
    config/keycloak.config.ts   realm, cliente y URL de Keycloak
    services/auth.ts            sesión, roles, login(returnUrl, loginHint), refrescarSesion()
    services/account-context.ts perfil KIDO (/me) e id numérico de la sesión
    services/api.ts             rutas del Gateway (incluye registro y verificación docente)
    utils/registro.ts           reglas del registro (iguales a las del backend y del realm)
    guards/role.guard.ts        roleGuard(rol) y sesionGuard
  shared/
    auth-shell/                 marco visual de /login y /registro
    docente-form/               formulario de verificación docente (registro y postulación)
    logo/                       marca KIDO (también en favicon.svg y en el tema de Keycloak)
    perfil-estado/              aviso si el perfil no carga (con reintento y modo demo)
  pages/
    login, registro, postular-docente, admin/solicitudes-docentes,
    home, catalogo, curso-detalle, estudiante, docente, admin, pago-resultado, preferencias
```
