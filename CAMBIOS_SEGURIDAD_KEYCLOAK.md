# Continuación: Seguridad Keycloak

Se agregó seguridad centralizada al Gateway sin alterar los cuatro microservicios de negocio.

- Keycloak en Docker con realm `kido` importado automáticamente.
- Roles `ADMIN`, `DOCENTE` y `ESTUDIANTE`.
- Usuarios académicos de demostración.
- Gateway como OAuth2 Resource Server JWT.
- Autorización de rutas por método HTTP y rol.
- Catálogo de cursos público y webhook de Mercado Pago público.
- Script `scripts/demo-security.ps1` para demostrar 200/401/403.
- Se conserva el arreglo del `RestClient` de `kido-pago-ms` que evita que Eureka pase por el LoadBalancer.
