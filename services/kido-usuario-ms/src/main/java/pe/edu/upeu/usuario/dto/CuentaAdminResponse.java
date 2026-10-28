package pe.edu.upeu.usuario.dto;
import java.util.List;
public record CuentaAdminResponse(String id, String username, String email, String nombres,
        String apellidos, boolean habilitada, List<String> roles, boolean sinAcceso, Long perfilId, String dni) { }
