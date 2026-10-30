package pe.edu.upeu.usuario.controller;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.usuario.repository.PerfilRepository;
import pe.edu.upeu.usuario.entity.TipoCuenta;
import java.util.List;

/** Solo identidad pública del docente verificado; excluye documentos y contacto. */
@RestController @RequestMapping("/api/v1/usuarios/docentes-publicos")
public class DocentesPublicosController {
 private final PerfilRepository perfiles;
 public DocentesPublicosController(PerfilRepository perfiles){this.perfiles=perfiles;}
 public record Docente(Long id,String nombre) {}
 @GetMapping public List<Docente> listar(){return perfiles.findAll().stream().filter(p->p.getTipoRegistro()==TipoCuenta.DOCENTE&&p.isDocenteVerificado()).map(p->new Docente(p.getId(),(p.getNombres()+" "+p.getApellidos()).strip())).toList();}
}
