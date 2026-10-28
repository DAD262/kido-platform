package pe.edu.upeu.usuario.dto;
import jakarta.validation.constraints.*;
public record ActualizarCuentaRequest(
    @NotBlank @Size(max=80) String nombres,
    @NotBlank @Size(max=80) String apellidos,
    @NotBlank @Email @Size(max=160) String email,
    @NotNull Boolean habilitada, @Size(max=8) String dni, @Size(min=3,max=160) String username) {
    public ActualizarCuentaRequest(String nombres,String apellidos,String email,Boolean habilitada) {this(nombres,apellidos,email,habilitada,null,null);}
    public ActualizarCuentaRequest(String nombres,String apellidos,String email,Boolean habilitada,String dni) {this(nombres,apellidos,email,habilitada,dni,null);}
}
