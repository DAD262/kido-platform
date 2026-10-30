CREATE TABLE carritos_estudiante (
 estudiante_id BIGINT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE carrito_cursos (
 estudiante_id BIGINT NOT NULL REFERENCES carritos_estudiante(estudiante_id),
 curso_id BIGINT NOT NULL,
 PRIMARY KEY (estudiante_id, curso_id)
);
