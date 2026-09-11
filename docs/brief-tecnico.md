# Brief técnico de Kido

## Datos del equipo

| Dato | Contenido |
|---|---|
| Proyecto | Kido |
| Curso | Desarrollo de aplicaciones distribuidas |
| Docente | Sullon Macalupu Abel Angel |
| Integrante 1 | Yana Rojas Adan |
| Integrante 2 | Coila Mamani Jhoel Midwar |
| Carrera y ciclo | Sistemas, ciclo 5 |
| Presentación | 11/09/2026 |

## Problema

Muchos docentes venden cursos por WhatsApp, revisan pagos de manera manual y comparten enlaces desde distintos lugares. Esto dificulta controlar alumnos, pagos, acceso y progreso.

## Solución

Kido será una plataforma web distribuida donde un docente publica cursos gratuitos o pagados y un estudiante se inscribe, compra y revisa su avance. El administrador controla usuarios, cursos, pagos, retiros y reembolsos.

## Roles

- **Estudiante:** busca cursos, se inscribe gratis, compra y registra progreso.
- **Docente:** crea cursos, organiza módulos y lecciones, consulta ventas y solicita retiros.
- **Administrador:** revisa cursos, usuarios, pagos, retiros y reembolsos.

## Reglas funcionales acordadas

- Un curso `GRATUITO` tiene precio S/ 0.00 y concede acceso inmediato.
- Un curso `PAGO` tiene precio mayor que cero y concede acceso después de confirmar el pago.
- La simulación académica usa una comisión de plataforma del 10 %.
- El saldo del docente permanece pendiente 7 días y luego pasa a disponible.
- El retiro mínimo simulado es S/ 50.00 y el administrador registra el pago manual por cuenta bancaria, Yape o Plin.
- El reembolso puede solicitarse durante 7 días si el estudiante no superó el 20 % del curso.
- Cada movimiento de dinero tendrá un registro; el saldo no se cambiará manualmente sin historial.

Estas cantidades son reglas del prototipo académico. Más adelante podrán convertirse en parámetros de configuración sin cambiar el flujo principal.

## Alcance por unidad

| Unidad | Resultado esperado |
|---|---|
| Unidad 1 | Curso, Inscripción, PostgreSQL, Flyway, Config Server, Eureka, Gateway y balanceo |
| Continuación de Midwar | Pago y Notificación integrados al backend existente; comisión, saldo, retiro y reembolso |
| Unidad 2 compartida | Auth/JWT, Kafka y Resilience4j sobre los servicios ya existentes |
| Unidad 3 | Angular, observabilidad completa, pruebas y documentación final |

## Tecnología definida

Java 21, Spring Boot 4.0.7, Maven, Spring Web MVC, Spring Data JPA, PostgreSQL 16, Flyway, Validation, Lombok, MapStruct, Swagger/OpenAPI, Actuator, Spring Cloud Config, Eureka, Spring Cloud Gateway, Spring Cloud LoadBalancer, OpenFeign, Resilience4j, Kafka, Keycloak/JWT, Docker, Docker Compose, Angular, Prometheus, Grafana, Loki y MkDocs.

Tomcat no necesita instalarse por separado: Spring Web MVC lo incluye embebido dentro del JAR.
