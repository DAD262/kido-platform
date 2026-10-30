# KIDO · Pagos reales de prueba con Mercado Pago (Checkout Pro)

> Actualización: el checkout ahora exige publicar la información comercial y aceptar sus condiciones. Antes de activar cobros, revisa [Comercio y pagos para la exposición](cambios/COMERCIO_Y_PAGOS_PARA_EXPOSICION.md). Configurar solo un Access Token no completa la activación.


Esta guía conecta KIDO con Mercado Pago para que la compra de un curso o de un certificado se apruebe **por Mercado Pago**, no por el botón de simulación.

## Cómo funciona

```
Estudiante ──"Comprar curso"──▶ kido-pago-ms crea la orden y una PREFERENCIA en Mercado Pago
                                 (external_reference = id de la orden, o CERT-id para certificados)
     │
     ▼
Checkout de Mercado Pago (misma pestaña) ── paga con tarjeta de prueba
     │
     ├──▶ vuelve a KIDO: /pago/resultado?payment_id=…&status=approved&external_reference=…
     │        └─ el frontend llama POST /pagos/ordenes/{id}/confirmar-mercado-pago?paymentId=…
     │
     └──▶ Mercado Pago avisa al WEBHOOK: POST /api/v1/pagos/webhooks/mercado-pago
              └─ kido-pago-ms consulta GET /v1/payments/{id} en Mercado Pago

En ambos caminos el backend comprueba en la API de Mercado Pago: estado "approved",
external_reference igual a la orden y monto idéntico. Solo entonces aprueba la orden,
registra el saldo del docente y publica el evento que crea la inscripción (SAGA).
```

El retorno al navegador funciona en local sin nada más. El webhook es el respaldo: confirma el pago aunque el estudiante cierre la pestaña. Para recibirlo en tu PC necesitas un túnel público (paso 5).

---

## Paso 1. Crea la aplicación en Mercado Pago

1. Entra a <https://www.mercadopago.com.pe/developers/panel/app> con tu cuenta de Mercado Pago (Perú).
2. **Crear aplicación** → nombre `KIDO` → tipo de pago **Pagos online** → producto **Checkout Pro**. Acepta los términos.

## Paso 2. Crea las cuentas de prueba

Dentro de la aplicación, ve a **Cuentas de prueba** → **Crear cuenta de prueba**:

| Cuenta | País | Para qué |
|---|---|---|
| Vendedor | Perú | Es «la tienda» KIDO que recibe el dinero ficticio. |
| Comprador | Perú | Con esta cuenta paga el estudiante en el checkout. |

Anota el usuario y la contraseña de cada una. Si Mercado Pago pide un código por correo al iniciar sesión, está en esa misma pantalla de **Cuentas de prueba**.

> No pagues con tu cuenta real contra una tienda de prueba (ni al revés): Mercado Pago lo bloquea con «Una de las partes con la que intentas hacer el pago es de prueba».

## Paso 3. Copia el Access Token

Tienes dos opciones. Empieza por la A; si el checkout no te deja pagar, usa la B.

**A. Credenciales de prueba de tu aplicación.** En la app → **Credenciales de prueba** → copia el **Access Token** (empieza con `TEST-`). KIDO detecta el prefijo y usa el enlace `sandbox_init_point`.

**B. Credenciales de la cuenta vendedora de prueba.** Abre una ventana de incógnito, inicia sesión en <https://www.mercadopago.com.pe/developers> con la **cuenta vendedora de prueba**, crea ahí una aplicación (igual que en el paso 1) y copia su **Access Token de producción** (empieza con `APP_USR-`). Como la cuenta es de prueba, no mueve dinero real. KIDO usa entonces `init_point`.

> El Access Token es secreto: solo va en `.env`, nunca en el frontend ni en GitHub.

## Paso 4. Configura `.env`

En la raíz de `kido-platform`, edita `.env` (sin borrar lo que ya tienes):

```env
MERCADO_PAGO_ACCESS_TOKEN=TEST-xxxxxxxxxxxxxxxx-xxxxxx-xxxxxxxxxxxxxxxx-xxxxxxxxx
MERCADO_PAGO_RETURN_URL=http://localhost:4300/pago/resultado
MERCADO_PAGO_AUTO_RETURN=false
# Se completan en el paso 5:
MERCADO_PAGO_NOTIFICATION_URL=
MERCADO_PAGO_WEBHOOK_SECRET=
# Para que el pago solo lo apruebe Mercado Pago, desactiva el botón de simulación:
KIDO_PAGOS_PERMITIR_SIMULACION=false
```

`MERCADO_PAGO_AUTO_RETURN=false` hace que, tras pagar, Mercado Pago muestre un botón «Volver al sitio». Ponlo en `true` solo si `MERCADO_PAGO_RETURN_URL` es una URL pública `https`; con `localhost` Mercado Pago puede rechazar la preferencia.

## Paso 5. Expón el webhook con un túnel (opcional, recomendado)

Mercado Pago no puede llamar a `localhost`. Abre un túnel hacia el **Gateway (18080)**:

```powershell
# Opción ngrok (https://ngrok.com, cuenta gratuita)
ngrok http 18080

# Opción Cloudflare (sin cuenta)
cloudflared tunnel --url http://localhost:18080
```

Copia la URL `https://…` que te muestra y arma:

```
https://TU-TUNEL/api/v1/pagos/webhooks/mercado-pago?source_news=webhooks
```

`?source_news=webhooks` evita que Mercado Pago envíe además el formato antiguo (IPN).

