# Protección de la publicación — 30 de octubre de 2026

- El archivo `.env` privado no forma parte de la rama publicada. `.env.example` es una plantilla sin credenciales operativas.
- Se amplió `.gitignore` para excluir variantes de `.env`, claves privadas, almacenes de certificados, credenciales JSON y directorios de secretos. Respaldos y temporales permanecen excluidos.
- Se compararon los valores privados de contraseñas, secretos y tokens de `.env` de al menos ocho caracteres con los archivos del índice Git, sin imprimir sus valores: no hubo coincidencias.
- El cliente confidencial de Keycloak obtiene su secreto del entorno y Compose exige configurarlo explícitamente.
- Se retiraron las contraseñas de demostración del frontend. La importación del realm deja las tres cuentas humanas de demostración deshabilitadas y sin credenciales; la cuenta técnica conserva su configuración de servicio.
- Los puertos publicados por el Compose principal se vinculan exclusivamente a `127.0.0.1`.

## Instalaciones existentes

La importación de Keycloak no actualiza realms existentes. Deshabilitar o cambiar las contraseñas de las cuentas demo existentes desde la administración de Keycloak antes de compartir una instalación. Las contraseñas demo antiguas ya estaban en el historial público: retirarlas del código actual no las vuelve secretas. Esta actualización no modifica las cuentas ni las contraseñas de un realm en ejecución.

Para activar los enlaces a localhost en contenedores existentes, ejecutar `docker compose up -d`. No se deben publicar directamente las bases, Kafka ni las consolas administrativas.

Esta revisión cubre los archivos publicados, credenciales y configuración de exposición local; no certifica la ausencia de todas las vulnerabilidades de la aplicación. Los pendientes de autorización, despliegue y producción siguen en [el roadmap](../roadmap-produccion.md).
