package pe.edu.upeu.usuario.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record SupervisionRequest(@NotNull Boolean habilitada,
    @NotNull @Size(max=100) List<@NotNull @Positive Long> cursos) { }
