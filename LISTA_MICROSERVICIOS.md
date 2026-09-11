# Lista de microservicios de Kido

## Programados en el avance de Adan

| Componente | Función sencilla | Datos propios | Estado |
|---|---|---|---|
| `kido-curso-ms` | Administra cursos gratuitos y pagados, categorías, módulos y lecciones | PostgreSQL `kido_curso_db` | Implementado |
| `kido-inscripcion-ms` | Registra alumnos, acceso al curso y progreso por lección | PostgreSQL `kido_inscripcion_db` | Implementado |
| `kido-config` | Entrega la configuración DEV y PROD | Archivos YAML | Implementado |
| `kido-eureka` | Registra las instancias disponibles | No aplica | Implementado |
| `kido-gateway` | Recibe las peticiones y las dirige al servicio correcto | No aplica | Implementado |

Los tres últimos componentes son infraestructura. Ayudan a los microservicios, pero no representan funciones del negocio.

## Microservicios que completará Midwar

| Microservicio | Función | Datos principales |
|---|---|---|
| `kido-pago-ms` | Compras, pagos, comisión, saldo, retiro y reembolso | orden de compra, pago, movimiento de saldo, retiro y reembolso |
| `kido-notificacion-ms` | Avisos internos y correos | notificación, plantilla e intento de envío |

## Trabajo compartido de ambos integrantes

| Microservicio | Función | Datos principales |
|---|---|---|
| `kido-auth-ms` | Registro, inicio de sesión, JWT y roles ADMIN, DOCENTE y ESTUDIANTE | usuario, rol y perfil |

También será compartida la integración con Kafka, OpenFeign, Resilience4j, Keycloak/JWT, Prometheus, Grafana, Loki, Angular y la documentación final.

## Regla para no romper el avance

Midwar debe agregar sus carpetas dentro de `services`, crear sus archivos DEV y PROD en `infra/kido-config/config-repo` y agregar rutas `lb://` en Gateway. Cada servicio debe conservar su propia base de datos. Las compras aprobadas crearán inscripciones por evento; el endpoint público de Inscripciones no acepta accesos `COMPRA` sin pago.
