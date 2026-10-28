package pe.edu.upeu.usuario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.usuario.entity.TipoCuenta;

/** Parte "datos" del registro multipart. {@code docente} es obligatorio solo si tipo = DOCENTE. */
public record RegistroRequest(
        @NotNull(message = "Elige si te registras como estudiante o docente.") TipoCuenta tipo,
        @NotBlank(message = "Ingresa tus nombres.") @Size(max = 80, message = "Máximo 80 caracteres.") String nombres,
        @NotBlank(message = "Ingresa tus apellidos.") @Size(max = 80, message = "Máximo 80 caracteres.") String apellidos,
        @NotBlank(message = "Ingresa tu correo.") @Email(message = "Ese correo no parece válido.") @Size(max = 160, message = "Máximo 160 caracteres.") String email,
        @NotBlank(message = "Ingresa tu DNI.") @jakarta.validation.constraints.Pattern(regexp="[0-9]{8}", message="El DNI debe tener 8 dígitos.") String dni,
        @NotBlank(message = "Crea una contraseña.") String password,
        @AssertTrue(message = "Acepta los términos para continuar.") boolean aceptaTerminos,
        @Valid DatosDocenteRequest docente) {

    /** Evita que la contraseña aparezca en logs si alguien imprime el request. */
    @Override
    public String toString() {
        return "RegistroRequest[tipo=" + tipo + ", email=" + email + "]";
    }
}
