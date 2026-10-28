package pe.edu.upeu.pago.dto;
import jakarta.validation.constraints.*;
public record DatosComerciales(@NotNull Long version,
 @NotNull @Size(max=180) String razonSocial,@NotNull @Pattern(regexp="[0-9]{11}|^$") String ruc,
 @NotNull @Size(max=300) String direccion,@NotNull @Email @Size(max=160) String emailSoporte,
 @NotNull @Size(max=30000) String terminos,@NotNull @Size(max=30000) String privacidad,
 @NotNull @Size(max=30000) String reembolsos,@NotNull @Size(max=500) String reclamacionesUrl, boolean publicada) {}
