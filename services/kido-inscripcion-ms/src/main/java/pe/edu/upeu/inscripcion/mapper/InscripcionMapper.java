package pe.edu.upeu.inscripcion.mapper;

import org.mapstruct.*;
import pe.edu.upeu.inscripcion.dto.*;
import pe.edu.upeu.inscripcion.entity.*;

@Mapper(componentModel="spring")
public interface InscripcionMapper {
    ProgresoResponse toResponse(ProgresoLeccion progreso);
    @Mapping(target="tipoAcceso",expression="java(i.getTipoAcceso().name())")
    @Mapping(target="estado",expression="java(i.getEstado().name())")
    @Mapping(target="porcentajeProgreso",expression="java(porcentaje(i))")
    InscripcionResponse toResponse(Inscripcion i);
    default int porcentaje(Inscripcion i) {
        if(i.getProgresos().isEmpty()) return 0;
        long completas=i.getProgresos().stream().filter(ProgresoLeccion::isCompletada).count();
        return (int)Math.round(completas*100.0/i.getProgresos().size());
    }
}
