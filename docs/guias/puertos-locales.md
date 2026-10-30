# Direcciones locales separadas

| Aplicación | Dirección |
|---|---|
| Kido | http://localhost:4300 |
| ContaCore | http://localhost:4200 |
| API de Kido | http://localhost:18080 |
| Identidad de Kido | http://localhost:19090 |

Desde `frontend`, ejecuta `npm.cmd start`. El lanzador verifica que 4300 esté libre y termina con un error claro si está ocupado; no acepta migrar automáticamente a otro puerto. `ng serve` directo también tiene 4300 configurado en angular.json.

La configuración local de Kido, el tema de acceso, la recuperación de contraseñas y el retorno local de pagos usan 4300. Para un realm Keycloak ya importado, ejecuta `scripts/configurar-puerto-kido-local.ps1`: solo actualiza URLs del cliente `kido-frontend`. Importar el JSON actualizado no modifica automáticamente un realm existente.

En este entorno el cliente existente se migró con Keycloak detenido mediante `scripts/migrar-url-keycloak-kido-local.sql`, conservando sus usuarios y permisos. La copia previa contiene únicamente URLs en `tmp/keycloak-urls-antes-4300.json`. El retorno de autenticación a 4300 se verificó por HTTP; el de 4200 es rechazado.

No abras Kido mediante 4200: ese puerto pertenece a ContaCore. Ningún proceso o archivo del proyecto ContaCore se modifica para iniciar Kido.

El retorno automático de Mercado Pago sigue requiriendo una URL HTTPS pública de desarrollo explícita. El puerto local no satisface ese requisito ni habilita producción.
