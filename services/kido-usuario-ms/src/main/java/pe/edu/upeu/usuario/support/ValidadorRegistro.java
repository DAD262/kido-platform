package pe.edu.upeu.usuario.support;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de negocio del registro que no dependen de Spring, para poder probarlas solas.
 * La política de contraseña replica la del realm (kido-realm.json → passwordPolicy) para
 * dar un mensaje claro antes de llamar a Keycloak, que de todas formas la vuelve a exigir.
 */
public final class ValidadorRegistro {

    public static final long MAX_DOCUMENTO_BYTES = 5L * 1024 * 1024;
    private static final Pattern DNI = Pattern.compile("\\d{8}");
    private static final Pattern CELULAR_PE = Pattern.compile("9\\d{8}");
    private static final Pattern NOMBRE = Pattern.compile("[\\p{L}][\\p{L} '\\-]{0,79}");
    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};

    private ValidadorRegistro() {
    }

    /** Une espacios repetidos y recorta. Devuelve null si queda vacío. */
    public static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.strip().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }

    public static String email(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? null : limpio.toLowerCase(Locale.ROOT);
    }

    public static String nombre(String valor, String campo) {
        String limpio = limpiar(valor);
        if (limpio == null || !NOMBRE.matcher(limpio).matches()) {
            throw new IllegalArgumentException(campo + ": usa solo letras, espacios, apóstrofo o guion.");
        }
        return limpio;
    }

    /** Devuelve el motivo por el que la contraseña no cumple la política, o null si la cumple. */
    public static String problemaContrasena(String contrasena, String email) {
        if (contrasena == null || contrasena.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres.";
        }
        if (contrasena.length() > 72) {
            return "La contraseña puede tener como máximo 72 caracteres.";
        }
        if (contrasena.chars().noneMatch(Character::isUpperCase)) {
            return "La contraseña necesita al menos una mayúscula.";
        }
        if (contrasena.chars().noneMatch(Character::isLowerCase)) {
            return "La contraseña necesita al menos una minúscula.";
        }
        if (contrasena.chars().noneMatch(Character::isDigit)) {
            return "La contraseña necesita al menos un número.";
        }
        if (email != null) {
            String usuario = email.split("@", 2)[0].toLowerCase(Locale.ROOT);
            String minuscula = contrasena.toLowerCase(Locale.ROOT);
            if (minuscula.equals(email.toLowerCase(Locale.ROOT)) || (usuario.length() >= 4 && minuscula.contains(usuario))) {
                return "La contraseña no puede contener tu correo.";
            }
        }
        return null;
    }

    public static String dni(String valor) {
        String limpio = valor == null ? "" : valor.replaceAll("\\s", "");
        if (!DNI.matcher(limpio).matches()) {
            throw new IllegalArgumentException("dni: el DNI tiene 8 dígitos.");
        }
        if (limpio.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("dni: ese DNI no es válido.");
        }
        return limpio;
    }

    /** Acepta 987654321, 987 654 321 o +51 987654321 y devuelve los 9 dígitos. */
    public static String celular(String valor) {
        String limpio = valor == null ? "" : valor.replaceAll("[\\s\\-()]", "");
        if (limpio.startsWith("+51")) {
            limpio = limpio.substring(3);
        }
        if (!CELULAR_PE.matcher(limpio).matches()) {
            throw new IllegalArgumentException("telefono: ingresa un celular peruano de 9 dígitos que empiece con 9.");
        }
        return limpio;
    }

    /** Enlace opcional a LinkedIn, portafolio o ORCID. Solo http(s). */
    public static String enlace(String valor) {
        String limpio = limpiar(valor);
        if (limpio == null) {
            return null;
        }
        try {
            URI uri = URI.create(limpio);
            String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(esquema.equals("https") || esquema.equals("http")) || uri.getHost() == null || limpio.length() > 300) {
                throw new IllegalArgumentException();
            }
            return limpio;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("enlaceProfesional: usa un enlace completo, por ejemplo https://linkedin.com/in/tu-nombre.");
        }
    }

    /** Verifica tamaño y que el archivo sea realmente un PDF (firma %PDF-), no solo su extensión. */
    public static void documentoPdf(String nombre, long tamano, byte[] inicio) {
        if (tamano <= 0) {
            throw new IllegalArgumentException("documento: adjunta tu CV, título o constancia en PDF.");
        }
        if (tamano > MAX_DOCUMENTO_BYTES) {
            throw new IllegalArgumentException("documento: el PDF puede pesar como máximo 5 MB.");
        }
        boolean firma = inicio != null && inicio.length >= FIRMA_PDF.length;
        for (int i = 0; firma && i < FIRMA_PDF.length; i++) {
            firma = inicio[i] == FIRMA_PDF[i];
        }
        if (!firma) {
            throw new IllegalArgumentException("documento: el archivo debe ser un PDF válido.");
        }
    }

    /** Nombre seguro para guardar y para la cabecera Content-Disposition. */
    public static String nombreArchivo(String original) {
        String base = original == null ? "" : original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N}._ \\-]", "_").strip();
        if (base.isEmpty() || base.equals(".pdf")) {
            base = "documento.pdf";
        }
        if (!base.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            base = base + ".pdf";
        }
        return base.length() > 160 ? base.substring(base.length() - 160) : base;
    }
}
