package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;import java.time.LocalDate;import org.junit.jupiter.api.Test;
class CalendarioLiquidacionesTest {
 @Test void dia15NoLiquidaAntesDeCerrar(){assertEquals(LocalDate.of(2026,9,30),CalendarioLiquidaciones.ultimoCorte(LocalDate.of(2026,10,15)));}
 @Test void dia16CierraPrimeraQuincena(){assertEquals(LocalDate.of(2026,10,15),CalendarioLiquidaciones.ultimoCorte(LocalDate.of(2026,10,16)));}
 @Test void primeroDeMesCierraMesAnterior(){assertEquals(LocalDate.of(2028,2,29),CalendarioLiquidaciones.ultimoCorte(LocalDate.of(2028,3,1)));}
 @Test void fechaInvalidaNoEsUnPeriodo(){assertThrows(IllegalArgumentException.class,()->CalendarioLiquidaciones.inicio(LocalDate.of(2026,10,14)));assertEquals(LocalDate.of(2026,10,16),CalendarioLiquidaciones.inicio(LocalDate.of(2026,10,31)));}
}
