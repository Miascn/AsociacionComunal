-- =====================================================
-- DATOS DE DEMOSTRACION - asociacion_comunal (Avance 3)
-- =====================================================
--
-- ADVERTENCIA: solo para una base LOCAL recien creada con database/schema.sql
-- (evaluacion, defensa o pruebas manuales). NO ejecutar en DEV/QA compartidos:
-- crea cuentas con contrasenas publicadas en el README.
--
--   mysql -u root -p asociacion_comunal < database/data.sql
--
-- Usuarios de prueba (las contrasenas se guardan solo como hash PBKDF2-SHA256):
--   demo.admin      / Demo.Admin.2026!      rol ADMIN      escritorio JavaFX
--   demo.directiva  / Demo.Directiva.2026!  rol DIRECTIVO  escritorio JavaFX
--   demo.miembro    / Demo.Miembro.2026!    rol MIEMBRO    app movil (Flutter)
--
-- Los identificadores son explicitos porque el script supone una base vacia.
-- =====================================================

USE asociacion_comunal;

-- Viviendas
INSERT INTO vivienda (id_vivienda, codigo, sector, direccion, referencia, fecha_registro, estado) VALUES
(1, 'A-01', 'Sector A', 'Pasaje 1, casa 1', 'Frente a la cancha', '2024-01-15', 'ACTIVA'),
(2, 'A-02', 'Sector A', 'Pasaje 1, casa 2', NULL, '2024-01-15', 'ACTIVA'),
(3, 'B-01', 'Sector B', 'Pasaje 4, casa 1', 'Esquina', '2024-02-01', 'ACTIVA'),
(4, 'B-02', 'Sector B', 'Pasaje 4, casa 2', NULL, '2024-02-01', 'ACTIVA');

-- Miembros: 7 activos y 1 inactivo (el inactivo no debe ser convocado)
INSERT INTO miembro (id_miembro, dui, tipo_documento, pais_origen, id_vivienda, nombres, apellidos, telefono, correo, direccion, fecha_ingreso, estado) VALUES
(1, '012345678', 'DUI', NULL, 1, 'Maria Elena', 'Lopez Rivas',     '7000-0001', 'maria.lopez@example.com',  NULL, '2024-01-20', 'ACTIVO'),
(2, '023456789', 'DUI', NULL, 1, 'Jose Antonio', 'Lopez Rivas',    '7000-0002', NULL,                       NULL, '2024-01-20', 'ACTIVO'),
(3, '034567890', 'DUI', NULL, 2, 'Ana Gabriela', 'Martinez Cruz',  '7000-0003', 'ana.martinez@example.com', NULL, '2024-02-05', 'ACTIVO'),
(4, '045678901', 'DUI', NULL, 2, 'Carlos Ernesto', 'Martinez Cruz','7000-0004', NULL,                       NULL, '2024-02-05', 'ACTIVO'),
(5, '056789012', 'DUI', NULL, 3, 'Rosa Isabel', 'Hernandez Diaz',  '7000-0005', NULL,                       NULL, '2024-03-10', 'ACTIVO'),
(6, '067890123', 'DUI', NULL, 3, 'Luis Fernando', 'Hernandez Diaz','7000-0006', NULL,                       NULL, '2024-03-10', 'ACTIVO'),
(7, 'AB123456', 'PASAPORTE', 'Guatemala', 4, 'Sofia', 'Ramirez Paz','7000-0007', NULL,                     NULL, '2024-04-01', 'ACTIVO'),
(8, '078901234', 'DUI', NULL, 4, 'Pedro Pablo', 'Gomez Vides',     '7000-0008', NULL,                       NULL, '2024-04-01', 'INACTIVO');

-- Usuarios de demostracion
INSERT INTO usuario (id_usuario, id_rol, id_miembro, nombre_usuario, clave_hash, estado, requiere_cambio_clave, clave_temporal) VALUES
(1, (SELECT id_rol FROM rol WHERE nombre = 'ADMIN'),     NULL, 'demo.admin',
    'pbkdf2_sha256$210000$U9kQ+1JPDrosoN4QH3eeCQ==$sPbUR2HUBmZZnFpZVGc6KaqyX9nFijsF06XAb1NaS2U=', 'ACTIVO', FALSE, NULL),
(2, (SELECT id_rol FROM rol WHERE nombre = 'DIRECTIVO'), 1,    'demo.directiva',
    'pbkdf2_sha256$210000$T1lWvu6Ih9VgQ8IVbGNRIQ==$ST7ZfAUVE1XKcnvs1paHIOd/7wHObrSNQRbW2aZYxyc=', 'ACTIVO', FALSE, NULL),
(3, (SELECT id_rol FROM rol WHERE nombre = 'MIEMBRO'),   3,    'demo.miembro',
    'pbkdf2_sha256$210000$6qeOF9lB3YK0qNZxW53aHg==$E0gIt/VjkEdn3rcfiIDqkFK9nAHrviem+VZ+wGbPAMA=', 'ACTIVO', FALSE, NULL);

-- Directiva actual
INSERT INTO miembro_cargo (id_miembro, id_cargo, id_periodo, fecha_asignacion, estado) VALUES
(1, (SELECT id_cargo FROM cargo WHERE nombre = 'Presidente'), 1, '2024-01-20', 'ACTIVO'),
(3, (SELECT id_cargo FROM cargo WHERE nombre = 'Tesorero'),   1, '2024-02-05', 'ACTIVO');

-- Proyecto comunitario
INSERT INTO proyecto (id_proyecto, creado_por, nombre, descripcion, presupuesto, fecha_creacion, estado) VALUES
(1, 1, 'Luminarias del pasaje 4', 'Instalacion de cuatro luminarias LED en el pasaje 4.', 480.00, '2026-09-01', 'PROPUESTO');

-- Cuotas de mantenimiento y un aporte voluntario al proyecto
INSERT INTO aportacion (id_miembro, id_proyecto, periodo_mes, monto, fecha_pago, metodo_pago, referencia, estado) VALUES
(1, NULL, '2026-09', 10.00, '2026-09-05', 'EFECTIVO',      NULL,        'REGISTRADA'),
(3, NULL, '2026-09', 10.00, '2026-09-07', 'TRANSFERENCIA', 'TRX-00921', 'REGISTRADA'),
(5, NULL, '2026-09', 10.00, '2026-09-10', 'EFECTIVO',      NULL,        'REGISTRADA'),
(1, 1,    '2026-09', 25.00, '2026-09-12', 'EFECTIVO',      NULL,        'REGISTRADA');

-- Reuniones: una programada (para demostrar la convocatoria concurrente) y una realizada
INSERT INTO reunion (id_reunion, id_proyecto, titulo, descripcion, fecha_hora, lugar, tipo, estado) VALUES
(1, 1,    'Asamblea general - luminarias', 'Presentacion y votacion del proyecto de luminarias.', '2026-12-05 18:00:00', 'Casa comunal', 'ORDINARIA', 'PROGRAMADA'),
(2, NULL, 'Asamblea de septiembre',        'Informe de tesoreria.',                               '2026-09-26 18:00:00', 'Casa comunal', 'ORDINARIA', 'REALIZADA');

-- Asistencia de la reunion realizada
INSERT INTO asistencia (id_reunion, id_miembro, asistio, observacion) VALUES
(2, 1, TRUE,  NULL),
(2, 3, TRUE,  NULL),
(2, 5, FALSE, 'Aviso por enfermedad');
