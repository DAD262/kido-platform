# Cambios: Yape con Mercado Pago (19/11/2026)

## Qué cambió

### Yape ahora se cobra por Mercado Pago (automático)
- `frontend/src/app/pages/pago-interno/pago-interno.ts` y `.html`: al elegir Yape el alumno escribe su
  celular y el **código de aprobación** de su app Yape. El SDK de Mercado Pago genera un token de un solo
  uso (`mp.yape({otp, phoneNumber}).create()`) y KIDO solo envía ese token al backend. El backend crea el
  pago con `payment_method_id=yape` e `installments=1`, y si sale `approved` la inscripción es inmediata.
- Mensajes claros cuando Yape rechaza (código vencido, límite diario, datos incorrectos).
- Aviso cuando el total supera S/ 500 (límite diario de Yape).
- El QR manual sigue existiendo solo como demo y está apagado con `KIDO_PAGOS_YAPE_CURSO_ID=0`.

### Pagos que se quedaban trabados (backend)
- `CheckoutInternoService.resolverAtascado`: si un pago anterior quedó sin respuesta por timeout, a los
  2 minutos se consulta Mercado Pago por `external_reference`. Si ese pago existe y está aprobado, se usa
  (no se cobra dos veces). Si fue rechazado, se libera para que el alumno pruebe con un código nuevo. Si
  no existe, se libera a los 10 minutos.
- `MercadoPagoClient.pagosDeReferencia` y `IntentoCheckoutStore.sinResolver` (nuevos).
- Pruebas nuevas en `CheckoutInternoServiceTest` y `pago-interno.spec.ts`.

### Incoherencias corregidas
- La comisión del README decía 10/90, pero el código, el panel y las pruebas usan **20 % KIDO / 80 %
  docente**. Se corrigió el README.
- `.env.example` no tenía `MERCADO_PAGO_PUBLIC_KEY` (sin esa variable no aparece el formulario de pago), ni
  `KIDO_CHECKOUT_PRUEBA_LOCAL` / `KIDO_ENTORNO_DESARROLLO_LOCAL`. Ahora están.
- Se quitó el nombre personal escrito a mano en la página del QR.
- Se borraron los `pom.xml.bak-*` sobrantes.

## Cómo instalarlo en VS Code
1. Descomprime el zip en una carpeta nueva y ábrela con **Archivo → Abrir carpeta**.
2. Copia tu `.env` actual a la raíz y agrega estas líneas con los datos de **Tus integraciones →
   Credenciales de prueba** de Mercado Pago (Access Token y Public Key deben ser de la **misma** aplicación):
   ```
   MERCADO_PAGO_ACCESS_TOKEN=TEST-...
   MERCADO_PAGO_PUBLIC_KEY=TEST-...
   KIDO_PAGOS_YAPE_CURSO_ID=0
   KIDO_CHECKOUT_PRUEBA_LOCAL=true
   KIDO_ENTORNO_DESARROLLO_LOCAL=true
   ```
3. Recompila el servicio de pagos y el frontend:
   ```
   docker compose up -d --build kido-pago-ms
   cd frontend && npm install && npm test && npm start
   ```
   (Si no usas Docker para el backend: `cd services/kido-pago-ms` y luego `./mvnw test`.)
4. En tu cuenta de Mercado Pago, comprueba que **Yape** está activo como medio de pago. Si no aparece en
   la página de pago, es porque la cuenta no lo tiene habilitado.

## Cómo probarlo (sandbox)
Compra un curso → elige **Yape** → acepta las condiciones:

| Celular | Código | Resultado |
|---|---|---|
| 111111111 | 123456 | Aprobado |
| 111111112 | 123456 | Rechazado (no autorizado) |
| 111111113 | 123456 | Rechazado (límite de monto) |
| 111111117 | 123456 | Rechazado (código inválido) |

## Antes de pasar a producción
- Usa las credenciales de producción, publica KIDO con HTTPS y configura `MERCADO_PAGO_NOTIFICATION_URL`
  y `MERCADO_PAGO_WEBHOOK_SECRET`.
- Confirma con Mercado Pago el modelo de cobro a nombre de los docentes
  (`MERCADO_PAGO_MODELO_RECAUDACION_CONFIRMADO`).
