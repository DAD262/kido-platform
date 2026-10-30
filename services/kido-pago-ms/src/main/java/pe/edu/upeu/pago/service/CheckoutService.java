package pe.edu.upeu.pago.service;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.beans.factory.annotation.Value;
import pe.edu.upeu.pago.client.MercadoPagoClient; import pe.edu.upeu.pago.dto.*; import java.util.*;
@Service @RequiredArgsConstructor
public class CheckoutService {
 private final MercadoPagoClient mp; private final ComercioService comercio;
 @Value("${kido.mercadopago.ambiente:pruebas}") private String ambiente;
 @Value("${kido.mercadopago.return-url:}") private String retorno;
 @Value("${kido.mercadopago.notification-url:}") private String notificacion;
 @Value("${kido.mercadopago.webhook-secret:}") private String firma;
 @Value("${kido.mercadopago.modelo-recaudacion-confirmado:false}") private boolean modeloConfirmado;
 @Value("${KIDO_CHECKOUT_PRUEBA_LOCAL:false}") private boolean habilitarPruebaLocal;
 @Value("${KIDO_ENTORNO_DESARROLLO_LOCAL:false}") private boolean desarrolloLocal;
 private List<MetodoPagoResponse.Medio> cache=List.of(); private long vence;
 public record Activacion(String ambiente,boolean activa,List<String> pendientes) {}
 private boolean retornoLocal(){try{var u=java.net.URI.create(retorno);return "http".equals(u.getScheme())&&Set.of("localhost","127.0.0.1","::1").contains(u.getHost());}catch(Exception e){return false;}}
 public boolean excepcionLocal(){if(!desarrolloLocal||!habilitarPruebaLocal||!"pruebas".equals(ambiente)||!(retornoLocal()||ComercioService.https(retorno)))return false;try{return mp.verificarCuentaConfigurada().ambiente()==pe.edu.upeu.pago.entity.AmbientePago.PRUEBAS;}catch(RuntimeException e){return false;}}
 public Activacion activacion(){var pendientes=new ArrayList<String>();if(!mp.disponible())pendientes.add("Conectar la cuenta de Mercado Pago");if(!Set.of("pruebas","produccion").contains(ambiente))pendientes.add("Elegir un ambiente válido");if("produccion".equals(ambiente)){if(!modeloConfirmado)pendientes.add("Confirmar con Mercado Pago el modelo de recaudación y liquidación a docentes");if(mp.credencialDePrueba())pendientes.add("Usar credenciales de producción");if(!ComercioService.https(retorno))pendientes.add("Publicar KIDO con HTTPS y configurar el retorno");if(!ComercioService.https(notificacion))pendientes.add("Configurar el webhook HTTPS público");if(firma==null||firma.isBlank())pendientes.add("Configurar la firma del webhook");}boolean local=excepcionLocal();if("pruebas".equals(ambiente)&&habilitarPruebaLocal&&!local)pendientes.add("Verificar cuenta de prueba y retorno local");boolean comercial=comercio.obtener(true).lista();boolean activa=pendientes.isEmpty()&&(comercial||local);if(!comercial)pendientes.add("Completar y publicar la información comercial para producción");return new Activacion(ambiente,activa,List.copyOf(pendientes));}
 public void exigirActivacion(){if(!activacion().activa())throw new IllegalStateException("Los pagos aún no están habilitados. Contacta con KIDO antes de intentar comprar.");}
 public synchronized List<MetodoPagoResponse> metodos(){var estado=activacion();boolean activo=estado.activa();List<MetodoPagoResponse.Medio> medios=List.of();if(activo){try{if(System.currentTimeMillis()>=vence){cache=mp.mediosDisponibles();vence=System.currentTimeMillis()+300000;}medios=cache;activo=!medios.isEmpty();}catch(org.springframework.web.client.RestClientException|IllegalStateException e){activo=false;}}return List.of(new MetodoPagoResponse("mercado-pago","Mercado Pago",activo,"PEN",medios,ambiente,activo&&excepcionLocal()&&!comercio.obtener(true).lista()));}
}
