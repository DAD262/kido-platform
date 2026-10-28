package pe.edu.upeu.pago.dto;
import java.util.List;
public record EstadoComercial(DatosComerciales datos,boolean lista,List<String> pendientes) {}
