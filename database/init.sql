-- ===========================================================================
-- Refugio de mascotas "Patitas al Rescate"
-- Script de creación de la base de datos y de la tabla de credenciales.
-- ===========================================================================
-- Ejecutar la primera sentencia conectado a la base "postgres" y luego
-- conectarse a "refugio_patitas" antes de crear la tabla.
-- ===========================================================================

CREATE DATABASE refugio_patitas;

-- \c refugio_patitas   (en psql; en pgAdmin seleccione la base)

CREATE TABLE usuarios (
    id       SERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    rol      VARCHAR(20)  NOT NULL,
    CONSTRAINT chk_usuarios_rol CHECK (rol IN ('ADMIN', 'USER'))
);

-- La columna password almacena hashes BCrypt de 60 caracteres (Hito 2).
-- El CHECK del rol protege la integridad de la arquitectura de identidad.

CREATE INDEX idx_usuarios_username ON usuarios (username);

-- Evidencia de los hitos:
--   Hito 1: SELECT * FROM usuarios;  -> contraseña en texto plano (vulnerable)
--   Hito 2: SELECT * FROM usuarios;  -> hash que empieza con $2a$ (BCrypt)
