package pe.edu.upeu.pago.dto;
import java.math.BigDecimal;import java.time.*;
public record LiquidacionResponse(Long id,Long docenteId,LocalDate desde,LocalDate corte,String estado,BigDecimal monto,BigDecimal baseAcumulada,BigDecimal compromisosAnteriores,Instant fechaCreacion,Instant fechaPago,String banco,String codigoOperacion,LocalDate fechaOperacion,String destino,String titular,String motivoAnulacion,boolean tieneConstancia) {}
