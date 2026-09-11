package pe.edu.upeu.curso.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.curso.dto.*;
import pe.edu.upeu.curso.entity.Curso;
import pe.edu.upeu.curso.exception.ResourceNotFoundException;
import pe.edu.upeu.curso.mapper.CursoMapper;
import pe.edu.upeu.curso.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CursoService {
    private final CursoRepository cursoRepository;
    private final CategoriaRepository categoriaRepository;
    private final LeccionRepository leccionRepository;
    private final CursoMapper mapper;

    @Transactional(readOnly = true)
    public List<CursoResponse> listar() {
        return cursoRepository.findAllConCategoria().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CursoResponse obtener(Long id) { return mapper.toResponse(buscar(id)); }


    @Transactional(readOnly = true)
    public CursoCompraResponse resumenCompra(Long id) {
        Curso curso = buscar(id);
        return new CursoCompraResponse(
                curso.getId(), curso.getTitulo(), curso.getTipo().name(), curso.getPrecio(),
                curso.getDocenteId(), curso.getEstado().name(),
                leccionRepository.findIdsByCursoId(id)
        );
    }

    @Transactional
    public CursoResponse crear(CursoRequest request) {
        Curso curso = mapper.toEntity(request);
        aplicar(request, curso);
        curso.setFechaCreacion(LocalDateTime.now());
        return mapper.toResponse(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest request) {
        Curso curso = buscar(id);
        curso.setTitulo(request.titulo());
        curso.setDescripcion(request.descripcion());
        curso.setDocenteId(request.docenteId());
        aplicar(request, curso);
        return mapper.toResponse(cursoRepository.save(curso));
    }

    @Transactional
    public void eliminar(Long id) {
        Curso curso = buscar(id);
        if (curso.getEstado() == Curso.EstadoCurso.PUBLICADO) {
            throw new IllegalArgumentException("Un curso publicado debe archivarse antes de eliminarse");
        }
        cursoRepository.delete(curso);
    }

    private void aplicar(CursoRequest r, Curso c) {
        try {
            c.setTipo(Curso.TipoCurso.valueOf(r.tipo().toUpperCase()));
            c.setEstado(r.estado() == null ? Curso.EstadoCurso.BORRADOR :
                    Curso.EstadoCurso.valueOf(r.estado().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo o estado de curso no válido");
        }
        if (c.getTipo() == Curso.TipoCurso.GRATUITO && r.precio().compareTo(BigDecimal.ZERO) != 0)
            throw new IllegalArgumentException("Un curso GRATUITO debe tener precio 0");
        if (c.getTipo() == Curso.TipoCurso.PAGO && r.precio().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Un curso PAGO debe tener precio mayor que 0");
        c.setPrecio(r.precio());
        c.setCategoria(categoriaRepository.findById(r.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + r.categoriaId())));
    }

    private Curso buscar(Long id) {
        return cursoRepository.findByIdConCategoria(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + id));
    }
}
