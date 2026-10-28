package pe.edu.upeu.usuario.dto;
import jakarta.validation.constraints.*;
public record ConfirmarRecuperacionRequest(@NotBlank @Pattern(regexp="[a-fA-F0-9-]{36}") String solicitudId,
 @NotBlank @Pattern(regexp="[0-9]{6}") String codigo,@Size(min=8,max=72) String password) {
 @Override public String toString(){return "ConfirmarRecuperacionRequest[oculto]";}
}
