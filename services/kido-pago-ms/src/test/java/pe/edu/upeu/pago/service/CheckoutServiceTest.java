package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import java.util.List;import org.junit.jupiter.api.*;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upeu.pago.client.MercadoPagoClient;import pe.edu.upeu.pago.dto.*;
@ExtendWith(MockitoExtension.class) class CheckoutServiceTest {
 @Mock MercadoPagoClient mp;@Mock ComercioService comercio;@InjectMocks CheckoutService checkout;
 @BeforeEach void setup(){ReflectionTestUtils.setField(checkout,"ambiente","produccion");ReflectionTestUtils.setField(checkout,"modeloConfirmado",true);ReflectionTestUtils.setField(checkout,"retorno","http://localhost:4200/pago/resultado");ReflectionTestUtils.setField(checkout,"notificacion","");ReflectionTestUtils.setField(checkout,"firma","");when(comercio.obtener(true)).thenReturn(new EstadoComercial(null,true,List.of()));}
 @Test void produccionSinHttpsNiFirmaNoCobra(){when(mp.disponible()).thenReturn(true);assertFalse(checkout.activacion().activa());assertThrows(IllegalStateException.class,()->checkout.exigirActivacion());verify(mp,never()).mediosDisponibles();}
 @Test void sinCredencialesNoPublicitaMedios(){var metodo=checkout.metodos().getFirst();assertFalse(metodo.disponible());assertTrue(metodo.medios().isEmpty());verify(mp,never()).mediosDisponibles();}
 @Test void soloMuestraCatalogoDelProveedorYCachaRespuesta(){when(mp.disponible()).thenReturn(true);ReflectionTestUtils.setField(checkout,"retorno","https://kido.example/pago/resultado");ReflectionTestUtils.setField(checkout,"notificacion","https://kido.example/api/v1/pagos/webhooks/mercado-pago");ReflectionTestUtils.setField(checkout,"firma","firma-unit-test");when(mp.mediosDisponibles()).thenReturn(List.of(new MetodoPagoResponse.Medio("visa","Visa","credit_card")));assertTrue(checkout.metodos().getFirst().disponible());assertEquals("visa",checkout.metodos().getFirst().medios().getFirst().id());verify(mp,times(1)).mediosDisponibles();}
}
