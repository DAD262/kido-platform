package pe.edu.upeu.curso.dto;

import java.math.BigDecimal;
import java.util.List;

public record CursoCompraResponse(
        Long id, String titulo, String tipo, BigDecimal precio, Long docenteId,
        String estado, List<Long> leccionIds
) {}
