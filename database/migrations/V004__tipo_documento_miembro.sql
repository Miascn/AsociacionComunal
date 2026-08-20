ALTER TABLE miembro
    MODIFY COLUMN dui VARCHAR(30) NOT NULL,
    ADD COLUMN tipo_documento VARCHAR(24) NOT NULL DEFAULT 'DUI' AFTER dui,
    ADD COLUMN pais_origen VARCHAR(80) NULL AFTER tipo_documento;

ALTER TABLE miembro DROP CHECK chk_miembro_dui;

UPDATE miembro
SET dui = REPLACE(dui, '-', ''), tipo_documento = 'DUI'
WHERE tipo_documento = 'DUI';

ALTER TABLE miembro
    ADD CONSTRAINT chk_miembro_tipo_documento
        CHECK (tipo_documento IN ('DUI', 'PASAPORTE', 'CARNET_RESIDENTE')),
    ADD CONSTRAINT chk_miembro_documento
        CHECK (
            (tipo_documento = 'DUI' AND dui REGEXP '^[0-9]{9}$')
            OR
            (tipo_documento IN ('PASAPORTE', 'CARNET_RESIDENTE') AND dui REGEXP '^[A-Za-z0-9]{5,30}$')
        ),
    ADD CONSTRAINT chk_miembro_pais_origen
        CHECK (tipo_documento = 'DUI' OR (pais_origen IS NOT NULL AND TRIM(pais_origen) <> ''));
