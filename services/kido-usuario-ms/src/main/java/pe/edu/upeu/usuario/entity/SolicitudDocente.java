package pe.edu.upeu.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_docente")
public class SolicitudDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false, unique = true)
    private Perfil perfil;

    @Column(nullable = false, length = 8)
    private String dni;

    @Column(nullable = false, length = 15)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "grado_academico", nullable = false, length = 20)
    private GradoAcademico gradoAcademico;

    @Column(nullable = false, length = 120)
    private String especialidad;

    @Column(nullable = false, length = 160)
    private String institucion;

    @Column(name = "anios_experiencia", nullable = false)
    private int aniosExperiencia;

    @Column(name = "enlace_profesional", length = 300)
    private String enlaceProfesional;

    @Column(nullable = false, length = 1200)
    private String presentacion;

    @Column(name = "declaracion_aceptada", nullable = false)
    private boolean declaracionAceptada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "comentario_revision", length = 600)
    private String comentarioRevision;

    @Column(name = "revisado_por", length = 160)
    private String revisadoPor;

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn;

    @Column(nullable = false)
    private int intentos;

    protected SolicitudDocente() {
    }

    public SolicitudDocente(Perfil perfil) {
        this.perfil = perfil;
        this.intentos = 0;
    }

    /** Copia los datos del formulario y deja la solicitud lista para revisión. */
    public void enviar(DatosDocente datos) {
        this.dni = datos.dni();
        this.telefono = datos.telefono();
        this.gradoAcademico = datos.gradoAcademico();
        this.especialidad = datos.especialidad();
        this.institucion = datos.institucion();
        this.aniosExperiencia = datos.aniosExperiencia();
        this.enlaceProfesional = datos.enlaceProfesional();
        this.presentacion = datos.presentacion();
        this.declaracionAceptada = datos.declaracionAceptada();
        this.estado = EstadoSolicitud.PENDIENTE;
        this.comentarioRevision = null;
        this.revisadoPor = null;
        this.revisadoEn = null;
        this.enviadoEn = LocalDateTime.now();
        this.intentos++;
    }

    public void revisar(EstadoSolicitud nuevoEstado, String comentario, String revisor) {
        this.estado = nuevoEstado;
        this.comentarioRevision = comentario;
        this.revisadoPor = revisor;
        this.revisadoEn = LocalDateTime.now();
    }

    /** Datos verificables del docente, ya normalizados. */
    public record DatosDocente(String dni, String telefono, GradoAcademico gradoAcademico, String especialidad,
            String institucion, int aniosExperiencia, String enlaceProfesional, String presentacion,
            boolean declaracionAceptada) {
    }

    public Long getId() { return id; }
    public Perfil getPerfil() { return perfil; }
    public String getDni() { return dni; }
    public String getTelefono() { return telefono; }
    public GradoAcademico getGradoAcademico() { return gradoAcademico; }
    public String getEspecialidad() { return especialidad; }
    public String getInstitucion() { return institucion; }
    public int getAniosExperiencia() { return aniosExperiencia; }
    public String getEnlaceProfesional() { return enlaceProfesional; }
    public String getPresentacion() { return presentacion; }
    public boolean isDeclaracionAceptada() { return declaracionAceptada; }
    public EstadoSolicitud getEstado() { return estado; }
    public String getComentarioRevision() { return comentarioRevision; }
    public String getRevisadoPor() { return revisadoPor; }
    public LocalDateTime getRevisadoEn() { return revisadoEn; }
    public LocalDateTime getEnviadoEn() { return enviadoEn; }
    public int getIntentos() { return intentos; }
}
