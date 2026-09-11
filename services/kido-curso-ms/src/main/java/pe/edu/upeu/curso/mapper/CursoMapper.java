package pe.edu.upeu.curso.mapper;

import org.mapstruct.*;
import pe.edu.upeu.curso.dto.*;
import pe.edu.upeu.curso.entity.Curso;

@Mapper(componentModel = "spring")
public interface CursoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Curso toEntity(CursoRequest request);

    @Mapping(target = "tipo", expression = "java(curso.getTipo().name())")
    @Mapping(target = "estado", expression = "java(curso.getEstado().name())")
    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoria", source = "categoria.nombre")
    CursoResponse toResponse(Curso curso);
}
