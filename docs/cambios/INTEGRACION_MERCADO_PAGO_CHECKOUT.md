# Mercado Pago en KIDO

## Implementado

- Método de pago Mercado Pago (Checkout Pro), con disponibilidad informada por el backend autenticado en GET /api/v1/pagos/metodos. Sin Access Token, la interfaz explica que está pendiente de habilitación y no inicia compras.
- Orden calculada por el servidor desde el curso publicado, en soles (PEN). Se reutiliza la orden pendiente del estudiante para ese curso.
- Redirección al checkout del proveedor; KIDO no pide ni almacena datos de tarjeta.
- Confirmación contra la API de Mercado Pago: estado approved, referencia de orden, importe completo y moneda PEN. El retorno del navegador no acredita por sí solo un pago.
- Comprobación del propietario en creación, consulta, inicio y confirmación de órdenes. La identidad del perfil se obtiene usando el token autenticado, no el ID del navegador.
- Corrección del límite por defecto de un segundo que interrumpía la consulta de cursos: Feign conecta en hasta 2 segundos y espera la lectura hasta 5; el TimeLimiter exterior permite hasta 8 segundos.
- Notificaciones con firma incorrecta o ausente responden 401 cuando hay una clave de webhook configurada.
- Aprobación simulada desactivada en este entorno local.

## Pendiente de la cuenta de Mercado Pago

Todavía no existe una aplicación de Mercado Pago del propietario de KIDO ni credenciales configuradas. Por eso no se ha creado una preferencia real ni se ha efectuado un pago en el checkout externo. Las pruebas de API del proveedor usan respuestas HTTP controladas, no dinero real.

1. Crear una aplicación en [Tus integraciones](https://www.mercadopago.com.pe/developers/panel/app), para Perú y Checkout Pro.
2. Preparar las credenciales y las cuentas de comprador/vendedor de prueba siguiendo la [documentación oficial](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/overview).
3. Desde PowerShell, ejecutar:

~~~powershell
.\scripts\configurar-mercado-pago-local.ps1
docker compose up -d --no-deps kido-pago-ms
~~~

El configurador oculta las credenciales al escribir y las guarda solo en .env, excluido de Git. No ejecuta cobros. Usar únicamente credenciales de prueba, nunca las de un vendedor real en esta etapa. El prefijo APP_USR por sí solo no prueba que una cuenta sea de prueba.

Para recibir notificaciones hace falta una URL HTTPS pública que apunte exclusivamente al webhook del gateway y su clave de firma. La configuración de red pública no se ha realizado. La confirmación al volver al sitio es útil para las pruebas locales, pero el webhook será necesario para confirmar aunque se cierre el navegador.

## Validación

- 30 pruebas de backend: contratos HTTP de preferencias/pagos, autenticación y propietario, confirmación idempotente, moneda, importe, referencias, firma y Saga existente.
- Compilación Angular correcta.
- Verificación local: ocho intentos consecutivos de creación recuperan la misma orden pendiente; IDs ajenos reciben 403; sin credenciales no se aprueba ni se inicia ningún pago.
- Interfaz de compra revisada para visitante y estudiante, escritorio y móvil.

## Prueba pendiente al activar las credenciales

Crear la preferencia real; pagar con un comprador y medio de prueba; verificar aprobación, inscripción única y acceso al curso. Repetir con pago rechazado y pendiente, y probar el webhook firmado y sus reintentos. Estas pruebas requieren la aplicación externa y no se consideran completadas.
