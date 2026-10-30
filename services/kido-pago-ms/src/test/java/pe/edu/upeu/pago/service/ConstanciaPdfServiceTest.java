package pe.edu.upeu.pago.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import pe.edu.upeu.pago.entity.ConstanciaCompra;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.nio.file.*;
import javax.imageio.ImageIO;

class ConstanciaPdfServiceTest {
 @Test void pdfEsLegibleYNoExponeComisionNiSimulaFactura()throws Exception{
  var c=new ConstanciaCompra();c.setOrdenId(29L);c.setNumero("KIDO-29-OPERACION-DE-EJEMPLO");c.setOperacion("OPERACION-DE-EJEMPLO");c.setAmbiente("PRUEBAS");c.setAlumnoNombre("María José Álvarez Muñoz (ejemplo)");c.setAlumnoEmail("ejemplo@example.invalid");c.setAlumnoDocumento("");c.setCursoTitulo("Fundamentos de Python 2.0: aplicaciones prácticas para aprender programación");c.setDocenteId(1L);c.setFechaPago(LocalDateTime.of(2026,10,29,11,29));c.setMedioPago("account_money");c.setDetalleEstado("accredited");c.setTotal(new BigDecimal("50.00"));c.setSoporte("");
  byte[] bytes=new ConstanciaPdfService().generar(c);try(var pdf=Loader.loadPDF(bytes)){assertEquals(1,pdf.getNumberOfPages());String texto=new PDFTextStripper().getText(pdf);assertTrue(texto.contains("SIN VALIDEZ TRIBUTARIA"));assertTrue(texto.contains("María José"));assertTrue(texto.contains("50.00"));assertFalse(texto.toLowerCase().contains("comisión"));assertFalse(texto.contains("20 %"));var dir=Path.of("target/revision-constancia");Files.createDirectories(dir);Files.write(dir.resolve("ejemplo.pdf"),bytes);ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0,140),"png",dir.resolve("ejemplo.png").toFile());}
 }
}
