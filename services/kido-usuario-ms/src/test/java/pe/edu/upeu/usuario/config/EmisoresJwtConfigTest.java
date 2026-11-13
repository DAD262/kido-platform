package pe.edu.upeu.usuario.config;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EmisoresJwtConfigTest {
 final String local="http://localhost:19090/realms/kido",publico="https://tunel.example";final Instant hasta=Instant.parse("2026-10-30T20:00:00Z");
 @Test void admiteSoloElEmisorPublicoExactoDuranteElPlazo(){assertTrue(EmisoresJwtConfig.permitido(publico+"/auth/realms/kido",local,publico,hasta,hasta.minusSeconds(1)));assertFalse(EmisoresJwtConfig.permitido("https://otro.example/auth/realms/kido",local,publico,hasta,hasta.minusSeconds(1)));}
 @Test void rechazaTokensPublicosDesdeElVencimiento(){assertFalse(EmisoresJwtConfig.permitido(publico+"/auth/realms/kido",local,publico,hasta,hasta));}
 @Test void mantieneAccesoLocalTrasElVencimiento(){assertTrue(EmisoresJwtConfig.permitido(local,local,publico,hasta,hasta.plusSeconds(1)));assertTrue(EmisoresJwtConfig.permitido("http://localhost:4300/auth/realms/kido",local,publico,hasta,hasta.plusSeconds(1)));}
 @Test void noAdmiteOrigenPublicoInseguro(){assertFalse(EmisoresJwtConfig.permitido("http://tunel.example/auth/realms/kido",local,"http://tunel.example",hasta,hasta.minusSeconds(1)));}
}
