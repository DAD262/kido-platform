package pe.edu.upeu.pago.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="configuracion_comercial") @Getter @Setter @NoArgsConstructor
public class ConfiguracionComercial {
 @Id private Long id;
 @Version private Long version;
 @Column(nullable=false,length=180) private String razonSocial="";
 @Column(nullable=false,length=11) private String ruc="";
 @Column(nullable=false,length=300) private String direccion="";
 @Column(nullable=false,length=160) private String emailSoporte="";
 @Column(nullable=false,columnDefinition="text") private String terminos="";
 @Column(nullable=false,columnDefinition="text") private String privacidad="";
 @Column(nullable=false,columnDefinition="text") private String reembolsos="";
 @Column(nullable=false,length=500) private String reclamacionesUrl="";
 @Column(nullable=false) private boolean publicada;
}
