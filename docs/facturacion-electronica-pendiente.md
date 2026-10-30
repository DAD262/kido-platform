# Preparación para facturación electrónica de KIDO

KIDO todavía no tiene RUC ni una entidad emisora configurada. Una constancia de compra de KIDO es informativa y no es una boleta ni factura electrónica. Una operación de prueba debe mostrar «OPERACIÓN DE PRUEBA — SIN VALIDEZ TRIBUTARIA» y nunca debe enviarse a SUNAT.

## Datos y decisiones que faltan

- Identidad, RUC, domicilio fiscal, régimen tributario y habilitación de la entidad que emite.
- Confirmación profesional del tratamiento tributario aplicable a cada servicio, incluidos afectación al IGV, tasas, precios y redondeo. No asumir que todo curso está gravado ni deducir impuestos de un pago sin una regla aprobada.
- Elección del sistema de emisión electrónica de SUNAT o del proveedor autorizado (PSE/OSE), y configuración segura de sus credenciales y certificados cuando corresponda.
- Requisitos reales de identificación del comprador según tipo de comprobante, importe y normativa vigente; obtenerlos con consentimiento y validarlos en el backend.
- Política de notas de crédito, devoluciones, contingencias y reintentos, incluyendo recepción, conservación y consulta del estado ante SUNAT u OSE.

## Diseño a implementar

1. Conservar por orden una instantánea de los datos del comprador que se usaron para la emisión, y por separado los datos del emisor y las reglas tributarias vigentes al momento de emitir. La edición posterior del perfil o del RUC no debe modificar documentos ya emitidos.
2. Mantener separados la constancia de compra, el pago y el comprobante electrónico. Crear un comprobante solo después de verificar el pago en el proveedor, aplicando el régimen autorizado; no usar el estado recibido en el navegador como prueba de pago.
3. Registrar tipo de documento, serie y número, moneda, conceptos, base imponible, tributos, total, fecha en America/Lima y referencias a la operación, únicamente cuando existan datos y reglas verificadas. No exponer la comisión interna al comprador.
4. Registrar el estado de envío y la respuesta del sistema emisor, con claves de idempotencia para evitar comprobantes duplicados por reintentos o webhooks repetidos. Una representación PDF no sustituye la emisión ni la respuesta del sistema electrónico.
5. Restringir la consulta y descarga al comprador titular y a administradores autorizados. Evitar incluir información personal o credenciales en logs.
6. Usar ambientes y datos aislados para pruebas; jamás transmitir operaciones de prueba a producción ni incluirlas en saldos o liquidaciones reales.

## Fuentes oficiales

- [SUNAT: boleta de venta electrónica](https://cpe.sunat.gob.pe/tipos_de_comprobantes/boleta).
- [SUNAT: sistema de emisión con operador de servicios electrónicos](https://cpe.sunat.gob.pe/informacion_general/operador_servicios_electronicos).
- [SUNAT: información general sobre comprobantes de pago electrónicos](https://cpe.sunat.gob.pe/informacion_general/obligados_cpe).
