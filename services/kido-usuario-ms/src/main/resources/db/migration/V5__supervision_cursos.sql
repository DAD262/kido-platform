CREATE TABLE supervision_cursos (
    supervisor_sub VARCHAR(64) NOT NULL,
    curso_id BIGINT NOT NULL CHECK (curso_id > 0),
    asignado_por VARCHAR(64) NOT NULL,
    asignado_en TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (supervisor_sub, curso_id)
);
