-- INICIO - Auditoría general
-- El usuario es opcional para eventos donde no existe una autenticación
-- disponible, como intentos con usuarios inexistentes y exportaciones.
ALTER TABLE IF EXISTS auditoria
    ALTER COLUMN usuario_id DROP NOT NULL;
-- FIN - Auditoría general
