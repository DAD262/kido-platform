package pe.edu.upeu.curso.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CursoResponse(
        Long id, String titulo, String descripcion, String tipo, BigDecimal precio,
        Long docenteId, String estado, Long categoriaId, String categoria,
        LocalDateTime fechaCreacion
) {}
