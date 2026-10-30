package pe.edu.upeu.pago.dto;

/**
 * @param checkoutUrl URL a la que el frontend debe redirigir: {@code sandbox_init_point} si se usan
 *     credenciales TEST-..., {@code init_point} en cualquier otro caso (incluidas las credenciales de
 *     una cuenta vendedora de prueba).
 */
public record PreferenciaResponse(String preferenceId, String initPoint, String sandboxInitPoint, String checkoutUrl,boolean retornoConfigurado) {
  public PreferenciaResponse(String preferenceId,String initPoint,String sandboxInitPoint,String checkoutUrl){this(preferenceId,initPoint,sandboxInitPoint,checkoutUrl,false);}
  public PreferenciaResponse(String preferenceId, String initPoint, String sandboxInitPoint) {
    this(preferenceId, initPoint, sandboxInitPoint, null,false);
  }
}
