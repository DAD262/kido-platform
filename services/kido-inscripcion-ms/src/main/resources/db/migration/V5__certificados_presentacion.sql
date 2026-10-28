ALTER TABLE certificados ADD COLUMN alumno_nombre VARCHAR(220),ADD COLUMN curso_titulo VARCHAR(300),ADD COLUMN docente_nombre VARCHAR(220);
UPDATE inscripciones SET certificado_habilitado=TRUE WHERE curso_id IN(1,2,3) AND certificado_habilitado=FALSE AND certificado_costo=0 AND estado IN('ACTIVA','COMPLETADA');
