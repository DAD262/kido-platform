# kido-pago-ms

Microservicio de Midwar para compras y liquidaciones de Kido. Mantiene su propia base PostgreSQL y se registra en Eureka.

Reglas implementadas: comisión Kido 20 %, 80 % al docente (`kido.pagos.comision: 0.20`), retención 7 días, retiro mínimo S/ 50, reembolso hasta 7 días con progreso máximo 20 %, inscripción automática tras pago aprobado y revocación por reembolso.

Mercado Pago usa la API real de sandbox cuando `MERCADO_PAGO_ACCESS_TOKEN` contiene un token `TEST-...`.

Yape se cobra a través de Mercado Pago (Checkout API): el navegador genera un token con `mp.yape({otp, phoneNumber})` usando `MERCADO_PAGO_PUBLIC_KEY`, y este servicio crea el pago con `payment_method_id=yape` e `installments=1`. KIDO nunca recibe el celular ni el código de aprobación. El checkout muestra los medios habilitados por Mercado Pago para la cuenta. En DEV las credenciales de prueba simulan la operación y no transfieren dinero; los cobros reales requieren credenciales de producción.
