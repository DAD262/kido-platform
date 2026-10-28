package pe.edu.upeu.pago.entity;
import jakarta.persistence.*;import lombok.*;
@Entity @Table(name="saldo_docente_bloqueos") @Getter @Setter @NoArgsConstructor public class BloqueoSaldo {@Id private Long docenteId;}
