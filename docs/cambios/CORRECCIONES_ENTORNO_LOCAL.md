# Correcciones del entorno local — 27 de octubre de 2026

Se corrigió la mezcla de la copia anterior D:\KIDO 2 con esta carpeta:

- Frontend actual en http://localhost:4200, compatible con las redirecciones de Keycloak.
- Config Server y servicios reconstruidos con el código de esta carpeta; Gateway incluye /api/v1/usuarios/**.
- Enlaces de los tres perfiles demo actualizados al realm vigente, manteniendo los ids 1001, 1 y 900. El respaldo de los enlaces anteriores se guarda en el directorio temporal de Windows.
- Credenciales de las bases de negocio sincronizadas con .env sin borrar tablas.
- Migraciones históricas de pagos restauradas a sus versiones reales: V2 orden_lecciones_snapshot, V3 pagos_certificados, V4 snapshot_curso_orden_saga. No se desactivó la validación ni se modificó el historial Flyway.
- Keycloak conserva su PostgreSQL y volumen kido_keycloak_postgres_data en Compose. La contraseña se mantiene únicamente en .env.
- Clientes kido-cli, kido-frontend y kido-usuario-admin sincronizados. La cuenta de servicio tiene manage-users, view-users, query-users y view-realm.
- La recuperación administrativa usa el comando oficial bootstrap-admin service y elimina los accesos temporales al terminar. No restablece las contraseñas de los usuarios.

Comprobar acceso, perfiles, deseados, aprendizaje, pagos, notificaciones y separación de roles:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verificar-acceso-local.ps1
```

Herramientas de reparación, solo cuando sean necesarias:

- scripts/sincronizar-keycloak-local.ps1: actualiza clientes y permisos, conservando usuarios. Acepta AdminUser/AdminPassword o AccessToken.
- scripts/recuperar-keycloak-local.ps1: recuperación local cuando no se conoce la clave administrativa; interrumpe brevemente Keycloak y elimina el acceso temporal al finalizar. Si falla, conserva la credencial cifrada con DPAPI en el directorio temporal para poder completar la limpieza.
- scripts/reparar-enlaces-demo.ps1: vuelve a enlazar exclusivamente las tres cuentas demo tras cambiar el realm; valida sus inicios de sesión y respalda los enlaces anteriores.

No recrear ni borrar volúmenes para resolver errores de inicio. Un realm existente no se vuelve a importar al recrear el contenedor.
