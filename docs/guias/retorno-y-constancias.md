# Retorno y constancias de compra de KIDO

## Compras en desarrollo local

Para habilitar el checkout local con una cuenta de prueba verificada, definir en el `.env` privado `KIDO_CHECKOUT_PRUEBA_LOCAL=true` y `KIDO_ENTORNO_DESARROLLO_LOCAL=true`, manteniendo `MERCADO_PAGO_AMBIENTE=pruebas`. El Compose principal transmite estas opciones al servicio; ambas quedan desactivadas por defecto. Ejecutar `docker compose up -d --no-deps kido-pago-ms` después de modificarlas. No publicar `.env` ni credenciales. Si el Gateway aún conserva una dirección de la instancia anterior, esperar la actualización del registro o reiniciar `kido-gateway`.

Si no está configurado un retorno HTTPS público, KIDO conserva la pantalla de resultado y ofrece un enlace para abrir Mercado Pago en otra pestaña. Al terminar, vuelve a la pestaña de KIDO y usa «Ya pagué: verificar mi compra». La verificación consulta el proveedor: no usa los parámetros de URL como prueba de pago y no realiza otro cobro.

El carrito ofrece un único botón «Pagar» por el total de todos sus cursos. La preferencia incluye el detalle y una sola operación, con referencia CARRITO. La aceptación sigue siendo obligatoria. Antes de abrir el proveedor, la pantalla local muestra «Tu checkout está listo» y «Ir a pagar en Mercado Pago». Consulta la orden autenticada, sin buscar un pago inexistente ni presentarlo como pendiente de acreditación. La comprobación contra Mercado Pago se realiza cuando el estudiante indica que ya pagó o regresa con un identificador de operación.

En «Mi aprendizaje → Compras» también están «Verificar mi pago» y «Ver constancia». Administración muestra todos los ambientes, con filtros; sus totales de ingresos y comisiones reales incluyen únicamente PRODUCCION. Los registros históricos sin evidencia conservan DESCONOCIDO.

La constancia KIDO contiene los datos disponibles del comprador, orden y operación. Tiene vista web y PDF; solo acceden el comprador titular y administradores autorizados. Los datos se conservan al emitirla. Varias operaciones acreditadas para una misma orden tienen constancias separadas, y habilitan un solo acceso. Los cobros repetidos requieren revisión y no se suman como nuevas ventas de esa orden.

## Regreso desde la página de Mercado Pago

El botón de la página alojada por Mercado Pago se controla mediante back_urls, no mediante cambios en el HTML de KIDO. No hay un dominio público asignado por este proyecto.

Cuando se disponga de una URL HTTPS de desarrollo real:

1. Publicar mediante un proxy de acceso limitado la aplicación y solo las rutas de API necesarias para el comprador autenticado. Bloquear /admin, /api/v1/pagos/admin, /api/v1/usuarios/admin, /actuator, /internal, Eureka, Config Server, bases de datos, Kafka y consolas administrativas. No exponer directamente Docker ni el Gateway completo.
2. Configurar MERCADO_PAGO_RETURN_URL con la dirección HTTPS real y la ruta /pago/resultado. No introducir una dirección ficticia ni compartir el token.
3. Configurar MERCADO_PAGO_AUTO_RETURN=true. El cliente backend enviará auto_return=approved únicamente con retorno HTTPS válido y utilizará ese destino para éxito, pendiente y error.
4. Autorizar el origen y la URL de retorno exactos en el cliente de autenticación de desarrollo; si el acceso se realiza fuera de esta PC, publicar solo los endpoints de autenticación necesarios y configurar correctamente el emisor, conservando bloqueada la administración de Keycloak.
5. Recrear el servicio local de pagos para cargar la configuración. Las preferencias antiguas no se actualizan automáticamente. No crear otro pago para comprobar el retorno de uno ya acreditado: el estado y la constancia siguen disponibles desde KIDO.

La excepción comercial requiere KIDO_CHECKOUT_PRUEBA_LOCAL=true, KIDO_ENTORNO_DESARROLLO_LOCAL=true, ambiente pruebas y cuenta de prueba verificada. Sus valores por defecto siguen siendo false; producción conserva los requisitos comerciales y de seguridad.

## Situación tributaria

La constancia es informativa: no es boleta ni factura SUNAT. Las pruebas llevan «OPERACIÓN DE PRUEBA — SIN VALIDEZ TRIBUTARIA». No se calcula ni inventa IGV mientras no estén configurados el emisor y el tratamiento tributario correspondiente. Tampoco se expone al comprador la comisión interna de KIDO. Véase facturacion-electronica-pendiente.md para los requisitos previos a una futura integración tributaria.
