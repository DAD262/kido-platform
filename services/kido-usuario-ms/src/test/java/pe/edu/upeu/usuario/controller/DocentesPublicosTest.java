package pe.edu.upeu.usuario.controller;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.usuario.entity.*;
import pe.edu.upeu.usuario.repository.PerfilRepository;
import java.util.List;
class DocentesPublicosTest {
 @Test void publicaSoloNombreDelDocenteVerificado(){var repo=mock(PerfilRepository.class);var docente=new Perfil("d","docente","privado@example.invalid","Braulio","Cornejo",TipoCuenta.DOCENTE);docente.setDocenteVerificado(true);var estudiante=new Perfil("e","alumno","otro@example.invalid","Alumno","Demo",TipoCuenta.ESTUDIANTE);var pendiente=new Perfil("p","pendiente","p@example.invalid","Pendiente","Demo",TipoCuenta.DOCENTE);when(repo.findAll()).thenReturn(List.of(docente,estudiante,pendiente));var rows=new DocentesPublicosController(repo).listar();assertEquals(1,rows.size());assertEquals("Braulio Cornejo",rows.getFirst().nombre());assertEquals(List.of("id","nombre"),java.util.Arrays.stream(DocentesPublicosController.Docente.class.getRecordComponents()).map(x->x.getName()).toList());}
}
