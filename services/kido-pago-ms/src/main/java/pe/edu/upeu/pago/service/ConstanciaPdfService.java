package pe.edu.upeu.pago.service;

import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import pe.edu.upeu.pago.entity.ConstanciaCompra;
import java.awt.Color;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ConstanciaPdfService {
 private static final Color BLUE=new Color(48,66,216), INK=new Color(29,39,63), MUTED=new Color(100,113,134), LINE=new Color(225,231,240);
 public byte[] generar(ConstanciaCompra c){
  try(var d=new PDDocument();var out=new ByteArrayOutputStream()){
   var page=new PDPage(PDRectangle.A4);d.addPage(page);
   var normal=font(d,"NotoSans-Regular.ttf");var bold=font(d,"NotoSans-Bold.ttf");
   try(var s=new PDPageContentStream(d,page)){
    rect(s,0,735,595,107,BLUE);text(s,bold,31,43,788,"K",Color.WHITE);rect(s,65,784,6,6,new Color(255,210,50));
    text(s,bold,22,87,790,"kido",Color.WHITE);text(s,normal,10,43,756,"CONSTANCIA DE COMPRA",Color.WHITE);
    String aviso="PRUEBAS".equals(c.getAmbiente())?"OPERACIÓN DE PRUEBA — SIN VALIDEZ TRIBUTARIA":"CONSTANCIA INFORMATIVA — SIN VALIDEZ TRIBUTARIA";
    rect(s,40,692,515,29,new Color(255,245,216));text(s,bold,10,51,702,aviso,new Color(139,85,0));
    label(s,bold,43,670,"NÚMERO DE COMPRA");text(s,bold,15,43,649,c.getCompraCarritoId()==null?"KIDO #"+c.getOrdenId():"CARRITO #"+c.getCompraCarritoId(),INK);
    label(s,bold,325,670,"FECHA DEL PAGO · LIMA");text(s,normal,11,325,650,c.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),INK);
    section(s,bold,613,"01  DATOS DEL COMPRADOR");
    float y=wrap(s,bold,13,51,587,c.getAlumnoNombre(),490,17);
    y=wrap(s,normal,10,51,y-7,"Correo: "+valor(c.getAlumnoEmail()),490,14);
    text(s,normal,10,51,y-7,"DNI: "+valor(c.getAlumnoDocumento()),MUTED);
    text(s,normal,8,51,y-24,"Datos de la cuenta registrados al emitir esta constancia.",MUTED);
    float courseY=Math.min(505,y-47);section(s,bold,courseY,"02  ACCESO ADQUIRIDO");
    float cy=wrap(s,bold,14,51,courseY-27,c.getCursoTitulo(),490,19);
    text(s,normal,10,51,cy-8,c.getDetalles().isEmpty()?"Docente registrado: #"+c.getDocenteId():"Cursos, docentes e importes: detalle en las páginas siguientes.",INK);
    text(s,normal,10,51,cy-24,"Modalidad: no registrada en la orden",MUTED);
    text(s,normal,10,51,cy-40,"Descripción: acceso al curso digital adquirido.",INK);
    float totalsY=cy-68;section(s,bold,totalsY,"03  DETALLE DEL IMPORTE");
    text(s,normal,10,51,totalsY-26,"Precio del acceso",INK);text(s,bold,11,440,totalsY-26,"S/ "+c.getTotal().toPlainString(),INK);
    text(s,normal,9,51,totalsY-45,"Descuentos: no registrados",MUTED);
    text(s,normal,9,51,totalsY-61,"IGV: no determinado; configuración tributaria pendiente.",MUTED);
    rect(s,40,totalsY-108,515,31,new Color(238,241,255));text(s,bold,11,51,totalsY-96,"TOTAL PAGADO · PEN",BLUE);text(s,bold,15,426,totalsY-97,"S/ "+c.getTotal().toPlainString(),BLUE);
    float opY=totalsY-137;section(s,bold,opY,"04  OPERACIÓN VERIFICADA");
    text(s,normal,10,51,opY-24,"Mercado Pago · "+medio(c.getMedioPago()),INK);
    text(s,normal,9,51,opY-42,"Operación: "+c.getOperacion()+" · approved / "+c.getDetalleEstado(),INK);
    text(s,normal,9,51,opY-59,"Soporte: "+valor(c.getSoporte()),MUTED);
    line(s,40,69,555,69);wrap(s,normal,8,43,54,"Esta constancia acredita el registro de la compra en KIDO. No es una boleta ni factura de SUNAT. La activación del acceso se consulta por separado en Mi aprendizaje.",510,11);
   }
   if(!c.getDetalles().isEmpty()){
    PDPageContentStream detalle=null;float y=0;
    try{for(var item:c.getDetalles()){
      if(detalle==null||y<160){if(detalle!=null)detalle.close();var nueva=new PDPage(PDRectangle.A4);d.addPage(nueva);detalle=new PDPageContentStream(d,nueva);section(detalle,bold,780,"DETALLE DE CURSOS · COMPRA ÚNICA");y=740;}
      y=wrap(detalle,bold,12,48,y,item.getTitulo(),490,18);
      text(detalle,normal,10,48,y-5,"Curso #"+item.getCursoId()+" · docente #"+item.getDocenteId()+" · orden #"+item.getOrdenId(),MUTED);
      text(detalle,bold,11,48,y-26,"S/ "+item.getImporte().toPlainString(),INK);line(detalle,43,y-42,550,y-42);y-=70;
    }}finally{if(detalle!=null)detalle.close();}
   }
   d.save(out);return out.toByteArray();
  }catch(IOException e){throw new IllegalStateException("No se pudo generar la constancia PDF",e);}
 }
 public static String medio(String id){return switch(id){case "account_money"->"Dinero disponible en Mercado Pago";case "visa","debvisa"->"Visa";case "master","debmaster"->"Mastercard";case "amex"->"American Express";case "yape"->"Yape";case "pagoefectivo_atm"->"PagoEfectivo";default->id;};}
 private static String valor(String s){return s==null||s.isBlank()?"No registrado":s;}
 private PDType0Font font(PDDocument d,String n)throws IOException{try(var in=getClass().getResourceAsStream("/fonts/"+n)){if(in==null)throw new IOException("Fuente no disponible");return PDType0Font.load(d,in);}}
 private static void text(PDPageContentStream s,PDFont f,float size,float x,float y,String t,Color c)throws IOException{s.beginText();s.setFont(f,size);s.setNonStrokingColor(c);s.newLineAtOffset(x,y);s.showText(t.replaceAll("[\\p{Cntrl}]"," "));s.endText();}
 private static void rect(PDPageContentStream s,float x,float y,float w,float h,Color c)throws IOException{s.setNonStrokingColor(c);s.addRect(x,y,w,h);s.fill();}
 private static void line(PDPageContentStream s,float x,float y,float x2,float y2)throws IOException{s.setStrokingColor(LINE);s.moveTo(x,y);s.lineTo(x2,y2);s.stroke();}
 private static void label(PDPageContentStream s,PDFont f,float x,float y,String t)throws IOException{text(s,f,8,x,y,t,MUTED);}
 private static void section(PDPageContentStream s,PDFont f,float y,String t)throws IOException{rect(s,40,y-5,515,21,new Color(243,246,251));text(s,f,9,51,y+2,t,BLUE);}
 private static float wrap(PDPageContentStream s,PDFont f,float size,float x,float y,String txt,float width,float height)throws IOException{String line="";for(String word:txt.replaceAll("[\\p{Cntrl}]"," ").split("\\s+")){String next=line.isEmpty()?word:line+" "+word;if(f.getStringWidth(next)*size/1000>width&&!line.isEmpty()){text(s,f,size,x,y,line,INK);y-=height;line=word;}else line=next;}if(!line.isEmpty()){text(s,f,size,x,y,line,INK);y-=height;}return y;}
}
