# Configuración comercial y cobros para la exposición

## Qué quedó implementado

- Administración > Finanzas > Pagos e información comercial permite guardar borradores y publicar los datos del vendedor y sus documentos.
- La publicación exige nombre/razón social, RUC de 11 dígitos, domicilio, correo de atención, condiciones de compra, privacidad, reembolsos y enlace HTTPS al Libro de Reclamaciones.
- Los borradores no se muestran al comprador. La aplicación valida formato y campos completos, no titularidad ni estado del RUC ni la adecuación jurídica de los textos.
- Página pública /condiciones-compra y enlaces en el pie de página. El Libro aparece cuando hay información publicada.
- El catálogo de medios se consulta con el token del servidor a GET /v1/payment_methods. Se muestran únicamente medios activos e integrados: tarjetas y, si el proveedor los devuelve, Yape, PagoEfectivo o saldo de Mercado Pago. La selección definitiva ocurre en Checkout Pro; no se anuncian Plin, Tunki, Apple Pay u otras opciones copiadas de otro comercio.
- Solo ADMIN puede consultar/editar la configuración administrativa. La información publicada y el estado de medios son públicos.
- Cursos y certificados requieren aceptación explícita de la versión comercial vigente. El servidor guarda sujeto autenticado, estudiante, orden, fecha, versión y copia de los textos aceptados. No se acepta una versión anterior ni una casilla omitida.
- En producción se exige credencial no TEST, retorno HTTPS, webhook HTTPS, firma y publicación comercial. Una variable de ambiente por sí sola no activa cobros.
- No se modificó el ambiente local a producción. No se inventaron datos del vendedor ni documentos legales.

## Requisitos externos que siguen pendientes

1. El propietario debe crear y habilitar su cuenta comercial de Mercado Pago y su aplicación.
2. Completar los datos reales y aprobar los documentos para su operación en Administración > Finanzas. Publicar textos no equivale a certificar cumplimiento de todas las obligaciones legales.
3. Tener un dominio con HTTPS y configurar el retorno y el webhook públicos, limitando la exposición a la aplicación y las rutas necesarias. Keycloak y las bases de datos no deben publicarse para la exposición.
4. Configurar las credenciales de producción y la firma en el servidor; nunca en el frontend ni en el chat. Seleccionar el ambiente de producción únicamente después de completar los pasos anteriores.
5. Definir la emisión de boletas/facturas y la atención del Libro de Reclamaciones. KIDO no tiene integración tributaria SUNAT ni crea automáticamente un Libro operativo: se publica el enlace del vendedor.
6. Definir las condiciones entre KIDO y sus docentes, comisiones, plazos de disponibilidad y retiros. El saldo contable del docente no es una transferencia bancaria automática.

La revisión automática rechazó activar producción anticipadamente mientras faltaban cuenta, credenciales y HTTPS. Se continuó con una alternativa segura: implementar la configuración y los controles sin activar cobros.

## Validación antes de la exposición

Con la configuración real completa, verificar desde dos cuentas de estudiante distintas: precio y documentos visibles, aceptación, redirección al proveedor, pago real de importe pequeño admitido por el proveedor, retorno, aprobación verificable, inscripción única y acceso al curso. Probar asimismo pago rechazado/pendiente, notificación repetida, cierre del navegador después de pagar y conciliación de saldo. Emitir el comprobante aplicable y verificar el proceso de retiro del docente y de reembolso. Este circuito externo no se ha ejecutado: faltan cuenta y credenciales.

## Fuentes oficiales consultadas

- [Medios de pago de Mercado Pago](https://www.mercadopago.com.pe/developers/es/reference/online-payments/checkout-api/payment-methods/get).
- [Libro de Reclamaciones - Indecopi](https://consumidor.gob.pe/libro-de-reclamaciones/).
- [Comprobantes de pago electrónicos - SUNAT](https://orientacion.sunat.gob.pe/comprobantes-de-pago-electronicos-1).
- [Emitir boleta de venta electrónica - SUNAT](https://www.gob.pe/7334-emitir-boleta-de-venta-electronica).

## Pruebas

41 pruebas del backend y 13 de Angular aprobadas. Incluyen filtro de medios activos, publicación incompleta, borradores privados, aceptación explícita, rechazo de versión anterior, copia de textos y controles de activación de producción. Las pruebas de proveedor usan respuestas HTTP controladas; no representan un pago real.
