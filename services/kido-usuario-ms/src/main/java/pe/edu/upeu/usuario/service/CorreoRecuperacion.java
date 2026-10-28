package pe.edu.upeu.usuario.service;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pe.edu.upeu.usuario.exception.IdentidadNoDisponibleException;
@Component
public class CorreoRecuperacion {
 private final JavaMailSender mail;private final String from;private final String app;
 public CorreoRecuperacion(JavaMailSender mail,@Value("${kido.recuperacion.from:no-reply@kido.local}")String from,
 @Value("${kido.recuperacion.app-url:http://localhost:4200}")String app){this.mail=mail;this.from=from;this.app=app;}
 public void enviar(String email,String id,String codigo){
  SimpleMailMessage msg=new SimpleMailMessage();msg.setFrom(from);msg.setTo(email);msg.setSubject("KIDO - Código para recuperar tu acceso");
  msg.setText("Tu código de recuperación de KIDO es: "+codigo+"\n\nCaduca en 10 minutos y solo puede usarse una vez.\nAbre "+app+"/recuperar?solicitud="+id+" y confirma el código.\n\nEstudiantes: después de verificar el correo, la clave temporal será tu DNI y deberás cambiarla al ingresar.\nDocentes, administradores y supervisores: elige una nueva contraseña en el formulario.\n\nSi no solicitaste recuperar tu acceso, ignora este correo. No compartas el código.");
  try{mail.send(msg);}catch(org.springframework.mail.MailException e){throw new IdentidadNoDisponibleException("No se pudo enviar el correo de recuperación. Intenta más tarde.",null);}
 }
}
