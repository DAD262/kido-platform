package pe.edu.upeu.usuario.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.usuario.entity.GradoAcademico;

/** Lo que un docente entrega para verificar su identidad y trayectoria. */
public record DatosDocenteRequest(
        @NotBlank(message = "Ingresa tu DNI.") String dni,
        @NotBlank(message = "Ingresa un celular de contacto.") String telefono,
        @NotNull(message = "Elige tu grado académico.") GradoAcademico gradoAcademico,
        @NotBlank(message = "Cuéntanos qué enseñarás.") @Size(max = 120, message = "Máximo 120 caracteres.") String especialidad,
        @NotBlank(message = "Indica tu institución o centro de trabajo.") @Size(max = 160, message = "Máximo 160 caracteres.") String institucion,
        @NotNull(message = "Indica tus años de experiencia.") @Min(value = 0, message = "No puede ser negativo.") @Max(value = 60, message = "Revisa los años de experiencia.") Integer aniosExperiencia,
        @Size(max = 300, message = "Máximo 300 caracteres.") String enlaceProfesional,
        @NotBlank(message = "Escribe una breve presentación.") @Size(min = 80, max = 1200, message = "La presentación debe tener entre 80 y 1200 caracteres.") String presentacion,
        @AssertTrue(message = "Debes declarar que la información es verdadera.") boolean declaracionAceptada) {
}
