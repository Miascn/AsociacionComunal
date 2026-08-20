-- Sesiones reutilizables para clientes Android, JavaFX y futuros clientes.
-- Aplicar manualmente sobre asociacion_comunal después de generar un respaldo.
CREATE TABLE sesion_usuario (
    id_sesion BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNSIGNED NOT NULL,
    access_token_hash CHAR(64) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    creada_en DATETIME NOT NULL,
    access_expira_en DATETIME NOT NULL,
    refresh_expira_en DATETIME NOT NULL,
    revocada_en DATETIME NULL,
    CONSTRAINT uq_sesion_access_token UNIQUE (access_token_hash),
    CONSTRAINT uq_sesion_refresh_token UNIQUE (refresh_token_hash),
    CONSTRAINT fk_sesion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    INDEX idx_sesion_usuario_activa (id_usuario, revocada_en),
    INDEX idx_sesion_refresh_expira (refresh_expira_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
