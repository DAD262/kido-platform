ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS orden_compra_id BIGINT;
CREATE UNIQUE INDEX IF NOT EXISTS uk_inscripciones_orden_compra
    ON inscripciones(orden_compra_id)
    WHERE orden_compra_id IS NOT NULL;
