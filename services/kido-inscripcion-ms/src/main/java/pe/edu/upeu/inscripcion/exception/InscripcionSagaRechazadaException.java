package pe.edu.upeu.inscripcion.exception;

/** A permanent domain rejection. Temporary database/Kafka failures must NOT be compensated. */
public class InscripcionSagaRechazadaException extends RuntimeException {
    public InscripcionSagaRechazadaException(String mensaje) { super(mensaje); }
}
