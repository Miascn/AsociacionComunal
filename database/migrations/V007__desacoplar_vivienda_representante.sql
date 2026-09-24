-- =====================================================
-- MIGRACION V007: Desacoplamiento de vivienda y representante
-- Permite que las viviendas se creen y gestionen de forma independiente
-- =====================================================

ALTER TABLE vivienda DROP FOREIGN KEY fk_vivienda_representante;

ALTER TABLE vivienda ADD CONSTRAINT fk_vivienda_representante 
    FOREIGN KEY (id_representante) REFERENCES miembro(id_miembro) 
    ON DELETE SET NULL;
