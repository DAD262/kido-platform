-- NULL preserves older manually-created notifications and legacy rows.
ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS evento_clave VARCHAR(120);
CREATE UNIQUE INDEX IF NOT EXISTS uk_notificacion_evento_destinatario_tipo
 ON notificaciones(evento_clave, usuario_id, tipo)
 WHERE evento_clave IS NOT NULL;
