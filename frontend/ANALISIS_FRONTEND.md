# KIDO · Análisis del frontend y rediseño v3

Revisión del frontend Angular 20 (`frontend/`) y del tema de Keycloak (`infra/keycloak/themes/kido`).
Objetivo: presentación formal, minimalista e intuitiva, manteniendo intacto el flujo con Keycloak y los microservicios.

---

## 1. Falencias encontradas

### 1.1 Críticas (afectan el funcionamiento o la demo)

| # | Falencia | Dónde | Efecto |
|---|----------|-------|--------|
| C1 | Si Keycloak no responde, `keycloak.init()` rechaza la promesa dentro de `provideAppInitializer` y **la aplicación entera queda en blanco** (ni siquiera el catálogo público carga). | `app.config.ts` | Una demo falla por completo si el contenedor de Keycloak tarda en levantar. |
| C2 | El interceptor no maneja el fallo de `updateToken()`. Si el refresh token caducó, la petición termina en un error no controlado en lugar de pedir login otra vez. | `auth.interceptor.ts` | Sesiones largas (p. ej. durante la exposición) terminan en errores 401 confusos. |
| C3 | El guard de roles redirige al inicio **sin explicar nada** cuando el usuario no tiene el rol requerido. | `role.guard.ts` | El docente/evaluador cree que la ruta “no funciona”. |
| C4 | El test por defecto `app.spec.ts` espera el texto `Hello, frontend`, que no existe. `npm test` falla siempre. | `app.spec.ts` | Mala señal ante una revisión formal del repositorio. |
| C5 | No hay evidencia de que `npm run build` se haya ejecutado (la guía anterior lo reconoce). El presupuesto `anyComponentStyle` es de 8 kB y `login.scss` ocupaba ≈8,6 kB ya minificado → **superaba el límite de error, por lo que el build de producción fallaba**. | `angular.json`, `login.scss` | `ng build` con la configuración por defecto (producción) aborta. |

### 1.2 Diseño y experiencia de usuario

- **Exceso decorativo, contrario al minimalismo**: barra de anuncio superior, banners con degradado repetidos en cada panel, glifos `✦ ✳ ◎ ▣` y emojis (`👋 🌙 ☀️`) como iconos, sombras en todas las tarjetas, tres familias de degradados distintas.
- **Textos que hablan del proyecto y no del usuario**: “El rediseño mejora inicio, login, paneles y percepción profesional”, “Diseño renovado para pruebas…”, “3 espacios conectados · 4 servicios de negocio”, “Contenido publicado desde el backend real de KIDO”. En una presentación formal esto se lee como relleno.
- **Datos ficticios en el login** (“82 % progreso”, “8 lecciones”, “4 logros”) — contradice la propia regla del proyecto de no mostrar montos o métricas simuladas.
- **Doble pantalla de login**: `/login` era una portada de 549 líneas de estilos con dos botones que luego llevan a la pantalla real de Keycloak. Un paso extra sin valor.
- **Estados técnicos visibles al usuario**: `PENDIENTE_REVISION`, `PENDING`, `APPROVED`, `PAID`, `CUENTA_BANCARIA`… mezclando inglés y español.
- **Fechas en ISO crudo** (`2026-10-15T10:24:11`) y montos con `toFixed(2)` (falla si el backend envía `null`).
- **Editor del docente apretado**: el asistente de 5 pasos vivía en media columna junto a la lista de cursos; en pantallas medianas los pasos se volvían ilegibles.
- **El estudiante debe escribir su “ID numérico”** antes de ver cualquier cosa, presentado como un panel grande de “Modo demo”.
- **Sin búsqueda global ni atajos**: para llegar a un curso había que pasar siempre por el catálogo.
- **Filtros del catálogo no viven en la URL**: no se pueden compartir ni sobreviven a recargar.
- **Categoría como “ID”** en el formulario del docente (`1 · Tecnología`).
- **Tema oscuro con destello**: la página se pintaba en claro y luego cambiaba a oscuro.

### 1.3 Calidad de código

- **Plantillas y TS minificados en una sola línea** (`docente.ts`, `estudiante.html`, `admin.html` con líneas de 600+ caracteres). Ilegible para revisión y para el trabajo en equipo.
- **Código muerto**: `core/services/cursos.ts` (servicio vacío), `ejecutarCurso`, `cerrarSesion`, `subirMaterial` duplicado de `subirMaterialProgreso`.
- **URLs codificadas a mano** en varios sitios (`:19090`, `http://` + host para la consola de admin, `http://localhost:18080` en el interceptor).
- **Fuentes con `@import` en SCSS** (bloquea el render) y dos familias tipográficas muy parecidas (DM Sans + Plus Jakarta Sans).
- **`localStorage` sin `try/catch`**: en modo privado de algunos navegadores lanza excepción.
- **Diálogos sin cierre por Escape/clic fuera** (solo uno lo tenía) y pestañas sin roles ARIA en dos de los tres paneles.

