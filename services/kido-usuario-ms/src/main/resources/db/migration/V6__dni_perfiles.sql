ALTER TABLE perfiles ADD COLUMN dni VARCHAR(8);
ALTER TABLE perfiles ADD CONSTRAINT ck_perfil_dni CHECK (dni IS NULL OR dni ~ '^[0-9]{8}$');
CREATE UNIQUE INDEX uk_perfil_dni ON perfiles(dni) WHERE dni IS NOT NULL;
UPDATE perfiles p SET dni=s.dni FROM solicitudes_docente s WHERE s.perfil_id=p.id;
