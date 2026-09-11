# Kido - avance de Adan para S5

Kido es una plataforma distribuida de cursos gratuitos y pagados. Este repositorio contiene el avance de **Yana Rojas Adan** para la Evaluación de la Unidad I: dos microservicios persistentes y la infraestructura compartida necesaria para configuración centralizada, descubrimiento, Gateway y balanceo.

## Qué funciona en este avance

| Componente | Tipo | Estado S5 |
|---|---|---|
| `kido-curso-ms` | Microservicio de Adan | CRUD REST completo de cursos, categorías, módulos y lecciones persistidos |
| `kido-inscripcion-ms` | Microservicio transaccional de Adan | Inscripción con cabecera y detalle de progreso por lección |
| `kido-config` | Infraestructura compartida | Configuración DEV y PROD fuera del código |
| `kido-eureka` | Infraestructura compartida | Registro y descubrimiento dinámico |
| `kido-gateway` | Infraestructura compartida | Punto único de acceso y balanceo `lb://` |

La instancia de `kido-curso-ms` devuelve el encabezado `X-Instance-ID`. Al ejecutar dos copias, las peticiones consecutivas mediante Gateway permiten demostrar el balanceo solicitado en S5.

## Microservicios del proyecto completo

| Microservicio | Responsable previsto | Momento |
|---|---|---|
| `kido-curso-ms` | Adan | Implementado en esta entrega |
| `kido-inscripcion-ms` | Adan | Implementado en esta entrega |
| `kido-pago-ms` | Midwar | Unidad 2; Mercado Pago, órdenes, comisión, retiros y reembolsos |
| `kido-notificacion-ms` | Midwar | Unidad 2; eventos y avisos |
| `kido-auth-ms` | Ambos | Unidad 2; Spring Security, JWT y roles |

El certificado digital se incorporará después del flujo de progreso completo. Puede vivir inicialmente dentro de Inscripciones o separarse como `kido-certificado-ms` si el docente aprueba ampliar el número de servicios.

## Requisitos

- Java 21.
- Docker Desktop con Docker Compose.
- Git.
- Puertos libres: 18080, 18761, 18888, 15432 y 15434.

## Arranque completo para S5

En PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env
docker compose up -d --build
docker compose ps
```

En `.env`, reemplaza el texto de ejemplo de `DB_PASS` por una contraseña local. Este archivo ya está ignorado por Git y no debe subirse.

Accesos:

- Gateway: <http://localhost:18080>
- Eureka: <http://localhost:18761>
- Config Server: <http://localhost:18888/kido-curso-ms/prod>
- Cursos por Gateway: <http://localhost:18080/api/v1/cursos>
- Inscripciones por Gateway: <http://localhost:18080/api/v1/inscripciones>

La primera construcción descarga dependencias y puede tardar varios minutos.

## Ejecución DEV como en las sesiones del docente

Primero crea `.env`, define la contraseña e inicia las dos bases:

```powershell
docker compose -f .\services\kido-curso-ms\compose-dev.yml --env-file .env up -d
docker compose -f .\services\kido-inscripcion-ms\compose-dev.yml --env-file .env up -d
$env:DB_USER="kido"
$env:DB_PASS="TU_CLAVE_LOCAL"
```

Luego abre seis terminales, entra en la carpeta indicada y ejecuta cada comando en este orden:

| Orden | Carpeta | Comando |
|---:|---|---|
| 1 | `infra/kido-config` | `.\mvnw.cmd spring-boot:run` |
| 2 | `infra/kido-eureka` | `.\mvnw.cmd spring-boot:run` |
| 3 | `services/kido-curso-ms` | `.\mvnw.cmd spring-boot:run` |
| 4 | `services/kido-curso-ms` | `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"` |
| 5 | `services/kido-inscripcion-ms` | `.\mvnw.cmd spring-boot:run` |
| 6 | `infra/kido-gateway` | `.\mvnw.cmd spring-boot:run` |

En DEV, Swagger queda disponible en `http://localhost:8080/swagger-ui.html` para Cursos y `http://localhost:8082/swagger-ui.html` para Inscripciones. El tráfico de la demostración debe entrar por Gateway en el puerto 18080.

## Prueba rápida

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-s5.ps1
```

El script muestra configuración centralizada, CRUD, error 400, dos instancias y balanceo por el encabezado `X-Instance-ID`.

## Detener el proyecto

```powershell
docker compose down
```

Para borrar también los datos de prueba:

```powershell
docker compose down -v
```

## Documentación

- [Brief técnico](docs/brief-tecnico.md)
- [Producto de Unidad 1](docs/u1-producto.md)
- [Guía para sustentar S5](docs/guia-s5.md)
- [Lista completa de microservicios](LISTA_MICROSERVICIOS.md)
- [Preparación y colaboración en GitHub](docs/github.md)
- [Trabajo que continúa Midwar](docs/continuacion-midwar.md)

## Decisión propia defendible

Pagatu vende productos físicos. Kido adapta el mismo patrón a cursos digitales: Curso reemplaza Producto e Inscripción con ProgresoLeccion reemplaza la operación transaccional de Orden con DetalleOrden. Cada microservicio conserva su propia base; los identificadores de curso y estudiante en Inscripciones no son claves foráneas hacia bases externas.