### 1.4 Pendiente de backend (no se puede resolver solo desde el frontend)

- **Identidad Keycloak ↔ ID académico**: el backend usa IDs numéricos y Keycloak UUID. La solución correcta es que cada microservicio derive el usuario desde el JWT validado (claim `sub`) o que Keycloak emita un atributo `kido_id` mediante un *protocol mapper*. **Actualizado:** el nuevo `kido-usuario-ms` expone `GET /api/v1/usuarios/me`, que devuelve ese número para la sesión; la vinculación manual desapareció (queda solo como modo demo si el servicio no responde). El paso pendiente es que cada microservicio lo derive del JWT en el servidor.
- **No existe `GET /categorias`**; el formulario usa las dos categorías del seed (`V2__seed_cursos.sql`).

---

## 2. Qué se cambió (v3)

### Sistema de diseño
- Un solo acento (azul KIDO `#2F49E5`), fondo neutro, bordes de 1 px en lugar de sombras, radios con jerarquía (18 / 12 / 9 px).
- Tipografía: **Bricolage Grotesque** (titulares) + **Instrument Sans** (texto), cargadas con `<link>` y `preconnect`.
- Tokens claros/oscuros en `styles.scss`; el tema se aplica **antes del primer pintado** (script en `index.html`).
- Iconos SVG propios (`shared/icon`) en lugar de emojis y glifos.
- Portadas de curso **tipográficas**: la inicial del título en grande sobre un tono estable derivado del id. Sin imágenes falsas.

### Innovación con propósito
- **Paleta de comandos `Ctrl/⌘ + K`** (también con `/`): buscar cursos, ir a cualquier panel según el rol, cambiar tema, gestionar cuenta o cerrar sesión, todo con teclado.
- **Búsqueda como portada**: el inicio abre con “¿Qué quieres aprender hoy?” y un buscador grande que lleva al catálogo filtrado.
- **“Continúa donde lo dejaste”** en el panel del estudiante: un clic retoma el curso más avanzado sin terminar.
- **Editor del docente a pantalla completa** con pasos marcados como completados y la **estructura del curso siempre visible** al costado.
- **Bandeja de administración** con contadores en las pestañas y un resumen en lenguaje natural (“Tienes 3 solicitudes esperando una decisión”).

### Keycloak
- `/login` reducido a una tarjeta simple; si ya hay sesión, redirige al panel (o al `returnUrl`, validado contra una lista blanca para evitar *open redirects*).
- Mensajes claros cuando falta el rol o Keycloak no responde; la app sigue funcionando en modo público.
- Menú de cuenta con **“Mi cuenta y contraseña”** (`keycloak.accountManagement()`).
- Renovación silenciosa del token (`onTokenExpired`) y re-login si el refresh token caducó.
- Tema de login de Keycloak (`login.css`) alineado al nuevo diseño, con modo oscuro automático.

### Código
- Plantillas reescritas y formateadas (Prettier), lógica de negocio y llamadas a la API **sin cambios**.
- Pipes `estado`, `tono`, `soles` y `fecha` para estados legibles, montos en `S/` (Intl `es-PE`) y fechas cortas.
- Catálogo con filtros sincronizados en la URL (`/cursos?q=java&tipo=GRATUITO`).
- `app.spec.ts` con pruebas reales (creación de la app y traducción de estados).
- Eliminado código muerto; configuración de Keycloak centralizada en `core/config/keycloak.config.ts`.

---

## 3. Cómo verificar

```powershell
cd frontend
npm install
npm run build        # debe compilar sin errores
npm test             # pruebas de App y de estados
npm start            # http://localhost:4200
```

Si Keycloak ya estaba levantado, reinicia su contenedor para que tome el nuevo `login.css` (o limpia la caché de temas).

Recorrido sugerido para la presentación:
1. Inicio → escribir “java” en el buscador → catálogo filtrado.
2. `Ctrl + K` → escribir el nombre de un curso → Enter.
3. Iniciar sesión como **docente** → Nuevo curso → completar los 5 pasos → Enviar a revisión.
4. Iniciar sesión como **admin** (otra ventana o PC) → pestaña Cursos → Revisar → Aprobar y publicar.
5. Iniciar sesión como **estudiante** → inscribirse → “Continúa donde lo dejaste” → marcar lección.
6. Entrar como estudiante a `/admin` → mensaje explicando que falta el rol.
7. Cambiar a tema oscuro y recargar: no hay destello.
