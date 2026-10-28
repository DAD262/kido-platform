# Lista de deseados

Los estudiantes pueden guardar cursos publicados desde el catálogo o el detalle y abrir su lista en `/deseados`. La lista se guarda en la base de datos de Usuario y se identifica mediante el `sub` del JWT de Keycloak.

## Configuración

El servicio Usuario consulta a Curso mediante `KIDO_CATALOGO_BASE_URL`. En Compose apunta a `http://kido-curso-1:8080`. Gateway, Usuario y Pago verifican el emisor del JWT con `KEYCLOAK_ISSUER_URI`; este valor debe coincidir exactamente con el `iss` del token. El valor de ejemplo es `http://localhost:19090/realms/kido`.

La migración `V4__lista_deseados.sql` crea la tabla al iniciar Usuario. No modifica las migraciones V1–V3 ni los datos de inscripción o pago.

## Verificación

1. Ejecuta `docker compose config --quiet` y levanta los servicios según el README.
2. Entra como estudiante, guarda un curso publicado y abre `/deseados`.
3. Recarga la página: el curso debe seguir en la lista. Quítalo y vuelve a añadirlo.
4. Comprueba que un usuario sin sesión recibe 401 y que una cuenta sin rol `ESTUDIANTE` recibe 403 al llamar a `/api/v1/usuarios/me/deseados`.

El servicio devuelve 404 para un curso inexistente, 409 para uno no publicado y 503 si Curso no está disponible.
