CREATE TABLE vivienda (
    id_vivienda INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    sector VARCHAR(80) NOT NULL,
    direccion VARCHAR(250) NOT NULL,
    referencia VARCHAR(250) NULL,
    id_representante INT UNSIGNED NULL,
    fecha_registro DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT fk_vivienda_representante FOREIGN KEY (id_representante) REFERENCES miembro(id_miembro),
    CONSTRAINT chk_vivienda_estado CHECK (estado IN ('ACTIVA', 'DESHABITADA', 'INACTIVA'))
);

ALTER TABLE miembro
    MODIFY COLUMN direccion VARCHAR(250) NULL,
    ADD COLUMN id_vivienda INT NULL AFTER pais_origen,
    ADD CONSTRAINT fk_miembro_vivienda FOREIGN KEY (id_vivienda) REFERENCES vivienda(id_vivienda);

CREATE TABLE residente_vivienda (
    id_residente INT AUTO_INCREMENT PRIMARY KEY,
    id_vivienda INT NOT NULL,
    id_miembro INT UNSIGNED NULL,
    nombre_completo VARCHAR(180) NOT NULL,
    tipo_persona VARCHAR(10) NOT NULL,
    es_representante BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_residente_vivienda FOREIGN KEY (id_vivienda) REFERENCES vivienda(id_vivienda),
    CONSTRAINT fk_residente_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
    CONSTRAINT chk_residente_tipo CHECK (tipo_persona IN ('ADULTO', 'MENOR')),
    UNIQUE (id_vivienda, id_miembro)
);

CREATE INDEX idx_vivienda_representante ON vivienda(id_representante);
CREATE INDEX idx_miembro_vivienda ON miembro(id_vivienda);
CREATE INDEX idx_residente_vivienda ON residente_vivienda(id_vivienda);
