-- =====================================================
-- SCRUM-253 - Proteger el historial de decisiones comunitarias
--
-- La restriccion original de schema.sql declaraba:
--     CONSTRAINT fk_votacion_proyecto FOREIGN KEY (id_proyecto)
--         REFERENCES proyecto(id_proyecto) ON DELETE SET NULL
--
-- Con ON DELETE SET NULL la base de datos no rechazaba el borrado de un
-- proyecto con votaciones: ponia en nulo la referencia y las votaciones
-- sobrevivian huerfanas, sin el proyecto sobre el que la comunidad decidio.
--
-- ON DELETE RESTRICT hace que el motor rechace la operacion. Es la segunda
-- capa de defensa: la primera es la comprobacion de ProyectoService.delete().
-- =====================================================

ALTER TABLE votacion
    DROP FOREIGN KEY fk_votacion_proyecto;

ALTER TABLE votacion
    ADD CONSTRAINT fk_votacion_proyecto
        FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto)
        ON DELETE RESTRICT;
