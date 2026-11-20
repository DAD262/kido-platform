ALTER TABLE ordenes_compra DROP CONSTRAINT ck_compra_ambiente;
ALTER TABLE ordenes_compra ADD CONSTRAINT ck_compra_ambiente
    CHECK (ambiente IN ('DESCONOCIDO','PRUEBAS','PRODUCCION','YAPE_MANUAL'));
ALTER TABLE ordenes_compra ADD COLUMN yape_operacion VARCHAR(48) UNIQUE;
ALTER TABLE ordenes_compra ADD COLUMN yape_confirmado_por VARCHAR(200);
CREATE UNIQUE INDEX uq_yape_manual_orden_activa
    ON ordenes_compra (estudiante_id, curso_id)
    WHERE ambiente = 'YAPE_MANUAL' AND estado IN ('PENDIENTE', 'APROBADA');
