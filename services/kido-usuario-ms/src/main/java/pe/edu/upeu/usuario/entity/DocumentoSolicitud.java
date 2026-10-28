package pe.edu.upeu.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_solicitud")
public class DocumentoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false, unique = true)
    private Long solicitudId;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(nullable = false)
    private long tamano;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "subido_en", nullable = false)
    private LocalDateTime subidoEn;

    protected DocumentoSolicitud() {
    }

    public DocumentoSolicitud(Long solicitudId) {
        this.solicitudId = solicitudId;
    }

    public void reemplazar(String nombre, String tipo, byte[] contenido) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.contenido = contenido;
        this.tamano = contenido.length;
        this.subidoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public long getTamano() { return tamano; }
    public byte[] getContenido() { return contenido; }
    public LocalDateTime getSubidoEn() { return subidoEn; }
}
