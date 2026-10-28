package pe.edu.upeu.pago.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FirmaWebhookTest {

  private static final String SECRETO = "clave-de-prueba";

  @Test
  void construyeElManifestEnElOrdenDocumentado() {
    assertEquals("id:123456;request-id:abc-1;ts:1704908010;", FirmaWebhook.manifest("123456", "abc-1", "1704908010"));
    assertEquals("id:abcdef;ts:1;", FirmaWebhook.manifest("ABCDEF", null, "1"));
  }

  @Test
  void aceptaUnaFirmaCorrectaYRechazaUnaAlterada() {
    String ts = "1704908010";
    String v1 = FirmaWebhook.hmacSha256Hex(SECRETO, FirmaWebhook.manifest("123456", "req-9", ts));
    String header = "ts=" + ts + ",v1=" + v1;
    Instant ahora = Instant.ofEpochSecond(1704908010L);

    assertTrue(FirmaWebhook.valida(SECRETO, header, "req-9", "123456", 0, ahora));
    assertFalse(FirmaWebhook.valida(SECRETO, header, "req-9", "999999", 0, ahora), "otro pago");
    assertFalse(FirmaWebhook.valida("otra-clave", header, "req-9", "123456", 0, ahora), "otra clave");
    assertFalse(FirmaWebhook.valida(SECRETO, "ts=" + ts, "req-9", "123456", 0, ahora), "sin v1");
  }

  @Test
  void respetaLaToleranciaDeTiempoSiSeConfigura() {
    String ts = "1704908010";
    String header = "ts=" + ts + ",v1=" + FirmaWebhook.hmacSha256Hex(SECRETO, FirmaWebhook.manifest("1", "r", ts));
    assertFalse(FirmaWebhook.valida(SECRETO, header, "r", "1", 300, Instant.ofEpochSecond(1704908010L + 3600)));
    assertTrue(FirmaWebhook.valida(SECRETO, header, "r", "1", 0, Instant.ofEpochSecond(1704908010L + 3600)));
  }
}
