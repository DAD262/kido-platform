# Cambios realizados por Midwar

Este archivo sirve como evidencia de aporte para el Pull Request de Midwar.

## Nuevos microservicios

- `services/kido-pago-ms`
- `services/kido-notificacion-ms`

## Integración agregada

- Configuración DEV/PROD de ambos servicios en Config Server.
- Rutas Gateway para pagos, retiros, reembolsos y notificaciones.
- PostgreSQL independiente para Pago y Notificación.
- Contenedores nuevos en `compose.yml`.
- Endpoint interno de Curso para obtener snapshot de compra y lecciones.
- Endpoints internos de Inscripción para crear acceso de compra, consultar progreso y revocar acceso.

## Reglas financieras programadas

- Comisión Kido 10 %.
- 90 % de la venta para el docente.
- Retención de 7 días.
- Retiro mínimo de S/ 50.
- Cuenta bancaria, Yape o Plin.
- Reembolso hasta 7 días y progreso máximo 20 %.
- Reembolso real por API de Mercado Pago cuando el pago no es simulado.
- Registro histórico de movimientos y reservas para impedir dobles retiros.

## Evidencia sugerida de Git

```bash
git checkout -b feature/pagos-notificaciones
git add .
git commit -m "feat(midwar): agrega pagos retiros reembolsos y notificaciones"
git push -u origin feature/pagos-notificaciones
```

Después crea un Pull Request hacia `main` usando la cuenta de Midwar.
