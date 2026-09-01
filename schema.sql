-- =====================================================
-- BASE DE DATOS: asociacion_comunal
-- Sistema de Gestion Comunitaria
-- =====================================================

-- Este script es solo para instalaciones nuevas. Nunca elimina una base existente.
CREATE DATABASE IF NOT EXISTS asociacion_comunal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE asociacion_comunal;

-- =====================================================
-- TABLA: rol
-- Catalogo de perfiles de acceso
-- =====================================================
CREATE TABLE rol (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE,
    descripcion VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: miembro
-- Persona perteneciente a la asociacion
-- =====================================================
CREATE TABLE miembro (
    id_miembro INT AUTO_INCREMENT PRIMARY KEY,
    dui VARCHAR(10) NOT NULL UNIQUE,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    telefono VARCHAR(20),
    correo VARCHAR(120),
    direccion VARCHAR(250),
    fecha_ingreso DATE NOT NULL,
    estado ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: usuario
-- Credenciales y permisos de acceso
-- =====================================================
CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    id_miembro INT NULL UNIQUE,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    clave_hash VARCHAR(255) NOT NULL,
    estado ENUM('ACTIVO', 'BLOQUEADO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    ultimo_acceso DATETIME,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol),
    CONSTRAINT fk_usuario_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: cargo
-- Catalogo de cargos de la directiva
-- =====================================================
CREATE TABLE cargo (
    id_cargo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: periodo_directiva
-- Periodo de vigencia de una directiva
-- =====================================================
CREATE TABLE periodo_directiva (
    id_periodo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado ENUM('PLANIFICADO', 'ACTIVO', 'FINALIZADO') NOT NULL DEFAULT 'PLANIFICADO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: miembro_cargo
-- Asignacion historica de un cargo a un miembro
-- =====================================================
CREATE TABLE miembro_cargo (
    id_miembro_cargo INT AUTO_INCREMENT PRIMARY KEY,
    id_miembro INT NOT NULL,
    id_cargo INT NOT NULL,
    id_periodo INT NOT NULL,
    fecha_asignacion DATE NOT NULL,
    CONSTRAINT fk_mc_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
    CONSTRAINT fk_mc_cargo FOREIGN KEY (id_cargo) REFERENCES cargo(id_cargo),
    CONSTRAINT fk_mc_periodo FOREIGN KEY (id_periodo) REFERENCES periodo_directiva(id_periodo),
    CONSTRAINT uk_mc_cargo_periodo UNIQUE (id_cargo, id_periodo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: aportacion
-- Pago mensual realizado por un miembro
-- =====================================================
CREATE TABLE aportacion (
    id_aportacion BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_miembro INT NOT NULL,
    id_proyecto INT NULL,
    periodo_mes CHAR(7) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_pago DATE NOT NULL,
    metodo_pago ENUM('EFECTIVO', 'TRANSFERENCIA', 'OTRO') NOT NULL,
    referencia VARCHAR(80),
    estado ENUM('REGISTRADA', 'ANULADA') NOT NULL DEFAULT 'REGISTRADA',
    CONSTRAINT fk_aportacion_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
    CONSTRAINT uk_aportacion_miembro_periodo UNIQUE (id_miembro, periodo_mes)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: proyecto
-- Propuesta de mejora comunitaria
-- =====================================================
CREATE TABLE proyecto (
    id_proyecto INT AUTO_INCREMENT PRIMARY KEY,
    creado_por INT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT NOT NULL,
    presupuesto DECIMAL(12,2),
    fecha_creacion DATE NOT NULL,
    estado ENUM('BORRADOR', 'PROPUESTO', 'APROBADO', 'RECHAZADO', 'EN_EJECUCION', 'FINALIZADO') NOT NULL DEFAULT 'BORRADOR',
    CONSTRAINT fk_proyecto_usuario FOREIGN KEY (creado_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE aportacion ADD CONSTRAINT fk_aportacion_proyecto FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto) ON DELETE SET NULL;

-- =====================================================
-- TABLA: votacion
-- Proceso de decision relacionado con un proyecto
-- =====================================================
CREATE TABLE votacion (
    id_votacion INT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    estado ENUM('PROGRAMADA', 'ABIERTA', 'CERRADA', 'CANCELADA') NOT NULL DEFAULT 'PROGRAMADA',
    CONSTRAINT fk_votacion_proyecto FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: opcion_votacion
-- Opciones disponibles dentro de una votacion
-- =====================================================
CREATE TABLE opcion_votacion (
    id_opcion INT AUTO_INCREMENT PRIMARY KEY,
    id_votacion INT NOT NULL,
    descripcion VARCHAR(120) NOT NULL,
    orden SMALLINT NOT NULL,
    CONSTRAINT fk_opcion_votacion FOREIGN KEY (id_votacion) REFERENCES votacion(id_votacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: voto
-- Seleccion emitida por un miembro
-- =====================================================
CREATE TABLE voto (
    id_voto BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_votacion INT NOT NULL,
    id_opcion INT NOT NULL,
    id_miembro INT NOT NULL,
    fecha_hora DATETIME NOT NULL,
    CONSTRAINT fk_voto_votacion FOREIGN KEY (id_votacion) REFERENCES votacion(id_votacion),
    CONSTRAINT fk_voto_opcion FOREIGN KEY (id_opcion) REFERENCES opcion_votacion(id_opcion),
    CONSTRAINT fk_voto_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
    CONSTRAINT uk_voto_votacion_miembro UNIQUE (id_votacion, id_miembro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: reunion
-- Convocatoria o sesion de la asociacion
-- =====================================================
CREATE TABLE reunion (
    id_reunion INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    fecha_hora DATETIME NOT NULL,
    lugar VARCHAR(150),
    tipo ENUM('ORDINARIA', 'EXTRAORDINARIA') NOT NULL DEFAULT 'ORDINARIA',
    estado ENUM('PROGRAMADA', 'REALIZADA', 'CANCELADA') NOT NULL DEFAULT 'PROGRAMADA'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: asistencia
-- Participacion de miembros en reuniones
-- =====================================================
CREATE TABLE asistencia (
    id_asistencia BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_reunion INT NOT NULL,
    id_miembro INT NOT NULL,
    asistio BOOLEAN NOT NULL DEFAULT FALSE,
    observacion VARCHAR(200),
    CONSTRAINT fk_asistencia_reunion FOREIGN KEY (id_reunion) REFERENCES reunion(id_reunion),
    CONSTRAINT fk_asistencia_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
    CONSTRAINT uk_asistencia_reunion_miembro UNIQUE (id_reunion, id_miembro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLA: bitacora
-- Registro de acciones relevantes
-- =====================================================
CREATE TABLE bitacora (
    id_bitacora BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    accion VARCHAR(80) NOT NULL,
    entidad VARCHAR(60),
    id_registro VARCHAR(40),
    fecha_hora DATETIME NOT NULL,
    detalle TEXT,
    CONSTRAINT fk_bitacora_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- INDICES ADICIONALES
-- =====================================================
CREATE INDEX idx_miembro_estado ON miembro(estado);
CREATE INDEX idx_usuario_estado ON usuario(estado);
CREATE INDEX idx_proyecto_estado ON proyecto(estado);
CREATE INDEX idx_votacion_estado ON votacion(estado);
CREATE INDEX idx_reunion_fecha ON reunion(fecha_hora);
CREATE INDEX idx_bitacora_fecha ON bitacora(fecha_hora);

-- =====================================================
-- DATOS INICIALES
-- =====================================================
INSERT INTO rol (nombre, descripcion) VALUES
('ADMIN', 'Administrador del sistema'),
('DIRECTIVO', 'Miembro de la junta directiva'),
('MIEMBRO', 'Miembro regular de la asociacion');

INSERT INTO cargo (nombre, descripcion) VALUES
('Presidente', 'Representante legal de la asociacion'),
('Vicepresidente', 'Sustituto del presidente'),
('Secretario', 'Responsable de actas y correspondencia'),
('Tesorero', 'Responsable de finanzas'),
('Vocal', 'Miembro sin cargo especifico');

INSERT INTO periodo_directiva (nombre, fecha_inicio, fecha_fin, estado) VALUES
('Directiva 2024-2026', '2024-01-01', '2026-12-31', 'ACTIVO');

-- Cree la primera cuenta administrativa con una contraseña aleatoria mediante
-- una herramienta operativa segura. El repositorio no contiene credenciales predeterminadas.
