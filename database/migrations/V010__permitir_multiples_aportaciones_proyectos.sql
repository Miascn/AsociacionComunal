-- =====================================================
-- MIGRACIÓN V010: Permitir múltiples aportaciones a proyectos comunitarios
-- 1. Crea índice regular (no único) que cubre la foreign key fk_aportacion_miembro y optimiza búsquedas.
-- 2. Elimina constraints únicos históricos (uq_aportacion_miembro_periodo o uk_aportacion_miembro_periodo_proyecto)
--    que impedían múltiples aportaciones al mismo proyecto o a diferentes proyectos en el mismo período.
-- 3. La unicidad de la cuota mensual ordinaria (id_proyecto IS NULL) se garantiza en la capa de negocio (AportacionService).
-- =====================================================

-- 1. Crear índice regular primero para que InnoDB no rechace la eliminación por dependencia de foreign key
CREATE INDEX idx_aportacion_miembro_periodo_proyecto ON aportacion(id_miembro, periodo_mes, id_proyecto);

-- 2. Eliminar índices únicos restrictivos
DROP INDEX IF EXISTS uq_aportacion_miembro_periodo ON aportacion;
DROP INDEX IF EXISTS uk_aportacion_miembro_periodo ON aportacion;
DROP INDEX IF EXISTS uk_aportacion_miembro_periodo_proyecto ON aportacion;
DROP INDEX IF EXISTS uk_aportacion_miembro_periodo_proy ON aportacion;
