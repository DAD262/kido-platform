package pe.edu.upeu.pago.controller;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.pago.service.*;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import pe.edu.upeu.pago.webhook.ValidadorWebhookMercadoPago;
@ExtendWith(MockitoExtension.class)
class WebhookPagoTest {
  @Mock PagoService service;
  @Mock CertificadoPagoService certificados;
  @Mock AccesoCuentaPago acceso;
  @Mock MercadoPagoClient mp;
  @Mock ValidadorWebhookMercadoPago validador;
  @InjectMocks PagoController controller;
  @Test void sinFirmaEsRechazadoSiWebhookEstaConfigurado(){when(validador.activo()).thenReturn(true);assertEquals(401,controller.webhook("payment",null,"123",null,null,"req",null).getStatusCode().value());verifyNoInteractions(service);}
  @Test void firmaInvalidaNoConsultaProveedor(){when(validador.activo()).thenReturn(true);when(validador.aceptar("invalida","req","123")).thenReturn(false);assertEquals(401,controller.webhook("payment",null,"123",null,"invalida","req",null).getStatusCode().value());verifyNoInteractions(service);}
  @Test void firmaValidaProcesaPago(){when(validador.activo()).thenReturn(true);when(validador.aceptar("valida","req","123")).thenReturn(true);assertEquals(200,controller.webhook("payment",null,"123",null,"valida","req",null).getStatusCode().value());verify(service).procesarWebhook("123");}
}
