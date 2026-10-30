# Actualización de KIDO — 30 de octubre de 2026

Esta entrega incorpora el avance local respecto de `main`, commit `7ae7104`. Conserva el historial del repositorio y publica los cambios en `actualizacion/kido-completo-2026-10-30`.

## 1. Carrito y compra conjunta

- Nueva página `/carrito`, acceso desde la navegación y acciones desde las tarjetas y el detalle del curso.
- Carrito persistente por estudiante en PostgreSQL, con un máximo de 20 cursos y una unidad por curso.
- Un solo checkout por el total: compra principal `CARRITO-{id}`, preferencia de Mercado Pago y órdenes individuales por curso.
- El servidor obtiene precios y condiciones del catálogo y comprueba que coincidan los cursos y el total revisados. Rechaza cursos gratuitos, retirados, no publicados o ya comprados.
- Los reintentos con el mismo carrito, precios, comprador y ambiente reutilizan la compra pendiente.
- Confirmación y webhook verifican estado, referencia, receptor, ambiente, moneda e importe contra el proveedor.
- Un evento por curso conserva la compra conjunta y la operación; las inscripciones siguen siendo idempotentes por orden.
- Distribución de costos y neto en céntimos, y reembolsos parciales por curso con referencia a la operación conjunta.

Detalles: [Carrito del estudiante](../guias/carrito-estudiante.md).

## 2. Pagos, retorno y ambientes

- Identificación explícita de operaciones `PRUEBAS`, `PRODUCCION` y `DESCONOCIDO`.
- Las operaciones de prueba y las históricas sin evidencia no generan saldos ni liquidaciones reales.
- Filtro de ambiente en Administración; los totales de ingresos y comisiones reales consideran producción.
- La aprobación simulada rechaza órdenes de producción.
- El retorno local conserva la página de resultado, permite abrir Mercado Pago y ofrece «Ya pagué: verificar mi compra».
- La URL del navegador no acredita un pago: la comprobación consulta al proveedor. Preparar un checkout no se presenta como una operación ya pagada.
- Se detectan operaciones aprobadas repetidas para revisión, manteniendo un único acceso académico por orden.
- Ajustes en conciliación, liquidaciones, retiros, reembolsos y eventos para conservar ambiente, receptor y compra conjunta.

## 3. Constancias de compra

- Nueva vista de constancia y descarga PDF, tanto individual como conjunta.
- Acceso limitado al comprador titular y a administradores autorizados.
- Datos conservados al emitir, detalle de cursos, docentes e importes, y paginación del PDF para compras extensas.
- Una compra conjunta muestra una operación y un total; las constancias individuales previas se mantienen.
- Fuentes locales para el PDF, con su licencia.
- Las constancias son informativas; la facturación electrónica sigue pendiente. Las pruebas se identifican como operaciones sin validez tributaria.

Detalles: [Retorno y constancias](../guias/retorno-y-constancias.md) y [facturación pendiente](../facturacion-electronica-pendiente.md).

## 4. Catálogo y contenido de programación

- 72 guías HTML originales para 12 rutas de programación, con explicación, ejemplo, ejercicio, criterios de comprobación y referencias oficiales.
- Temas: HTML/CSS, Git, JavaScript, TypeScript, Angular, SQL, Python básico, aplicado y automatización, Java inicial, orientación a objetos y Spring REST.
- Lecturas imprimibles y enlaces entre lecciones; son materiales de estudio y práctica.
- Generador reproducible `scripts/catalogo-programacion-2026.cjs` y verificador `scripts/verificar-guias-programacion.cjs`.
- El generador prepara SQL en `tmp`; los datos de una base local no se transfieren a GitHub ni se aplican automáticamente al clonar.
- Retirada lógica del catálogo con `retirado_catalogo`, conservando compras, inscripciones y consulta histórica por ID.
- Nuevas lecciones complementarias para inscripciones anteriores, sin alterar sus condiciones ni progreso.
- Consulta pública de docentes verificados y mejoras en portadas y nombres mostrados.
- Validación de enlaces de clase: guías locales conocidas o direcciones HTTP/HTTPS.

Detalles: [Catálogo de programación](../guias/catalogo-programacion.md).

## 5. Paneles y presentación

