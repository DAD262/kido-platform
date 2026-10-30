package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.pago.entity.*;
import pe.edu.upeu.pago.repository.*;
import pe.edu.upeu.pago.client.MercadoPagoClient;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.nio.file.*;
import javax.imageio.ImageIO;
class ConstanciaCarritoTest {
 @Test void unSoloComprobanteDetallaAmbosCursosSinDuplicarElTotal()throws Exception{
  var ordenes=mock(OrdenCompraRepository.class);var snapshots=mock(ConstanciaCompraRepository.class);var comercio=mock(ConfiguracionComercialRepository.class);var mp=mock(MercadoPagoClient.class);var acceso=mock(AccesoCuentaPago.class);var grupos=mock(CompraCarritoRepository.class);
  var grupo=new CompraCarrito();grupo.setId(1L);grupo.setEstudianteId(7L);grupo.setAmbiente(AmbientePago.PRUEBAS);grupo.setReceptorMp("123");grupo.setTotal(new BigDecimal("159.80"));
  var a=linea(101L,12L,"Angular: componentes y aplicaciones","89.90");var b=linea(102L,15L,"Automatización con Python: archivos y reportes","69.90");
  when(ordenes.findById(101L)).thenReturn(Optional.of(a));when(ordenes.findById(102L)).thenReturn(Optional.of(b));when(ordenes.bloquear(101L)).thenReturn(Optional.of(a));when(ordenes.findByCompraCarritoIdOrderByIdAsc(1L)).thenReturn(List.of(a,b));when(grupos.findById(1L)).thenReturn(Optional.of(grupo));
  AtomicReference<ConstanciaCompra> guardada=new AtomicReference<>();when(snapshots.findById("555")).thenAnswer(x->Optional.ofNullable(guardada.get()));when(snapshots.save(any())).thenAnswer(x->{guardada.set(x.getArgument(0));return guardada.get();});
  when(mp.obtenerPago("555")).thenReturn(new MercadoPagoClient.PagoExterno("555","approved","CARRITO-1","159.80","PEN",null,null,null,null,BigDecimal.ZERO,"123",false,"visa","accredited","2026-10-29T16:00:00-05:00"));
  when(acceso.datosComprador(7L)).thenReturn(new AccesoCuentaPago.Persona("Estudiante de prueba","prueba@example.invalid",""));
  var service=new ConstanciaCompraService(ordenes,snapshots,comercio,mp,acceso,new PagoCarritoValidador(grupos,ordenes,mp),grupos);
  var constancia=service.obtener(102L);assertSame(constancia,service.obtener(101L));assertEquals(2,constancia.getDetalles().size());assertEquals(new BigDecimal("159.80"),constancia.getTotal());assertEquals(101L,constancia.getOrdenId());assertEquals(1L,constancia.getCompraCarritoId());verify(snapshots,times(1)).save(any());
  var pdf=new ConstanciaPdfService().generar(constancia);try(var documento=Loader.loadPDF(pdf)){assertEquals(2,documento.getNumberOfPages());String texto=new PDFTextStripper().getText(documento);assertTrue(texto.contains("159.80"));assertTrue(texto.contains("89.90"));assertTrue(texto.contains("69.90"));assertTrue(texto.contains("SIN VALIDEZ TRIBUTARIA"));var dir=Paths.get("target/revision-constancia");Files.createDirectories(dir);Files.write(dir.resolve("carrito.pdf"),pdf);ImageIO.write(new PDFRenderer(documento).renderImageWithDPI(1,110),"png",dir.resolve("carrito-detalle.png").toFile());}
 }
 private OrdenCompra linea(Long id,Long curso,String titulo,String total){var o=new OrdenCompra();o.setId(id);o.setEstudianteId(7L);o.setCursoId(curso);o.setCursoTitulo(titulo);o.setDocenteId(curso);o.setMontoTotal(new BigDecimal(total));o.setAmbiente(AmbientePago.PRUEBAS);o.setReceptorMp("123");o.setCompraCarritoId(1L);o.setMercadoPagoPaymentId("555");o.setEstado(OrdenCompra.EstadoOrden.APROBADA);return o;}
}
