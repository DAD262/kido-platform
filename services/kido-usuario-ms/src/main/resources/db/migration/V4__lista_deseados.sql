-- S7b / lista de deseados: la identidad es el sub inmutable del JWT de Keycloak.
-- No modifica perfiles.id, estudiante_id ni órdenes históricas de Pago.
CREATE TABLE lista_deseados (
    keycloak_sub VARCHAR(64) NOT NULL,
    curso_id BIGINT NOT NULL CHECK (curso_id > 0),
    agregado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_lista_deseados PRIMARY KEY (keycloak_sub, curso_id),
    CONSTRAINT ck_deseados_sub CHECK (length(trim(keycloak_sub)) BETWEEN 1 AND 64)
);
CREATE INDEX idx_lista_deseados_usuario_fecha ON lista_deseados (keycloak_sub, agregado_en DESC);
