package pe.edu.upeu.pago.dto;

import jakarta.validation.constraints.*;

/** Solicitud de edición: omitir RUC o libro equivale a dejarlo vacío en el borrador. */
public record GuardarComercioRequest(
    @NotNull Long version,
    @NotNull @Size(max=180) String razonSocial,
    @Pattern(regexp="[0-9]{11}|^$") String ruc,
    @NotNull @Size(max=300) String direccion,
    @NotNull @Email @Size(max=160) String emailSoporte,
    @NotNull @Size(max=30000) String terminos,
    @NotNull @Size(max=30000) String privacidad,
    @NotNull @Size(max=30000) String reembolsos,
    @Size(max=500) String reclamacionesUrl,
    boolean publicada) {
  public DatosComerciales aDatos() {
    return new DatosComerciales(version, razonSocial, ruc == null ? "" : ruc, direccion,
        emailSoporte, terminos, privacidad, reembolsos,
        reclamacionesUrl == null ? "" : reclamacionesUrl, publicada);
  }
}
