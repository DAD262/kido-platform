-- Migración histórica aplicada previamente a esta base de datos.
-- Reconstrucción descriptiva: SQL original no disponible.
-- La columna leccion_ids_snapshot ya existe.
ALTER TABLE ordenes_compra
ADD COLUMN IF NOT EXISTS leccion_ids_snapshot TEXT NOT NULL DEFAULT '';
