ALTER TABLE ordenes_compra ADD COLUMN pagos_aprobados_detectados INTEGER NOT NULL DEFAULT 0;
CREATE TABLE constancias_compra (
 orden_id BIGINT NOT NULL REFERENCES ordenes_compra(id),
 numero VARCHAR(90) NOT NULL UNIQUE,
 operacion VARCHAR(120) PRIMARY KEY,
 ambiente VARCHAR(20) NOT NULL,
 alumno_nombre VARCHAR(300) NOT NULL,
 alumno_email VARCHAR(200),
 alumno_documento VARCHAR(40),
 curso_titulo VARCHAR(180) NOT NULL,
 docente_id BIGINT NOT NULL,
 fecha_pago TIMESTAMP NOT NULL,
 medio_pago VARCHAR(100) NOT NULL,
 detalle_estado VARCHAR(100) NOT NULL,
 total NUMERIC(10,2) NOT NULL,
 soporte VARCHAR(160),
 generada_en TIMESTAMP NOT NULL
);
CREATE INDEX idx_constancia_orden ON constancias_compra(orden_id);
