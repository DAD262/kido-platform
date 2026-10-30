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
import pe.edu.upeu.pago.entity.AmbientePago;

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
  @Value("${kido.mercadopago.ambiente:pruebas}")
  private String ambiente;

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

  public record CuentaVerificada(String receptor, AmbientePago ambiente) {}

  /** Consulta la identidad al proveedor; no infiere el ambiente del prefijo del token. */
  public CuentaVerificada verificarCuenta() {
    requireToken();
    @SuppressWarnings("unchecked")
    Map<String,Object> usuario = mercadoPagoRestClient.get().uri("/users/me")
        .header("Authorization", "Bearer " + token).retrieve().body(Map.class);
    return interpretarCuenta(usuario);
  }

  public CuentaVerificada verificarCuentaConfigurada() {
    var cuenta = verificarCuenta();
    var esperado = switch (ambiente) {
      case "pruebas" -> AmbientePago.PRUEBAS;
      case "produccion" -> AmbientePago.PRODUCCION;
      default -> throw new IllegalStateException("Ambiente de Mercado Pago inválido");
    };
    if (cuenta.ambiente() != esperado) throw new IllegalStateException("La cuenta no corresponde al ambiente configurado");
    return cuenta;
  }

  /** Verifica también la identidad del vendedor para cuentas de prueba con live_mode=true. */
  public void validarPago(PagoExterno pago, String receptor, AmbientePago ambiente) {
    if (ambiente == AmbientePago.PRUEBAS && Boolean.TRUE.equals(pago.liveMode())) {
      var cuenta = verificarCuentaConfigurada();
      if (cuenta.ambiente() != AmbientePago.PRUEBAS || !java.util.Objects.equals(cuenta.receptor(), receptor)
          || !java.util.Objects.equals(pago.receptor(), receptor))
        throw new IllegalArgumentException("El receptor o ambiente del pago no corresponde a la orden");
      return;
    }
    pago.validarIdentidad(receptor, ambiente);
  }

  public static CuentaVerificada interpretarCuenta(Map<String,Object> usuario) {
    if (usuario == null || usuario.get("id") == null || !(usuario.get("tags") instanceof List<?> tags))
      throw new IllegalStateException("Mercado Pago no devolvió una identidad verificable");
    String id = String.valueOf(usuario.get("id"));
    if (!id.matches("[0-9]{1,40}") || !"MPE".equals(usuario.get("site_id")))
      throw new IllegalStateException("Cuenta de Mercado Pago inválida o ajena a Perú");
    return new CuentaVerificada(id, tags.contains("test_user") ? AmbientePago.PRUEBAS : AmbientePago.PRODUCCION);
  }

  public PreferenciaResponse crearPreferencia(OrdenCompra o) {
    return crearPreferencia(o.getCursoTitulo(), o.getMontoTotal(), String.valueOf(o.getId()));
  }

  public PreferenciaResponse crearPreferencia(String titulo, BigDecimal monto, String referencia) {
    return crearPreferenciaItems(List.of(Map.of("id",referencia,"title",titulo,"quantity",1,"currency_id","PEN","unit_price",monto)),referencia);
  }
  public PreferenciaResponse crearPreferenciaCarrito(List<OrdenCompra> lineas,String referencia){
    return crearPreferenciaItems(lineas.stream().map(o->Map.<String,Object>of("id",String.valueOf(o.getCursoId()),"title",o.getCursoTitulo(),"quantity",1,"currency_id","PEN","unit_price",o.getMontoTotal())).toList(),referencia);
  }
  private PreferenciaResponse crearPreferenciaItems(List<Map<String,Object>> items,String referencia){
    requireToken();
    Map<String, Object> req = new LinkedHashMap<>();
    req.put("items",items);
    req.put("external_reference", referencia);
    req.put("statement_descriptor", statementDescriptor);
    if (!notificationUrl.isBlank()) req.put("notification_url", notificationUrl);
    if (pe.edu.upeu.pago.service.ComercioService.https(returnUrl)) {
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
    return new PreferenciaResponse(str(res, "id"), init, sandbox, checkout,pe.edu.upeu.pago.service.ComercioService.https(returnUrl));
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
        str(res,"money_release_status"), str(res,"money_release_date"), res==null?null:decimal(res.get("transaction_amount_refunded")),
        str(res,"collector_id"), res != null && res.get("live_mode") instanceof Boolean b ? b : null,
        str(res,"payment_method_id"),str(res,"status_detail"),str(res,"date_approved"));
  }

  @SuppressWarnings("unchecked")
  public List<String> pagosAprobadosDeOrden(String referencia) {
    requireToken();
    Map<String,Object> resultado=mercadoPagoRestClient.get().uri(b->b.path("/v1/payments/search")
        .queryParam("external_reference",referencia).queryParam("status","approved").queryParam("limit",100).build())
        .header("Authorization","Bearer "+token).retrieve().body(Map.class);
    if(resultado==null||!(resultado.get("results") instanceof List<?> filas))throw new IllegalStateException("No se pudo verificar el pago con Mercado Pago");
    return filas.stream().filter(Map.class::isInstance).map(x->(Map<String,Object>)x)
        .filter(x->referencia.equals(str(x,"external_reference"))&&"approved".equals(str(x,"status")))
        .map(x->str(x,"id")).filter(java.util.Objects::nonNull).filter(x->x.matches("[0-9]{1,30}"))
        .distinct().sorted(java.util.Comparator.comparing(java.math.BigInteger::new)).toList();
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
  public record PagoExterno(String id,String status,String externalReference,String amount,String currency,BigDecimal costoProcesamiento,BigDecimal netoRecibido,String fondosEstado,String fechaLiberacion,BigDecimal reembolsado,String receptor,Boolean liveMode,String medioPago,String detalleEstado,String fechaAprobada) {
    public PagoExterno(String id,String status,String externalReference,String amount,String currency){this(id,status,externalReference,amount,currency,null,null,null,null,null,null,null,null,null,null);}
    public PagoExterno(String id,String status,String externalReference,String amount,String currency,BigDecimal costoProcesamiento,BigDecimal netoRecibido,String fondosEstado,String fechaLiberacion,BigDecimal reembolsado){this(id,status,externalReference,amount,currency,costoProcesamiento,netoRecibido,fondosEstado,fechaLiberacion,reembolsado,null,null,null,null,null);}
    public PagoExterno(String id,String status,String externalReference,String amount,String currency,BigDecimal costoProcesamiento,BigDecimal netoRecibido,String fondosEstado,String fechaLiberacion,BigDecimal reembolsado,String receptor,Boolean liveMode){this(id,status,externalReference,amount,currency,costoProcesamiento,netoRecibido,fondosEstado,fechaLiberacion,reembolsado,receptor,liveMode,null,null,null);}
    public void validarIdentidad(String receptorEsperado, AmbientePago ambienteEsperado) {
      if (receptorEsperado == null || !receptorEsperado.equals(receptor) || liveMode == null
          || ambienteEsperado == null || ambienteEsperado == AmbientePago.DESCONOCIDO
          || (ambienteEsperado == AmbientePago.PRODUCCION) != liveMode)
        throw new IllegalArgumentException("El receptor o ambiente del pago no corresponde a la orden");
    }
    public void validarImporte(BigDecimal esperado) {
      if (!"PEN".equals(currency)) throw new IllegalArgumentException("La moneda del pago no corresponde a soles");
      if (amount == null || new BigDecimal(amount).compareTo(esperado) != 0)
        throw new IllegalArgumentException("El monto aprobado por Mercado Pago no coincide con la orden");
    }
  }
}
