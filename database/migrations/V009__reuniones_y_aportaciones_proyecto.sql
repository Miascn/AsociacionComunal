-- =====================================================
-- MIGRACIÓN V009: Reuniones vinculadas a proyectos y diferenciación de aportaciones
-- Permite vincular asambleas a proyectos comunitarios y flexibilizar aportaciones
-- =====================================================

-- 1. Añadir descripción y relación con proyecto en la tabla reunion
ALTER TABLE reunion
    ADD COLUMN id_proyecto INT NULL AFTER id_reunion,
    ADD COLUMN descripcion VARCHAR(500) NULL AFTER titulo,
    ADD CONSTRAINT fk_reunion_proyecto FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto) ON DELETE SET NULL;

-- 2. Índice para consultas rápidas de reuniones por proyecto
CREATE INDEX idx_reunion_proyecto ON reunion(id_proyecto);

-- 3. Ajustar índice único en aportaciones para que cuotas mensuales y aportes de proyectos no colisionen
ALTER TABLE aportacion DROP INDEX uk_aportacion_miembro_periodo;
ALTER TABLE aportacion ADD CONSTRAINT uk_aportacion_miembro_periodo_proyecto UNIQUE (id_miembro, periodo_mes, id_proyecto);
