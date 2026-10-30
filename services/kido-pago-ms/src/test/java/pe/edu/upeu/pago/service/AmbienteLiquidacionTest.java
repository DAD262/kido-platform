package pe.edu.upeu.pago.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.pago.entity.AmbientePago;
import pe.edu.upeu.pago.entity.MovimientoSaldo;

class AmbienteLiquidacionTest {
 @Test void soloProduccionVerificadaEsElegible() {
  var m=new MovimientoSaldo();
  m.setMonto(BigDecimal.TEN); m.setConciliado(true);
  m.setEstado(MovimientoSaldo.EstadoMovimiento.DISPONIBLE);
  m.setFechaCreacion(LocalDateTime.of(2026,1,1,0,0)); m.setFechaDisponible(m.getFechaCreacion());
  var fin=LocalDateTime.of(2026,2,1,0,0);
  assertFalse(LiquidacionService.elegible(m,fin));
  m.setAmbiente(AmbientePago.PRUEBAS); assertFalse(LiquidacionService.elegible(m,fin));
  m.setAmbiente(AmbientePago.PRODUCCION); assertTrue(LiquidacionService.elegible(m,fin));
 }
}
