# Cobros y liquidaciones docentes de KIDO

## Reparto de cada venta

Las órdenes nuevas guardan el 20 % bruto de KIDO y el 80 % del docente. Se redondea la comisión a dos decimales y el docente recibe la diferencia, para conservar exactamente el importe total. Las órdenes anteriores, incluidas las pendientes, conservan el reparto con el que se crearon: la configuración nueva no modifica importes históricos.

El costo del proveedor no se descuenta del monto docente. Mercado Pago informa el costo cargado al vendedor y el neto recibido; ambos se guardan aparte. Las comisiones que paga el comprador por financiación no se suman al costo de KIDO. Un costo desconocido aparece pendiente de conciliación, nunca se inventa como cero.

Ejemplo ilustrativo, no tarifa del proveedor: de S/100, el docente conserva S/80 y la comisión bruta KIDO es S/20. Si el neto informado por el proveedor fuera S/95, el margen de ese cobro sería S/15 antes de otros gastos. El margen no se muestra como ingreso vigente si la venta fue reembolsada o sufrió contracargo.

## Dinero aprobado y dinero disponible

La inscripción se activa con el pago aprobado, su referencia, importe y moneda verificados. El crédito del docente permanece pendiente hasta que el pago real esté conciliado, Mercado Pago indique fondos liberados y se cumpla la retención local configurada (actualmente 7 días). La fecha de liberación del proveedor también se respeta.

La conciliación se actualiza con los webhooks y con una tarea cada cinco minutos, en lotes de hasta 100 órdenes de curso y 100 certificados. Los errores se reintentan; no se sustituyen por aprobaciones. No basta que transcurran siete días para declarar que existe dinero disponible.

Las ventas SIM del historial se conservan y se identifican, pero no son fondos reales ni generan liquidaciones. Al incorporar esta función no había cobros externos ni retiros históricos en esta base. No se borraron compras ni inscripciones.

## Calendario quincenal

Los periodos son del 1 al 15 y del 16 al último día del mes. La preparación automática se ejecuta a las 00:10, hora de Lima, del 16 y del 1 del mes siguiente. Administración también puede generar un corte cerrado desde Finanzas.

La liquidación toma el saldo conciliado elegible acumulado al corte, incluidos saldos anteriores no pagados, y descuenta todas las reservas y pagos existentes. Guarda los movimientos usados como fotografía del cálculo. No limita artificialmente el pago al ingreso del periodo si hay saldos pendientes anteriores.

Una reserva negativa en el libro de saldos impide volver a comprometer el mismo dinero. Hay bloqueo transaccional por docente, cortes únicos mientras la liquidación esté pendiente o pagada, y restricciones de unicidad en la base de datos. Repetir Generar no duplica una quincena. Las nuevas solicitudes de retiro individual quedan sustituidas por estas liquidaciones.

## Transferencia y constancia

Generar una liquidación no mueve dinero. El administrador realiza la transferencia desde su banco, verifica que sea exitosa y registra banco, código de operación, fecha, titular, destino, importe exacto y constancia PDF/PNG/JPG de hasta 5 MB. Debe confirmar expresamente que comprobó la operación.

El estado PAGADA solo se registra con esos datos. El archivo se valida por tipo y firma de contenido, se guarda de forma privada en una tabla separada y se calcula su SHA-256. Se impide reutilizar el código de una transferencia del mismo banco/fecha o una constancia de otra liquidación. Reintentar la misma operación no vuelve a descontar el saldo. El servidor no comprueba la transferencia con una API bancaria: la verificación sigue siendo responsabilidad del administrador.

Solo ADMIN genera, anula o registra pagos. Cada docente consulta únicamente sus liquidaciones y descarga sus propias constancias. Supervisores y estudiantes no tienen acceso financiero. Las liquidaciones pagadas no pueden anularse ni reemplazar su comprobante.

## Reembolsos y contracargos

Una solicitud de reembolso bloquea el crédito afectado y anula las liquidaciones pendientes del docente para recalcularlas. Una disputa o contracargo informado por el proveedor también retiene o revierte el crédito y anula reservas pendientes.

Si la transferencia ya se registró como pagada, el pago histórico se conserva. La reversión de la venta produce un saldo deudor que debe cubrirse antes de otra liquidación; no se realiza un débito en la cuenta bancaria del docente. Esta política debe quedar acordada en los contratos y condiciones de la plataforma. Las revocaciones académicas por devoluciones o contracargos de compras se reintentan sin deshacer la retención financiera cuando el servicio académico está caído.

## Paneles y API

Administración > Finanzas: liquidaciones por corte, fotografía del cálculo, registro verificado de transferencia, constancia, anulación de pendientes, reparto de cada orden, costo y neto del proveedor y margen del cobro.

Docente > Ingresos: participación vigente, saldo conciliado, historial de liquidaciones, detalle y constancias. Las ventas históricas conservan sus importes originales.

- GET /api/v1/pagos/liquidaciones/politica
- POST /api/v1/pagos/liquidaciones/generar
- GET /api/v1/pagos/liquidaciones (ADMIN)
- GET /api/v1/pagos/liquidaciones/docente/{id}
- GET /api/v1/pagos/liquidaciones/{id}/detalle
- POST /api/v1/pagos/liquidaciones/{id}/pagar (multipart: datos JSON y constancia)
- POST /api/v1/pagos/liquidaciones/{id}/anular
- GET /api/v1/pagos/liquidaciones/{id}/constancia

## Activación comercial pendiente

La implementación no confirma que Mercado Pago autorice concentrar cobros de varios docentes en la cuenta de KIDO y liquidarlos posteriormente. El propietario debe obtener esa conformidad para su cuenta en Perú. En producción se exige la confirmación explícita del modelo de recaudación, además de las credenciales, HTTPS, webhook firmado e información comercial publicada.

MERCADO_PAGO_MODELO_RECAUDACION_CONFIRMADO permanece false por defecto. No se activó producción, no se conectó una cuenta bancaria ni se ejecutaron pagos o devoluciones reales durante el desarrollo. La transferencia al docente no usa Split ni una API de desembolsos de Mercado Pago.

## Fuentes oficiales

- [Split de Pagos: modelo diferente de distribución inmediata con OAuth por vendedor](https://www.mercadopago.com.pe/developers/es/docs/split-payments/split-1-1/integration-configuration/integrate-marketplace).
- [Campos de pagos y costos del proveedor](https://www.mercadopago.com.pe/developers/en/docs/resources-pci-compliant-merchants/unified-post).
- [Respuesta de pagos en Perú y estado de liberación](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-payments/integration-configuration/paycash).

## Validación

68 pruebas de backend y 18 de interfaz: reparto y redondeo, conservación histórica, costos del pagador y vendedor, fondos pendientes/liberados, disputas, contracargos, reservas anteriores, cortes cerrados y años bisiestos, solicitudes repetidas, importes incorrectos, constancias inválidas, transferencia duplicada, pago idempotente y anulación. Las pruebas de banco y proveedor usan respuestas controladas y archivos de prueba en memoria, no operaciones bancarias reales.

Verificación en los servicios locales: política 20/80, histórico sin cambios, bloqueo de permisos ajenos, generación repetida sin fondos con resultado cero y migración V6 aplicada. Se revisaron los paneles administrador/docente en escritorio y móvil. No se crearon liquidaciones ni constancias ficticias.
