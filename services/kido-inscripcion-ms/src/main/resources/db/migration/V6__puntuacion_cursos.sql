ALTER TABLE inscripciones ADD COLUMN puntuacion SMALLINT;
ALTER TABLE inscripciones ADD COLUMN fecha_puntuacion TIMESTAMP;
ALTER TABLE inscripciones ADD CONSTRAINT ck_inscripciones_puntuacion CHECK (puntuacion IS NULL OR puntuacion BETWEEN 1 AND 5);
