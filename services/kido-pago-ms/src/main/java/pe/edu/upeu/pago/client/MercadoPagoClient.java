package pe.edu.upeu.pago.client;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.pago.dto.PreferenciaResponse;
import pe.edu.upeu.pago.entity.OrdenCompra;

/**
 * Cliente de Mercado Pago (Checkout Pro).
 *
 * <p>Flujo: KIDO crea una preferencia con {@code external_reference} = id de la orden (o
 * {@code CERT-id} para certificados). El estudiante paga en Mercado Pago y vuelve a KIDO por
 * {@code back_urls}. La aprobación se confirma consultando el pago en la API de Mercado Pago, ya
 * sea por el webhook o desde la página de resultado del frontend; nunca se confía solo en lo que
 * dice el navegador.
 */
@Component
@RequiredArgsConstructor
public class MercadoPagoClient {

  private final RestClient mercadoPagoRestClient;

  @Value("${kido.mercadopago.access-token:}")
  private String token;

  /** URL pública (https) del webhook; vacía = sin notificaciones automáticas. */
  @Value("${kido.mercadopago.notification-url:}")
  private String notificationUrl;

  /** Página del frontend a la que vuelve el estudiante, p. ej. http://localhost:4200/pago/resultado */
  @Value("${kido.mercadopago.return-url:}")
  private String returnUrl;

  /**
   * Redirige solo al aprobarse el pago. Mercado Pago puede rechazar la preferencia si la URL de
   * retorno no es pública; por eso es opcional y se activa con MERCADO_PAGO_AUTO_RETURN=true.
   */
  @Value("${kido.mercadopago.auto-return:false}")
  private boolean autoReturn;

  /** Texto que verá el comprador en el resumen de su tarjeta. */
  @Value("${kido.mercadopago.statement-descriptor:KIDO}")
  private String statementDescriptor;

  public boolean credencialDePrueba() { return token != null && token.startsWith("TEST-"); }
  @SuppressWarnings("unchecked")
  public List<pe.edu.upeu.pago.dto.MetodoPagoResponse.Medio> mediosDisponibles() {
    requireToken();
    List<Map<String,Object>> datos=mercadoPagoRestClient.get().uri("/v1/payment_methods").header("Authorization","Bearer "+token).retrieve().body(List.class);
    if(datos==null)throw new IllegalStateException("No se pudieron consultar los medios de pago");
    Map<String,pe.edu.upeu.pago.dto.MetodoPagoResponse.Medio> medios=new LinkedHashMap<>();
    for(var dato:datos){String id=str(dato,"id"),tipo=str(dato,"payment_type_id"),nombre=str(dato,"name");
      if(!"active".equals(str(dato,"status"))||id==null||tipo==null||nombre==null)continue;
      if(List.of("credit_card","debit_card","prepaid_card").contains(tipo)||List.of("yape","pagoefectivo_atm","account_money").contains(id))medios.putIfAbsent(id,new pe.edu.upeu.pago.dto.MetodoPagoResponse.Medio(id,nombre,tipo));
    }
    return medios.values().stream().limit(20).toList();
  }
  public boolean disponible() { return token != null && !token.isBlank(); }

  public PreferenciaResponse crearPreferencia(OrdenCompra o) {
    return crearPreferencia(o.getCursoTitulo(), o.getMontoTotal(), String.valueOf(o.getId()));
  }

