-- Persist the course terms at checkout. Never call Course synchronously when confirming a paid order.
-- Historical S08 orders receive safe defaults; previously synchronized enrollments are preserved.
ALTER TABLE ordenes_compra ADD COLUMN lecciones_snapshot VARCHAR(8000) NOT NULL DEFAULT '';
ALTER TABLE ordenes_compra ADD COLUMN progreso_minimo_snapshot INTEGER NOT NULL DEFAULT 100;
ALTER TABLE ordenes_compra ADD COLUMN asistencia_minima_snapshot INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ordenes_compra ADD COLUMN certificado_habilitado_snapshot BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ordenes_compra ADD COLUMN certificado_costo_snapshot NUMERIC(10,2) NOT NULL DEFAULT 0;
