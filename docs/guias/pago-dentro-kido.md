# Pago dentro de KIDO

La selección de logos es interactiva. Después de preparar la compra, KIDO abre `/pago/interno/{tipo}/{id}` y conserva el medio elegido. Soporta cursos individuales, compras conjuntas y certificados.

- Tarjetas: Card Payment Brick de Mercado Pago, incluyendo crédito y débito. La marca de la tarjeta debe coincidir con la seleccionada.
- Yape: celular y código de aprobación, tokenizados mediante MercadoPago.js. El código se borra después del intento y no se envía al backend de KIDO.
- PagoEfectivo: genera una operación pendiente y presenta el código de pago e instrucciones del proveedor. Generar el código no acredita la compra; el comprador lo paga desde una entidad compatible.

## Configuración pendiente

Añadir `MERCADO_PAGO_PUBLIC_KEY` al `.env` privado, con la Public Key de la misma aplicación y cuenta que el Access Token configurado. No publicar el archivo ni colocar el Access Token en Angular. El backend transmite únicamente la Public Key al comprador autenticado. Sin esta configuración la pantalla informa que está pendiente de activación y rechaza crear cobros internos.

```powershell
docker compose up -d --build --no-deps kido-pago-ms kido-gateway
```

## Validación y protección

El importe y la referencia se obtienen de la compra persistida. El backend verifica titular, estado, ambiente, receptor, medio disponible y aceptación vigente. Solo recibe tokens de pago; no recibe el número de tarjeta, vencimiento ni CVV. Los intentos guardan una huella y una clave de idempotencia antes de comunicarse con Mercado Pago. Una operación pendiente o una respuesta ambigua impiden iniciar otra con datos distintos. Los intentos rechazados explícitamente permiten volver a elegir un medio.

Una respuesta aprobada se confirma con los verificadores existentes, que vuelven a comprobar el pago contra Mercado Pago antes de habilitar accesos. No se confía en estados enviados por el navegador. Los pagos pendientes se consultan desde Mis compras; requieren confirmación posterior del proveedor. Las verificaciones adicionales que exija el emisor siguen sujetas a Mercado Pago.

La migración V11 crea `intentos_checkout_interno`. Las pruebas automatizadas usan respuestas simuladas del proveedor; la prueba de extremo a extremo de cada medio requiere la Public Key y datos de sandbox. No se han realizado cobros para implementar esta función.

## Documentación oficial utilizada

- [Card Payment Brick](https://www.mercadopago.com.pe/developers/es/docs/checkout-bricks/card-payment-brick/default-rendering).
- [Yape por Payments API](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-payments/integration-configuration/yape).
- [PagoEfectivo y otros medios](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-payments/integration-configuration/other-payment-methods).