  public PreferenciaResponse crearPreferencia(String titulo, BigDecimal monto, String referencia) {
    requireToken();
    Map<String, Object> req = new LinkedHashMap<>();
    req.put(
        "items",
        List.of(
            Map.of(
                "id", referencia,
                "title", titulo,
                "quantity", 1,
                "currency_id", "PEN",
                "unit_price", monto)));
    req.put("external_reference", referencia);
    req.put("statement_descriptor", statementDescriptor);
    if (!notificationUrl.isBlank()) req.put("notification_url", notificationUrl);
    if (!returnUrl.isBlank()) {
      req.put(
          "back_urls",
          Map.of(
              "success", returnUrl,
              "pending", returnUrl,
              "failure", returnUrl));
      if (autoReturn) req.put("auto_return", "approved");
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> res =
        mercadoPagoRestClient
            .post()
            .uri("/checkout/preferences")
            .header("Authorization", "Bearer " + token)
            .body(req)
            .retrieve()
            .body(Map.class);
    String init = str(res, "init_point");
    String sandbox = str(res, "sandbox_init_point");
    boolean credencialTest = token.startsWith("TEST-");
    String checkout = credencialTest && sandbox != null ? sandbox : init != null ? init : sandbox;
    if (str(res, "id") == null || checkout == null)
      throw new IllegalStateException("Mercado Pago no devolvió un enlace de pago. Inténtalo de nuevo.");
    return new PreferenciaResponse(str(res, "id"), init, sandbox, checkout);
  }

  public PagoExterno obtenerPago(String paymentId) {
    requireToken();
    if (paymentId == null || !paymentId.matches("[0-9]{1,30}")) throw new IllegalArgumentException("Identificador de pago inválido");
    @SuppressWarnings("unchecked")
    Map<String, Object> res =
        mercadoPagoRestClient
            .get()
            .uri("/v1/payments/{id}", paymentId)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .body(Map.class);
    return new PagoExterno(
        str(res, "id"),
        str(res, "status"),
        str(res, "external_reference"),
        str(res, "transaction_amount"), str(res, "currency_id"), costoProveedor(res),
        res!=null&&res.get("transaction_details") instanceof Map<?,?> d?decimal(d.get("net_received_amount")):null,
        str(res,"money_release_status"), str(res,"money_release_date"), res==null?null:decimal(res.get("transaction_amount_refunded")));
  }

  public void reembolsar(String paymentId, BigDecimal amount, String idempotencyKey) {
    requireToken();
    mercadoPagoRestClient
        .post()
        .uri("/v1/payments/{id}/refunds", paymentId)
        .header("Authorization", "Bearer " + token)
        .header("X-Idempotency-Key", idempotencyKey)
        .body(Map.of("amount", amount))
        .retrieve()
        .toBodilessEntity();
  }

  private void requireToken() {
    if (token == null || token.isBlank())
      throw new IllegalStateException(
          "Mercado Pago aún no está habilitado. Inténtalo más tarde.");
  }

  private String str(Map<String, Object> m, String k) {
    return m == null || m.get(k) == null ? null : String.valueOf(m.get(k));
  }

  private static BigDecimal decimal(Object value){return value==null?null:new BigDecimal(String.valueOf(value));}
  private static BigDecimal costoProveedor(Map<String,Object> res){
    if(res==null||!(res.get("fee_details") instanceof List<?> fees))return null;
    BigDecimal total=BigDecimal.ZERO;for(Object item:fees)if(item instanceof Map<?,?> fee&&"collector".equals(fee.get("fee_payer"))){var amount=decimal(fee.get("amount"));if(amount!=null)total=total.add(amount);}return total.setScale(2,java.math.RoundingMode.HALF_UP);
  }
  public record PagoExterno(String id,String status,String externalReference,String amount,String currency,BigDecimal costoProcesamiento,BigDecimal netoRecibido,String fondosEstado,String fechaLiberacion,BigDecimal reembolsado) {
    public PagoExterno(String id,String status,String externalReference,String amount,String currency){this(id,status,externalReference,amount,currency,null,null,null,null,null);}
    public void validarImporte(BigDecimal esperado) {
      if (!"PEN".equals(currency)) throw new IllegalArgumentException("La moneda del pago no corresponde a soles");
      if (amount == null || new BigDecimal(amount).compareTo(esperado) != 0)
        throw new IllegalArgumentException("El monto aprobado por Mercado Pago no coincide con la orden");
    }
  }
}
