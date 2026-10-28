package pe.edu.upeu.inscripcion.service;

import org.springframework.stereotype.Service;
import pe.edu.upeu.inscripcion.dto.CertificadoDocumento;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.rendering.*;
import java.awt.Color;
import java.io.*;
import java.util.*;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;

@Service
public class CertificadoPdfService {
 private static final Color NAVY=new Color(10,40,62),GOLD=new Color(187,147,62),INK=new Color(25,46,62);
 public byte[] generar(CertificadoDocumento d){
  try(var pdf=new PDDocument();var out=new ByteArrayOutputStream()){
   var page=new PDPage(new PDRectangle(PDRectangle.A4.getHeight(),PDRectangle.A4.getWidth()));pdf.addPage(page);
   var sans=fuente(pdf,"NotoSans-Regular.ttf");var bold=fuente(pdf,"NotoSans-Bold.ttf");var serif=fuente(pdf,"NotoSerif-Regular.ttf");var italic=fuente(pdf,"NotoSerif-Italic.ttf");
   try(var p=new PDPageContentStream(pdf,page)){
    rect(p,NAVY,0,0,842,596);rect(p,Color.WHITE,20,20,802,556);
    p.setStrokingColor(GOLD);p.setLineWidth(1.3f);p.addRect(13,13,816,569);p.stroke();p.setLineWidth(.6f);p.addRect(29,29,784,537);p.stroke();
    p.setNonStrokingColor(NAVY);p.moveTo(20,20);p.lineTo(220,20);p.lineTo(20,221);p.closePath();p.fill();
    p.setNonStrokingColor(GOLD);p.moveTo(20,20);p.lineTo(251,20);p.lineTo(20,251);p.lineTo(20,222);p.lineTo(222,20);p.closePath();p.fill();
    center(p,bold,28,421,523,"KIDO",NAVY);
    center(p,sans,8,421,504,"APRENDE  ·  CRECE  ·  AVANZA",GOLD);
    center(p,serif,34,421,448,"CERTIFICADO",NAVY);
    center(p,bold,14,421,420,"DE FINALIZACIÓN",NAVY);
    line(p,283,404,559,404,GOLD,1);
    center(p,sans,11,421,373,"Se otorga a",INK);
    textLines(p,italic,d.alumno(),421,334,654,29,2,NAVY);
    line(p,150,289,692,289,GOLD,.8f);
    center(p,sans,11,421,267,"Por cumplir los requisitos de aprendizaje del curso",INK);
    textLines(p,bold,d.curso(),421,233,630,19,2,NAVY);
    center(p,sans,9,421,180,"Reconocemos tu dedicación y tu compromiso con el aprendizaje.",INK);
    center(p,sans,9,421,161,"Emitido digitalmente por KIDO, plataforma de aprendizaje.",INK);
    line(p,168,106,308,106,GOLD,.8f);
    center(p,bold,12,238,116,d.fechaEmision().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",Locale.forLanguageTag("es-PE"))),NAVY);
    center(p,sans,8,238,90,"FECHA DE EMISIÓN",INK);
    line(p,533,106,673,106,GOLD,.8f);center(p,bold,15,603,117,"KIDO",NAVY);center(p,sans,8,603,90,"PLATAFORMA DE APRENDIZAJE",INK);
    sello(p,421,111);center(p,bold,18,421,104,"K",NAVY);
    center(p,sans,8,421,53,"Código: "+d.codigo(),INK);
    center(p,sans,7,421,39,"Constancia de finalización del curso. Conserva este documento y su código de emisión.",INK);
   }
   pdf.getDocumentInformation().setTitle("Certificado KIDO - "+d.curso());pdf.getDocumentInformation().setAuthor("KIDO");pdf.save(out);return out.toByteArray();
  }catch(IOException e){throw new IllegalStateException("No se pudo generar el certificado",e);}
 }
 public byte[] png(byte[] pdf){try(var d=org.apache.pdfbox.Loader.loadPDF(pdf);var out=new ByteArrayOutputStream()){var image=new PDFRenderer(d).renderImageWithDPI(0,200,ImageType.RGB);ImageIO.write(image,"png",out);return out.toByteArray();}catch(IOException e){throw new IllegalStateException("No se pudo exportar el certificado como imagen",e);}}
 private PDType0Font fuente(PDDocument d,String nombre)throws IOException{try(var in=getClass().getResourceAsStream("/fonts/"+nombre)){if(in==null)throw new IOException("Fuente no disponible");return PDType0Font.load(d,in);}}
 private static String limpio(String s){return Objects.toString(s,"").replace((char)10,' ').replace((char)13,' ').strip();}
 private static float ancho(PDFont f,String s,float size)throws IOException{return f.getStringWidth(s)/1000*size;}
 private static void center(PDPageContentStream p,PDFont f,float size,float x,float y,String texto,Color color)throws IOException{String s=limpio(texto);p.beginText();p.setFont(f,size);p.setNonStrokingColor(color);p.newLineAtOffset(x-ancho(f,s,size)/2,y);p.showText(s);p.endText();}
 private static void textLines(PDPageContentStream p,PDFont f,String texto,float x,float y,float width,float initial,int max,Color color)throws IOException{String clean=limpio(texto);float size=initial;java.util.List<String> lines;do{lines=new ArrayList<>();String line="";for(String word:clean.split(" +")){if(!line.isEmpty()&&ancho(f,line+" "+word,size)>width){lines.add(line);line=word;}else line=line.isEmpty()?word:line+" "+word;}if(!line.isEmpty())lines.add(line);boolean fits=lines.size()<=max;for(String v:lines)if(ancho(f,v,size)>width)fits=false;if(fits)break;size-=1;}while(size>=10);for(int i=0;i<lines.size();i++)center(p,f,size,x,y-i*(size+5),lines.get(i),color);}
 private static void rect(PDPageContentStream p,Color c,float x,float y,float w,float h)throws IOException{p.setNonStrokingColor(c);p.addRect(x,y,w,h);p.fill();}
 private static void line(PDPageContentStream p,float x,float y,float xx,float yy,Color c,float w)throws IOException{p.setStrokingColor(c);p.setLineWidth(w);p.moveTo(x,y);p.lineTo(xx,yy);p.stroke();}
 private static void sello(PDPageContentStream p,float x,float y)throws IOException{p.setNonStrokingColor(GOLD);for(int i=0;i<40;i++){double a=Math.PI*2*i/40;float r=i%2==0?31:28;float xx=x+(float)Math.cos(a)*r,yy=y+(float)Math.sin(a)*r;if(i==0)p.moveTo(xx,yy);else p.lineTo(xx,yy);}p.closePath();p.fill();p.setStrokingColor(NAVY);p.setLineWidth(.7f);p.addRect(x-18,y-18,36,36);p.stroke();}
}
