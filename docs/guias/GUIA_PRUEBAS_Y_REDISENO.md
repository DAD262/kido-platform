# KIDO — Rediseño y pruebas de integración

Esta entrega continúa el proyecto KIDO basado en Angular, Spring Boot, PostgreSQL, Keycloak y Kafka. Es una actualización de interfaz y experiencia; no sustituye los microservicios ni sus reglas de negocio.

## Cambios integrados

- Inicio renovado, navegación más compacta y uniforme; acceso directo a Keycloak desde el botón superior.
- Ruta `/login` como portada de entrada y registro; el formulario real sigue en Keycloak, con tema visual adaptado.
- Ruta `/configuracion` con tema claro, oscuro o automático, densidad compacta y reducción de movimiento. La elección se guarda en el navegador.
- Administrador con bandeja de moderación, vista previa de módulos y lecciones, acceso a materiales, decisión de aprobar o pedir correcciones, búsqueda general y finanzas con filtros y resúmenes.
- Docente con edición guiada en cinco pasos: datos, módulos, lecciones, materiales y requisitos académicos/certificados. Mantiene llamadas API existentes y la subida real.
- Estudiante con indicadores más comprensibles, progreso visible y conexión numérica de prueba presentada explícitamente como demo.
- Catálogo con búsqueda, modalidad, categoría y orden por nombre/precio. Sin calificaciones inventadas ni cursos ficticios.
- Errores de conexión explicados de manera más clara. Si una API responde con HTML, se recomienda revisar proxy/Gateway en lugar de mostrar al usuario el error crudo de JSON.
- Config Server cambia su puerto publicado por defecto a `18889`, configurable con `KIDO_CONFIG_PORT`, evitando el conflicto previo de `18888`. El puerto interno sigue siendo `8888`.

## Ejecución en la PC principal (Windows)

Abre PowerShell en la raíz de `kido-platform`:

```powershell
Copy-Item .env.example .env
notepad .env
docker compose config --services
docker compose up -d --build
docker compose ps -a
```

Si `.env` ya existe, NO lo reemplaces: conserva la contraseña que corresponde a tus volúmenes PostgreSQL. Tampoco ejecutes `docker compose down -v` a menos que decidas borrar todas las bases y registros de la demo.

En otra terminal:

```powershell
cd frontend
npm.cmd install
npx.cmd ng serve --proxy-config proxy.conf.json
```

En esa misma PC entra a `http://localhost:4300`. Keycloak suele publicarse en `http://localhost:19090` y Gateway en `http://localhost:18080`, según `compose.yml`.

## Prueba con dos PC del mismo laboratorio

Las dos PC deben estar en la misma red y tener autorización para comunicarse. Solo la **PC A** necesita ejecutar Docker y Angular; la PC B accede mediante navegador.

1. En PC A identifica la IP LAN con `ipconfig` (por ejemplo `192.168.1.50`; úsala únicamente como ejemplo, no como dirección real).
2. Ejecuta en la PC A `frontend\iniciar-red-local.cmd`. Eso sirve Angular en `0.0.0.0:4300` **solo para desarrollo**. No lo expongas a Internet.
3. Desde la PC B comprueba `Test-NetConnection -ComputerName IP_DE_PC_A -Port 4300` y lo mismo con `-Port 19090`. Si falla, revisa Firewall de Windows y la red del laboratorio; pide autorización al administrador.
4. En Keycloak Admin de la PC A, dentro del realm `kido` y cliente `kido-frontend`, agrega el origen exacto `http://IP_DE_PC_A:4300` a **Web origins** y la ruta `http://IP_DE_PC_A:4300/*` a **Valid redirect URIs**. Conserva los valores de localhost para PC A. No uses `*` como acceso universal.
5. Desde PC B abre `http://IP_DE_PC_A:4300`. Angular usa su propio proxy de desarrollo para enviar `/api` al Gateway `localhost:18080` **desde la PC A**. Keycloak se resuelve con el hostname de la página (no `localhost` del cliente).
6. Inicia sesión en PC A como DOCENTE y en PC B como ADMIN; crea un curso y sube un PDF o imagen, envía a revisión y revisa desde la bandeja de moderación en PC B. Prueba la aprobación y vuelve a consultar el catálogo.
7. Si falla la autenticación entre equipos, verifica también el `issuer` de JWT y la configuración de los Resource Servers. Las configuraciones de tokens/servidores deben ser consistentes. No desactives la validación del JWT para “hacerlo funcionar”.

## Pruebas mínimas antes de integrar a GitHub

| Prueba | Resultado esperado |
|---|---|
| Login | Keycloak autentica y se navega al panel del rol |
| Registro | Keycloak crea cuenta según sus políticas; el rol docente debe aprobarse/asignarse según la política de KIDO |
| Curso DOCENTE | Se crea un borrador real; se agregan módulos, lecciones y materiales |
| Moderación ADMIN | Se inspecciona el curso completo antes de aprobar o pedir correcciones |
| Catálogo | Solo aparecen cursos publicados que devuelve la API |
| ESTUDIANTE | Una inscripción real muestra progreso, materiales y estado académico |
| Finanzas | Las órdenes y solicitudes vienen del backend; no son montos simulados de UI |
| Pago/Certificado | Se validan contra la pasarela sandbox y las reglas de negocio, sin cobros automáticos en el navegador |
| Temas | Claro/oscuro/automático persisten después de recargar |
| Dos equipos | Curso enviado desde una PC se puede revisar desde la otra |

## Pendientes conocidos: no confundir diseño con seguridad real

**Identidad numérica de prueba:** algunos endpoints existentes todavía usan IDs numéricos para docentes y estudiantes, mientras Keycloak autentica por UUID. Actualmente el navegador permite seleccionar un ID local para demostración, pero eso **no comprueba propiedad ni autorización del dato**. Antes de una prueba con cuentas reales o una publicación externa, el backend debe resolver el usuario académico a partir de un JWT validado y comprobar que solo accede a su información. No elimines esa comprobación ni la sustituyas con un campo oculto.

**Keycloak:** el registro público no debe conceder privilegios de docente ni administrador automáticamente. La asignación de roles está bajo políticas de identidad.

**Verificación automática:** se han comprobado sintácticamente archivos TypeScript; el build de Angular completo depende de `npm install` y no se pudo ejecutar en el entorno de entrega por falta de conexión al registro de npm. Ejecuta `npm.cmd install` y `npm.cmd run build` en tu PC antes del Pull Request, además de las pruebas de interfaz/API.

## Diagnóstico rápido

Desde la raíz en PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\diagnostico-local.ps1
```

Este diagnóstico solo lee estado de servicios y puertos; no reinicia nada ni borra datos.