1. Ponla en `.env` como `MERCADO_PAGO_NOTIFICATION_URL`.
2. En el panel de tu app → **Webhooks → Configurar notificaciones** → **Modo de prueba**: pega la misma URL, marca el evento **Pagos** y **Guardar**.
3. En esa pantalla, revela la **Clave secreta** y cópiala en `MERCADO_PAGO_WEBHOOK_SECRET`. Desde ese momento KIDO rechaza (401) cualquier webhook sin una firma `x-signature` válida.

> La URL de ngrok gratuita cambia cada vez que lo reinicias: actualiza `.env` y el panel cuando ocurra.

## Paso 6. Reinicia lo necesario

El Config Server lee `infra/kido-config/config-repo` desde un volumen, así que basta con reiniciarlo. Hay que reconstruir `kido-pago-ms` porque su código cambió:

```powershell
docker compose restart kido-config
docker compose up -d --build kido-pago-ms
docker compose logs -f kido-pago-ms
```

Y en otra terminal, el frontend:

```powershell
cd frontend
npm.cmd start
```

## Paso 7. Prueba una compra

1. En **otra ventana de incógnito**, abre `http://localhost:4300`, ingresa como **estudiante** (tu número de estudiante se resuelve solo con `GET /api/v1/usuarios/me`).
2. Abre un curso **de pago** → **Comprar curso**. KIDO te lleva al checkout de Mercado Pago en la misma pestaña.
3. Inicia sesión con la **cuenta compradora de prueba**.
4. Paga con una tarjeta de prueba. Las tarjetas de Perú están en tu panel: **Tus integraciones → Tarjetas de prueba**. Usa:
   - **Titular:** `APRO` → aprobado · `OTHE` → rechazado · `CONT` → pendiente.
   - **CVV:** `123` (Amex: `1234`). **Vencimiento:** `11/30`. **Documento:** DNI, cualquier número de 8 dígitos.
5. Pulsa **Volver al sitio**. Llegas a `/pago/resultado`: KIDO verifica el pago con Mercado Pago y muestra «¡Pago aprobado!».
6. En **Mi aprendizaje** aparece el curso (la inscripción la crea la SAGA en unos segundos). En el panel del docente sube el saldo pendiente y en Administración → Finanzas la orden aparece **Aprobada**.

Repite con titular `OTHE` para ver el rechazo: no se cobra nada y la orden no se aprueba.

**Certificados:** con un certificado de pago pendiente, en Mi aprendizaje → **Pagar certificado**. El flujo es el mismo (`external_reference = CERT-<id>`).

## Paso 8. Comprueba el webhook

- En la consola de ngrok o cloudflared deberías ver `POST /api/v1/pagos/webhooks/mercado-pago 200`.
- En el panel de Mercado Pago, **Webhooks** muestra el historial de envíos y permite **Simular notificación**.
- En los logs de `kido-pago-ms` no debe aparecer «firma x-signature inválida». Si aparece, revisa que la clave secreta sea la de esa misma aplicación.

---

## Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| «Configura MERCADO_PAGO_ACCESS_TOKEN…» | `.env` sin token o el servicio no se reinició. | Paso 4 y paso 6. |
| El checkout dice «Una de las partes… es de prueba». | Mezclaste una cuenta real con una de prueba. | Paga con la cuenta compradora de prueba, en incógnito. Si persiste, usa la opción B del paso 3. |
| Volviste a KIDO pero dice «aún no reporta approved». | Mercado Pago tarda unos segundos. | La página reintenta sola; si no, pulsa **Volver a verificar**. El webhook también lo aprobará. |
| La orden queda en «Pago iniciado». | No volviste al sitio y el webhook no llega. | Activa el túnel (paso 5) o vuelve a abrir el enlace de retorno. |
| Webhook responde 401. | Clave secreta incorrecta o de otra aplicación. | Copia de nuevo la clave del panel de esa aplicación. |
| «El monto aprobado… no coincide». | Se cambió el precio del curso después de crear la orden. | Crea una orden nueva. |

## Qué se cambió en el código

- `kido-pago-ms/client/MercadoPagoClient.java`: `back_urls` hacia `/pago/resultado`, `auto_return` opcional, `statement_descriptor` y elección automática del enlace (`sandbox_init_point` con `TEST-`, `init_point` en otro caso) en el nuevo campo `checkoutUrl`.
- `kido-pago-ms/webhook/FirmaWebhook.java` y `ValidadorWebhookMercadoPago.java`: verificación HMAC-SHA256 del header `x-signature` (manifest `id:[data.id];request-id:[x-request-id];ts:[ts];`), con prueba unitaria `FirmaWebhookTest`.
- `PagoController.webhook`: solo procesa el tópico `payment`, responde 200 a los demás para evitar reintentos y no reintenta errores de negocio.
- Config (`kido-pago-ms-dev.yml`, `kido-pago-ms-prod.yml`), `compose.yml` y `.env.example`: variables `MERCADO_PAGO_RETURN_URL`, `MERCADO_PAGO_AUTO_RETURN`, `MERCADO_PAGO_WEBHOOK_SECRET` y `MERCADO_PAGO_STATEMENT_DESCRIPTOR`.
- Frontend: redirección en la misma pestaña (`core/utils/checkout.ts`, solo a dominios `mercadopago.com`) y nueva página `pages/pago-resultado` (ruta `/pago/resultado`, rol ESTUDIANTE) que confirma la orden o el certificado.

Referencias: [Webhooks de Checkout Pro](https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/additional-content/notifications/webhooks) · [Tarjetas de prueba](https://www.mercadopago.com.mx/developers/es/docs/checkout-pro/additional-content/your-integrations/test/cards) · [Compras de prueba](https://www.mercadopago.com.mx/developers/es/docs/checkout-pro/integration-test/test-purchases).


El reparto vigente y las liquidaciones quincenales se describen en [Liquidaciones docentes](liquidaciones-docentes.md).
