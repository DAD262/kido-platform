# kido-pago-ms

Microservicio de Midwar para compras y liquidaciones de Kido. Mantiene su propia base PostgreSQL y se registra en Eureka.

Reglas implementadas: comisión Kido 10 %, 90 % al docente, retención 7 días, retiro mínimo S/ 50, reembolso hasta 7 días con progreso máximo 20 %, inscripción automática tras pago aprobado y revocación por reembolso.

Mercado Pago usa la API real de sandbox cuando `MERCADO_PAGO_ACCESS_TOKEN` contiene un token `TEST-...`. En DEV existe el endpoint de simulación para demostrar el flujo sin exponer credenciales.
