package pe.edu.upeu.usuario.controller;
import java.time.*;import java.util.Map;import jakarta.servlet.http.HttpServletRequest;import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;import pe.edu.upeu.usuario.dto.*;import pe.edu.upeu.usuario.service.RecuperacionService;
import pe.edu.upeu.usuario.support.LimitadorIntentos;import pe.edu.upeu.usuario.exception.DemasiadosIntentosException;
@RestController @RequestMapping("/api/v1/usuarios/recuperacion")
public class RecuperacionController {
 @org.springframework.beans.factory.annotation.Value("${spring.mail.host}") private String smtpHost;
 @GetMapping("/configuracion") public Map<String,Boolean> configuracion(){return Map.of("bandejaLocal","kido-mailpit".equals(smtpHost));}
 private final RecuperacionService service;private final LimitadorIntentos solicitudes;private final LimitadorIntentos verificaciones;
 public RecuperacionController(RecuperacionService service,Clock clock){this.service=service;solicitudes=new LimitadorIntentos(30,Duration.ofMinutes(15),clock);verificaciones=new LimitadorIntentos(120,Duration.ofMinutes(15),clock);}
 @PostMapping("/solicitar") public Map<String,String> solicitar(@Valid @RequestBody SolicitarRecuperacionRequest r,HttpServletRequest req){
  if(!solicitudes.permitir(req.getRemoteAddr()))throw new DemasiadosIntentosException("Demasiadas solicitudes. Espera unos minutos.");
  return Map.of("solicitudId",service.solicitar(r.email()),"mensaje","Si existe una cuenta activa con ese correo, recibirás un código. Revisa también el correo no deseado.");
 }
 @PostMapping("/confirmar") public Map<String,Object> confirmar(@Valid @RequestBody ConfirmarRecuperacionRequest r,HttpServletRequest req){
  if(!verificaciones.permitir(req.getRemoteAddr()))throw new DemasiadosIntentosException("Demasiados intentos. Espera unos minutos.");
  boolean temporal=service.confirmar(r);
  return Map.of("temporalDni",temporal,"mensaje",temporal?"Acceso recuperado. Ingresa con tu correo y tu DNI como contraseña temporal. Deberás cambiarla al ingresar.":"Contraseña actualizada. Ya puedes ingresar con tu nueva clave.");
 }
}
