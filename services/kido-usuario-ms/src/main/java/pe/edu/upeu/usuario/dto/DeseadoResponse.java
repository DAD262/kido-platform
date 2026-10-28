package pe.edu.upeu.usuario.dto;

import java.time.OffsetDateTime;

/** Elemento de una lista de deseados propiedad del sub del JWT autenticado. */
public record DeseadoResponse(Long cursoId, OffsetDateTime agregadoEn) { }
