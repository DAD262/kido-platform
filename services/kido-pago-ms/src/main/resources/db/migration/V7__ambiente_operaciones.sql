-- Los datos anteriores no contienen evidencia suficiente para clasificarlos.
ALTER TABLE ordenes_compra ADD COLUMN ambiente VARCHAR(20) NOT NULL DEFAULT 'DESCONOCIDO';
ALTER TABLE ordenes_certificado ADD COLUMN ambiente VARCHAR(20) NOT NULL DEFAULT 'DESCONOCIDO';
ALTER TABLE ordenes_compra ADD COLUMN receptor_mp VARCHAR(40);
ALTER TABLE ordenes_certificado ADD COLUMN receptor_mp VARCHAR(40);
ALTER TABLE movimientos_saldo ADD COLUMN ambiente VARCHAR(20) NOT NULL DEFAULT 'DESCONOCIDO';
ALTER TABLE ordenes_compra ADD CONSTRAINT ck_compra_ambiente CHECK (ambiente IN ('DESCONOCIDO','PRUEBAS','PRODUCCION'));
ALTER TABLE ordenes_certificado ADD CONSTRAINT ck_certificado_ambiente CHECK (ambiente IN ('DESCONOCIDO','PRUEBAS','PRODUCCION'));
ALTER TABLE movimientos_saldo ADD CONSTRAINT ck_movimiento_ambiente CHECK (ambiente IN ('DESCONOCIDO','PRUEBAS','PRODUCCION'));
