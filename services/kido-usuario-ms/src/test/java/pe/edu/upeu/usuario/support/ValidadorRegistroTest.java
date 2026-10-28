package pe.edu.upeu.usuario.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ValidadorRegistroTest {

    @Test
    void contrasenaSigueLaPoliticaDelRealm() {
        assertNull(ValidadorRegistro.problemaContrasena("Docente123!", "dani@kido.local"));
        assertNotNull(ValidadorRegistro.problemaContrasena("corta1A", null));
        assertNotNull(ValidadorRegistro.problemaContrasena("sinmayus123", null));
        assertNotNull(ValidadorRegistro.problemaContrasena("SINMINUS123", null));
        assertNotNull(ValidadorRegistro.problemaContrasena("SinNumeros", null));
        assertNotNull(ValidadorRegistro.problemaContrasena("Mateo2026x", "mateo2026@correo.pe"));
    }

    @Test
    void normalizaDniYCelular() {
        assertEquals("45678912", ValidadorRegistro.dni(" 4567 8912"));
        assertThrows(IllegalArgumentException.class, () -> ValidadorRegistro.dni("1234567"));
        assertThrows(IllegalArgumentException.class, () -> ValidadorRegistro.dni("11111111"));
        assertEquals("987654321", ValidadorRegistro.celular("+51 987 654 321"));
        assertThrows(IllegalArgumentException.class, () -> ValidadorRegistro.celular("812345678"));
    }

    @Test
    void enlaceSoloHttp() {
        assertEquals("https://linkedin.com/in/x", ValidadorRegistro.enlace("https://linkedin.com/in/x"));
        assertNull(ValidadorRegistro.enlace("   "));
        assertThrows(IllegalArgumentException.class, () -> ValidadorRegistro.enlace("javascript:alert(1)"));
    }

    @Test
    void documentoDebeSerPdfReal() {
        ValidadorRegistro.documentoPdf("cv.pdf", 100, "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class,
                () -> ValidadorRegistro.documentoPdf("cv.pdf", 100, "MZ\u0090".getBytes(StandardCharsets.ISO_8859_1)));
        assertThrows(IllegalArgumentException.class,
                () -> ValidadorRegistro.documentoPdf("cv.pdf", ValidadorRegistro.MAX_DOCUMENTO_BYTES + 1, "%PDF-".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("mi cv_1_.PDF", ValidadorRegistro.nombreArchivo("C:\\x\\../mi cv<1>.PDF"));
        assertEquals("foto.pdf", ValidadorRegistro.nombreArchivo("foto"));
    }

    @Test
    void nombresSinHtml() {
        assertEquals("María José", ValidadorRegistro.nombre("  María   José ", "nombres"));
        assertThrows(IllegalArgumentException.class, () -> ValidadorRegistro.nombre("<script>", "nombres"));
    }

    @Test
    void limitadorPorVentana() {
        RelojFijo reloj = new RelojFijo();
        LimitadorIntentos limitador = new LimitadorIntentos(2, Duration.ofMinutes(10), reloj);
        assertTrue(limitador.permitir("1.2.3.4"));
        assertTrue(limitador.permitir("1.2.3.4"));
        assertFalse(limitador.permitir("1.2.3.4"));
        assertTrue(limitador.permitir("5.6.7.8"));
        reloj.ms = Duration.ofMinutes(11).toMillis();
        assertTrue(limitador.permitir("1.2.3.4"));
    }

    private static final class RelojFijo extends Clock {
        long ms;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(ms);
        }
    }
}
