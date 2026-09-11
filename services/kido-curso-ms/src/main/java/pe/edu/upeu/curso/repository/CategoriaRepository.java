package pe.edu.upeu.curso.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.curso.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {}