- Editor docente: selección de módulo normalizada, selección del módulo recién creado, estados de carga y guardado, límites de espera y conservación de los datos ante errores.
- Panel de aprendizaje: contenido complementario, acceso a guías y acciones de verificación de compras y constancias.
- Administración: filtros financieros por ambiente y verificación de compras.
- Ajustes visuales en carrito, checkout, resultado del pago, recuperación, registro, supervisión, certificados y liquidaciones.
- Tipografías servidas localmente con sus licencias y recursos visuales para los medios de pago.

## 6. Arranque en puertos separados

| Servicio | Puerto del equipo |
|---|---|
| Angular | 4300 |
| Gateway | 18080 |
| Keycloak | 19090 |
| Eureka | 18761 |
| Config Server | 18889 |
| Kafka / Kafka UI | 19092 / 18085 |
| Prometheus / Grafana | 19091 / 13000 |
| Mailpit | 18025 |
| PostgreSQL cursos | 15432 |
| PostgreSQL inscripciones | 15434 |
| PostgreSQL pagos | 15435 |
| PostgreSQL notificaciones | 15436 |
| PostgreSQL usuarios | 15437 |

Los microservicios usan sus redes y contenedores propios: pueden escuchar en 8080 internamente sin competir por un puerto del equipo. La base de Keycloak permanece interna.

`npm.cmd start` comprueba que 4300 esté libre y falla con un mensaje claro si está ocupado. Angular también declara ese puerto en su configuración. Se actualizaron URLs de retorno, recuperación, Keycloak, tema de acceso y utilidades locales. Los puertos del backend ya estaban separados en la base publicada.

Detalles: [Puertos locales](../guias/puertos-locales.md).

## 7. Persistencia y actualización

Nuevas migraciones Flyway:

- Cursos V6: retirada segura del catálogo.
- Pagos V7: ambientes de operaciones.
- Pagos V8: constancias de compra.
- Pagos V9: carrito del estudiante.
- Pagos V10: checkout conjunto.

Antes de actualizar una instalación con datos, conservar un respaldo y revisar estas migraciones. Para iniciar el código actualizado:

```powershell
docker compose up -d --build
cd frontend
npm.cmd ci
npm.cmd start
```

Usar `.env.example` como referencia; mantener las credenciales reales en `.env`. En un realm existente, revisar las URLs del cliente con `scripts/configurar-puerto-kido-local.ps1`. La importación inicial de Keycloak no actualiza por sí sola un realm existente.

`KIDO_USUARIO_ADMIN_SECRET` es obligatorio y carece de valor predeterminado en Compose y en el realm. Definir un secreto propio y mantenerlo sincronizado entre Keycloak y el servicio de usuarios. La plantilla `.env.example` lo deja vacío. Cambiar la variable no rota automáticamente un secreto de un realm ya existente.

## 8. Alcance de la documentación

Los roles, gestión de cuentas, supervisión, certificados académicos, comisiones, SAGA y servicios base ya existían en `main`. Esta entrega amplía esos módulos; las notas anteriores en `docs/cambios` conservan el detalle de su implementación.

No se incluyen dependencias instaladas, compilados, bases de datos, respaldos, logs, `.env` ni archivos temporales. La configuración local excepcional de checkout no se activa automáticamente al clonar. Los requisitos de producción y la facturación electrónica continúan documentados como pendientes.

## 9. Validación de esta entrega

- Compilación de producción Angular: correcta; tamaño inicial 407.96 kB.
- Verificador de contenido: 72 guías completas y 432 enlaces entre lecciones comprobados.
- Frontend: 61 pruebas correctas en Chrome Headless.
- Backend: 185 pruebas correctas (124 de pagos, 3 de cursos, 21 de inscripciones, 36 de usuarios y 1 del Gateway). Notificaciones completó la fase Maven de pruebas; ese módulo no contiene pruebas automatizadas.
- Arranque local: frontend, API, Keycloak y proxy de Angular respondieron HTTP 200.
- Configuración Docker Compose validada después de eliminar el secreto predeterminado del cliente de usuarios.
- Persisten avisos no bloqueantes: import de `RouterLink` no utilizado en Docente y presupuestos de estilos en Carrito, Estudiante y Registro.

Las pruebas automatizadas no sustituyen una validación comercial completa de Mercado Pago en un entorno configurado. No se realizaron nuevos cobros para preparar esta rama.
