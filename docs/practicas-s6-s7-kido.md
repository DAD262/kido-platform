# Prácticas S6 y S7 adaptadas a Kido

Esta implementación adapta las dos tareas de Sistemas Distribuidos al proyecto Kido existente.

## S6: Feign y Circuit Breaker

El flujo de pago consulta `kido-curso-ms` antes de guardar una orden. Esa comunicación ahora usa Spring Cloud OpenFeign con balanceo por Eureka y Circuit Breaker de Resilience4j. Se configuraron timeout de conexión de 1 s, lectura de 3 s, apertura al superar 60 % de fallos (ventana de 5 llamadas, mínimo 3) y espera abierta de 10 s antes de permitir llamadas de prueba.

- Si cursos no responde, la API devuelve HTTP 503 y no guarda una orden sin precio ni crea un pago ficticio.
- Si el curso no existe, se conserva el HTTP 404.
- Las llamadas de inscripción usan Feign; el flujo de aprobación conserva `inscripcionSincronizada=false` y permite reintentar la sincronización como ADMIN.
- Notificaciones usan Feign con fallback no bloqueante porque son accesorias después de aprobar un pago.
- Las conexiones Feign reenvían el JWT del usuario a otros servicios.

Los estados y eventos se pueden observar en `/actuator/circuitbreakers` y `/actuator/circuitbreakerevents` dentro del servicio. Actuator también publica el estado de salud `circuitBreakers`.

### Prueba manual de resiliencia

1. Inicia los servicios según `README.md`, copiando `.env.example` a `.env` y configurando `DB_PASS`.
2. Para hacer accesible el Resource Server de pagos solo en `localhost`, inicia Compose con ambos archivos: `docker compose -f compose.yml -f compose.security-test.yml up --build -d`.
3. Consigue un token ESTUDIANTE como en `scripts/demo-security.ps1`. Usa el endpoint de órdenes de pago con un curso publicado y válido: `POST http://localhost:18080/api/v1/pagos/ordenes` con `{"estudianteId":1,"cursoId":1}`.
4. Detén las instancias de cursos con `docker compose stop kido-curso-1 kido-curso-2` y repite la llamada de creación al menos tres veces. Se espera HTTP 503 y no debe aparecer una nueva orden persistida.
5. Con un token ADMIN, revisa `GET http://localhost:18083/actuator/circuitbreakers` y `GET http://localhost:18083/actuator/circuitbreakerevents`. El circuito de `KidoCursoObtener` pasa de CLOSED a OPEN después de alcanzar el umbral. Tras 10 s hará pruebas HALF_OPEN cuando lleguen llamadas.
6. Inicia cursos con `docker compose start kido-curso-1 kido-curso-2`; cuando las pruebas HALF_OPEN tengan éxito, el estado vuelve a CLOSED.

Los IDs `KidoCursoObtener`, `KidoInscripcionCrearCompra`, `KidoInscripcionBuscar`, `KidoInscripcionRevocar` y `KidoNotificacionEnviar` se fijan con `CircuitBreakerNameResolver` y se configuran iguales en DEV y PROD.

## S7: Seguridad distribuida

El Gateway ya valida JWT de Keycloak y roles del realm. El microservicio de pagos ahora también valida directamente la firma JWT usando el JWK Set URI de Keycloak y aplica los mismos roles. Así, enviar una solicitud directamente a pagos no salta sus controles. Las llamadas Feign retransmiten el JWT para que el servicio receptor pueda validar al usuario.

| Operación de pagos | Acceso |
| --- | --- |
| Crear órdenes y consultar órdenes propias | ESTUDIANTE o ADMIN |
| Saldo y órdenes de docente | DOCENTE o ADMIN |
| Simular aprobación, reintentar inscripción y liberar saldos | ADMIN |
| Retiros | DOCENTE crea/consulta; ADMIN actualiza |
| Reembolsos | ESTUDIANTE crea; ADMIN consulta/actualiza |
| Webhook Mercado Pago | Público; debe validarse por la integración de Mercado Pago |

Las rutas `/internal/v1/**` son endpoints de red privada que no publica el Gateway; permanecen sin autenticación de usuario para permitir comunicación entre servicios. Los accesos externos pasan por el Gateway. El paso siguiente de endurecimiento sería autenticación máquina a máquina con credenciales de servicio.

### Prueba de acceso directo

Con el override `compose.security-test.yml` y los servicios levantados, ejecuta en PowerShell:

```powershell
./scripts/demo-tarea-s6-s7.ps1
```

Debe mostrar 401 sin token, 401 con token inválido, 403 al consultar el saldo de docente como estudiante y 200 como docente. El override publica el servicio de pagos únicamente en `127.0.0.1:18083` para esa prueba.
