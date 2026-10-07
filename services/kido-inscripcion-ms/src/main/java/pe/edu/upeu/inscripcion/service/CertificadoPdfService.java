package pe.edu.upeu.inscripcion.service;
import org.springframework.stereotype.Service; import pe.edu.upeu.inscripcion.dto.CertificadoResponse; import java.io.*; import java.nio.charset.StandardCharsets; import java.util.*;
@Service
public class CertificadoPdfService {
 public byte[] generar(CertificadoResponse c,Long estudianteId,Long cursoId){
  String[] lineas={"KIDO","CERTIFICADO DE FINALIZACION","Se certifica que el estudiante #"+estudianteId,"ha cumplido los requisitos del curso #"+cursoId,"Codigo: "+c.codigo(),"Emitido: "+(c.fechaEmision()==null?"pendiente":c.fechaEmision().toLocalDate())};
  try{ByteArrayOutputStream out=new ByteArrayOutputStream();List<Integer> off=new ArrayList<>();String header="%PDF-1.4\n";out.write(header.getBytes(StandardCharsets.ISO_8859_1));
   off.add(out.size()); obj(out,1,"<< /Type /Catalog /Pages 2 0 R >>"); off.add(out.size()); obj(out,2,"<< /Type /Pages /Kids [3 0 R] /Count 1 >>"); off.add(out.size()); obj(out,3,"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>");
   StringBuilder content=new StringBuilder("BT /F1 24 Tf 120 470 Td "); for(int i=0;i<lineas.length;i++){if(i>0)content.append("0 -45 Td ");content.append("(").append(esc(lineas[i])).append(") Tj ");} content.append("ET");byte[] cb=content.toString().getBytes(StandardCharsets.ISO_8859_1);
   off.add(out.size());out.write(("4 0 obj\n<< /Length "+cb.length+" >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));out.write(cb);out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1)); off.add(out.size());obj(out,5,"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
   int xref=out.size();out.write(("xref\n0 6\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));for(int o:off)out.write(String.format("%010d 00000 n \n",o).getBytes(StandardCharsets.ISO_8859_1));out.write(("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));return out.toByteArray();
  }catch(IOException e){throw new IllegalStateException("No se pudo generar el certificado",e);}
 }
 private void obj(ByteArrayOutputStream o,int n,String body)throws IOException{o.write((n+" 0 obj\n"+body+"\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));}
 private String esc(String s){return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)").replaceAll("[^\\x20-\\x7E]"," ");}
}
