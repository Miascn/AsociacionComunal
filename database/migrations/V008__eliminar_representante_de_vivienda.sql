-- =====================================================
-- MIGRACION V008: Eliminar columna id_representante de vivienda
-- La vivienda se desacopla completamente de miembros.
-- La relacion ahora es unidireccional: miembro -> vivienda (miembro.id_vivienda).
-- =====================================================

ALTER TABLE vivienda DROP FOREIGN KEY fk_vivienda_representante;
ALTER TABLE vivienda DROP COLUMN id_representante;
