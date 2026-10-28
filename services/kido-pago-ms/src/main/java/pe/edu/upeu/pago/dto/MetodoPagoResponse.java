package pe.edu.upeu.pago.dto;
import java.util.List;
public record MetodoPagoResponse(String id,String nombre,boolean disponible,String moneda,List<Medio> medios,String ambiente) {
 public record Medio(String id,String nombre,String tipo) {}
}
