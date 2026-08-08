ALTER TABLE usuario
    ADD COLUMN requiere_cambio_clave BOOLEAN NOT NULL DEFAULT FALSE
    AFTER estado;
