package pe.edu.upeu.usuario.entity;

public enum EstadoSolicitud {
    /** Enviada, a la espera de administración. */
    PENDIENTE,
    /** Administración pidió corregir algo; la persona puede reenviarla. */
    OBSERVADA,
    /** Verificada: la cuenta recibió el rol DOCENTE en Keycloak. */
    APROBADA,
    /** Rechazada; puede postular de nuevo con otros datos. */
    RECHAZADA
}
