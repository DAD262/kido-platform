package pe.edu.upeu.pago.webhook;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Si MERCADO_PAGO_WEBHOOK_SECRET está configurado, solo se aceptan webhooks con firma válida. Si
 * está vacío (desarrollo inicial), se aceptan todos: no es inseguro para aprobar pagos porque el
 * servicio siempre vuelve a consultar el pago en la API de Mercado Pago antes de aprobar, pero sí
 * permite a terceros forzar consultas innecesarias.
 */
@Slf4j
@Component
public class ValidadorWebhookMercadoPago {

  @Value("${kido.mercadopago.webhook-secret:}")
  private String secreto;

  @Value("${kido.mercadopago.webhook-tolerancia-segundos:0}")
  private long tolerancia;

  public boolean activo() {
    return secreto != null && !secreto.isBlank();
  }

  public boolean aceptar(String xSignature, String xRequestId, String dataId) {
    if (!activo()) return true;
    boolean ok = FirmaWebhook.valida(secreto, xSignature, xRequestId, dataId, tolerancia, Instant.now());
    if (!ok) log.warn("Webhook de Mercado Pago rechazado: firma x-signature inválida (request-id={})", xRequestId);
    return ok;
  }
}
