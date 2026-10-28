package pe.edu.upeu.pago.service;
import java.time.*;
public final class CalendarioLiquidaciones {
 private CalendarioLiquidaciones(){}
 public static LocalDate ultimoCorte(LocalDate hoy){return hoy.getDayOfMonth()>=16?hoy.withDayOfMonth(15):hoy.minusMonths(1).withDayOfMonth(hoy.minusMonths(1).lengthOfMonth());}
 public static LocalDate inicio(LocalDate corte){if(corte.getDayOfMonth()!=15&&corte.getDayOfMonth()!=corte.lengthOfMonth())throw new IllegalArgumentException("El corte debe ser el día 15 o el último día del mes");return corte.withDayOfMonth(corte.getDayOfMonth()==15?1:16);}
}
