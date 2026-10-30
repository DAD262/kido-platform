package pe.edu.upeu.pago.client;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoClientTest {
  MercadoPagoClient client; MockRestServiceServer server;
  @BeforeEach void setup() {
    var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");
    server=MockRestServiceServer.bindTo(builder).build(); client=new MercadoPagoClient(builder.build());
    ReflectionTestUtils.setField(client,"token","TEST-solo-para-pruebas");
    ReflectionTestUtils.setField(client,"notificationUrl","https://kido.example/webhook");
    ReflectionTestUtils.setField(client,"returnUrl","https://kido.example/pago/resultado");
    ReflectionTestUtils.setField(client,"autoReturn",true);
    ReflectionTestUtils.setField(client,"statementDescriptor","KIDO");
  }
  @Test void preferenciaEnSolesConReferenciaYRetornoUsaSandbox() {
    server.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
      .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer TEST-solo-para-pruebas"))
      .andExpect(jsonPath("$.items[0].currency_id").value("PEN"))
      .andExpect(jsonPath("$.items[0].unit_price").value(79.90))
      .andExpect(jsonPath("$.external_reference").value("42"))
      .andExpect(jsonPath("$.back_urls.success").value("https://kido.example/pago/resultado"))
      .andExpect(jsonPath("$.back_urls.pending").value("https://kido.example/pago/resultado"))
      .andExpect(jsonPath("$.back_urls.failure").value("https://kido.example/pago/resultado"))
      .andExpect(jsonPath("$.auto_return").value("approved"))
      .andExpect(jsonPath("$.notification_url").value("https://kido.example/webhook"))
      .andRespond(withSuccess("{\"id\":\"pref-42\",\"init_point\":\"https://www.mercadopago.com.pe/checkout\",\"sandbox_init_point\":\"https://sandbox.mercadopago.com.pe/checkout\"}",MediaType.APPLICATION_JSON));
    var result=client.crearPreferencia("Curso Spring",new BigDecimal("79.90"),"42");
    assertEquals("pref-42",result.preferenceId());assertTrue(result.checkoutUrl().contains("sandbox."));server.verify();
  }
  @Test void sinTokenNoLlamaProveedor() {ReflectionTestUtils.setField(client,"token","");assertFalse(client.disponible());assertThrows(IllegalStateException.class,()->client.crearPreferencia("Curso",BigDecimal.TEN,"1"));server.verify();}
  @Test void noAceptaIdDePagoConRuta() {assertThrows(IllegalArgumentException.class,()->client.obtenerPago("../users"));server.verify();}
  @Test void respuestaSinEnlaceNoSeAcepta() {server.expect(requestTo("https://api.mercadopago.com/checkout/preferences")).andRespond(withSuccess("{\"id\":\"42\"}",MediaType.APPLICATION_JSON));assertThrows(IllegalStateException.class,()->client.crearPreferencia("Curso",BigDecimal.TEN,"42"));}
  @Test void consultaPagoIncluyeMonedaYValidaImporte() {
    server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
      .andRespond(withSuccess("{\"id\":123,\"status\":\"approved\",\"external_reference\":\"42\",\"transaction_amount\":79.9,\"currency_id\":\"PEN\"}",MediaType.APPLICATION_JSON));
    var pago=client.obtenerPago("123");assertEquals("123",pago.id());pago.validarImporte(new BigDecimal("79.90"));server.verify();
  }
  @Test void rechazaMonedaDistinta() {assertThrows(IllegalArgumentException.class,()->new MercadoPagoClient.PagoExterno("1","approved","42","79.90","USD").validarImporte(new BigDecimal("79.90")));}
  @Test void rechazaImporteAusenteOIncorrecto() {
    for(String monto:new String[]{null,"0","79.89"})assertThrows(IllegalArgumentException.class,()->new MercadoPagoClient.PagoExterno("1","approved","42",monto,"PEN").validarImporte(new BigDecimal("79.90")));
  }
  @Test void catalogoOmiteMediosInactivosYNoIntegrados() {
    server.expect(requestTo("https://api.mercadopago.com/v1/payment_methods")).andRespond(withSuccess("[{\"id\":\"visa\",\"name\":\"Visa\",\"payment_type_id\":\"credit_card\",\"status\":\"active\"},{\"id\":\"yape\",\"name\":\"Yape\",\"payment_type_id\":\"digital_currency\",\"status\":\"inactive\"},{\"id\":\"otro\",\"name\":\"Otro\",\"payment_type_id\":\"unknown\",\"status\":\"active\"}]",MediaType.APPLICATION_JSON));
    var medios=client.mediosDisponibles();assertEquals(1,medios.size());assertEquals("visa",medios.getFirst().id());server.verify();
  }
 @Test void costosNoIncluyenComisionesPagadasPorElAlumno(){server.expect(requestTo("https://api.mercadopago.com/v1/payments/123")).andRespond(withSuccess("{\"id\":123,\"status\":\"approved\",\"external_reference\":\"42\",\"transaction_amount\":100,\"currency_id\":\"PEN\",\"fee_details\":[{\"amount\":5,\"fee_payer\":\"collector\"},{\"amount\":3,\"fee_payer\":\"payer\"}],\"transaction_details\":{\"net_received_amount\":95},\"money_release_status\":\"pending\"}",MediaType.APPLICATION_JSON));var pago=client.obtenerPago("123");assertEquals(new BigDecimal("5.00"),pago.costoProcesamiento());assertEquals(new BigDecimal("95"),pago.netoRecibido());assertEquals("pending",pago.fondosEstado());server.verify();}
}
