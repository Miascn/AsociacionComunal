-- =====================================================
-- MIGRACIÓN V010: Clave temporal provisional y configuración comunal
-- =====================================================

-- Tabla para almacenar parámetros y configuración del sistema comunal
CREATE TABLE IF NOT EXISTS configuracion (
    clave VARCHAR(60) PRIMARY KEY,
    valor VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255) NULL,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cuota mensual de mantenimiento de la colonia por defecto ($10.00)
INSERT IGNORE INTO configuracion (clave, valor, descripcion)
VALUES ('cuota_mantenimiento_mensual', '10.00', 'Monto de la cuota mensual de mantenimiento y vigilancia de la colonia');

-- Columna para resguardar la clave provisional del miembro hasta que la cambie en el celular
ALTER TABLE usuario
    ADD COLUMN clave_temporal VARCHAR(255) NULL AFTER requiere_cambio_clave;
