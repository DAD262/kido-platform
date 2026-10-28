package pe.edu.upeu.pago.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verificación del header {@code x-signature} de los webhooks de Mercado Pago (sin dependencias de
 * Spring, para poder probarla aislada).
 *
 * <p>Header: {@code ts=<epoch>,v1=<hmac>}. Manifest firmado con HMAC-SHA256 y la clave secreta de
 * la aplicación ({@code Tus integraciones → Webhooks → Clave secreta}):
 *
 * <pre>id:[data.id];request-id:[x-request-id];ts:[ts];</pre>
 *
 * Las partes ausentes se omiten del manifest. Si {@code data.id} es alfanumérico se usa en
 * minúsculas.
 */
public final class FirmaWebhook {

  private FirmaWebhook() {}

  public static String manifest(String dataId, String requestId, String ts) {
    StringBuilder m = new StringBuilder();
    if (dataId != null && !dataId.isBlank()) m.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
    if (requestId != null && !requestId.isBlank()) m.append("request-id:").append(requestId).append(';');
    if (ts != null && !ts.isBlank()) m.append("ts:").append(ts).append(';');
    return m.toString();
  }

  public static String hmacSha256Hex(String secret, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo calcular HMAC-SHA256", e);
    }
  }

  /**
   * @param toleranciaSegundos 0 = no comprobar antigüedad. Mercado Pago reintenta notificaciones
   *     durante días, así que una tolerancia corta rechazaría reintentos legítimos.
   */
  public static boolean valida(
      String secret, String xSignature, String xRequestId, String dataId, long toleranciaSegundos, Instant ahora) {
    if (secret == null || secret.isBlank() || xSignature == null || xSignature.isBlank()) return false;
    String ts = null, v1 = null;
    for (String parte : xSignature.split(",")) {
      String[] kv = parte.trim().split("=", 2);
      if (kv.length != 2) continue;
      if (kv[0].trim().equals("ts")) ts = kv[1].trim();
      else if (kv[0].trim().equals("v1")) v1 = kv[1].trim();
    }
    if (ts == null || v1 == null) return false;
    if (toleranciaSegundos > 0) {
      try {
        long t = Long.parseLong(ts);
        // ts puede venir en segundos o en milisegundos
        long seg = t > 9_999_999_999L ? t / 1000 : t;
        if (Math.abs(ahora.getEpochSecond() - seg) > toleranciaSegundos) return false;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    String esperado = hmacSha256Hex(secret, manifest(dataId, xRequestId, ts));
    return MessageDigest.isEqual(
        esperado.getBytes(StandardCharsets.US_ASCII), v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
  }
}
