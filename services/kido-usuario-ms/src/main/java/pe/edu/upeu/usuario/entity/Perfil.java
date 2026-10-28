package pe.edu.upeu.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "perfiles")
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", unique = true, length = 64)
    private String keycloakId;

    @Column(nullable = false, unique = true, length = 160)
    private String username;

    @Column(unique = true, length = 160)
    private String email;

    @Column(length = 8, unique = true)
    private String dni;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_registro", nullable = false, length = 20)
    private TipoCuenta tipoRegistro;

    @Column(name = "docente_verificado", nullable = false)
    private boolean docenteVerificado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    protected Perfil() {
    }

    public Perfil(String keycloakId, String username, String email, String nombres, String apellidos,
            TipoCuenta tipoRegistro) {
        this.keycloakId = keycloakId;
        this.username = username;
        this.email = email;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.tipoRegistro = tipoRegistro;
    }

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now();
    }

    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }

    public Long getId() { return id; }
    public String getKeycloakId() { return keycloakId; }
    public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username=username; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni=dni; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public TipoCuenta getTipoRegistro() { return tipoRegistro; }
    public void setTipoRegistro(TipoCuenta tipoRegistro) { this.tipoRegistro = tipoRegistro; }
    public boolean isDocenteVerificado() { return docenteVerificado; }
    public void setDocenteVerificado(boolean docenteVerificado) { this.docenteVerificado = docenteVerificado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
