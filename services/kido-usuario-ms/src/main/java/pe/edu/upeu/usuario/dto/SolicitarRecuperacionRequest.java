package pe.edu.upeu.usuario.dto;
import jakarta.validation.constraints.*;
public record SolicitarRecuperacionRequest(@NotBlank @Email @Size(max=160) String email) { }
