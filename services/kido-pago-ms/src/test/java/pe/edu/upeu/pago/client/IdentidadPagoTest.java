package pe.edu.upeu.pago.client;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.pago.entity.AmbientePago;
import static org.mockito.Mockito.*;

class IdentidadPagoTest {
 @Test void cuentaSoloEsPruebaConEtiquetaDelProveedor(){
  assertEquals(AmbientePago.PRUEBAS,MercadoPagoClient.interpretarCuenta(Map.of("id",123,"site_id","MPE","tags",List.of("test_user"))).ambiente());
  assertEquals(AmbientePago.PRODUCCION,MercadoPagoClient.interpretarCuenta(Map.of("id",123,"site_id","MPE","tags",List.of())).ambiente());
  assertThrows(IllegalStateException.class,()->MercadoPagoClient.interpretarCuenta(Map.of("id",123,"site_id","MPE")));
 }
 @Test void pagoDebeCoincidirEnReceptorYAmbiente(){
  var p=new MercadoPagoClient.PagoExterno("1","approved","42","10","PEN",null,null,null,null,null,"123",false);
  assertDoesNotThrow(()->p.validarIdentidad("123",AmbientePago.PRUEBAS));
  assertThrows(IllegalArgumentException.class,()->p.validarIdentidad("999",AmbientePago.PRUEBAS));
  assertThrows(IllegalArgumentException.class,()->p.validarIdentidad("123",AmbientePago.PRODUCCION));
  assertThrows(IllegalArgumentException.class,()->p.validarIdentidad("123",AmbientePago.DESCONOCIDO));
  assertThrows(IllegalArgumentException.class,()->new MercadoPagoClient.PagoExterno("1","approved","42","10","PEN").validarIdentidad("123",AmbientePago.PRUEBAS));
 }
 @Test void liveModeEnCuentaDePruebaExigeVendedorVerificado(){
  var mp=spy(new MercadoPagoClient(null));
  var p=new MercadoPagoClient.PagoExterno("1","approved","28","79.90","PEN",null,null,null,null,null,"123",true);
  doReturn(new MercadoPagoClient.CuentaVerificada("123",AmbientePago.PRUEBAS)).when(mp).verificarCuentaConfigurada();
  assertDoesNotThrow(()->mp.validarPago(p,"123",AmbientePago.PRUEBAS));
  assertThrows(IllegalArgumentException.class,()->mp.validarPago(p,"456",AmbientePago.PRUEBAS));
  doReturn(new MercadoPagoClient.CuentaVerificada("123",AmbientePago.PRODUCCION)).when(mp).verificarCuentaConfigurada();
  assertThrows(IllegalArgumentException.class,()->mp.validarPago(p,"123",AmbientePago.PRUEBAS));
 }
}
