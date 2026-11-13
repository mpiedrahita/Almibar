-- Almíbar · Módulo de productos
-- V1: estructura de la base de datos (sección 3 del documento, RNF-10 y RNF-24)

CREATE TABLE categoria (
    id_categoria BIGINT       NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(60)  NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE producto (
    id_producto         BIGINT        NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(100)  NOT NULL,
    descripcion         VARCHAR(255)  NULL,
    categoria_id        BIGINT        NOT NULL,
    precio              DECIMAL(10,2) NOT NULL,
    disponibilidad      BOOLEAN       NOT NULL DEFAULT TRUE,
    activo              BOOLEAN       NOT NULL DEFAULT TRUE,
    imagen_url          VARCHAR(500)  NULL,
    fecha_registro      DATETIME      NOT NULL,
    fecha_actualizacion DATETIME      NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_producto PRIMARY KEY (id_producto),
    -- La categoría debe existir y no se puede borrar si tiene productos (RNF-24)
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria (id_categoria) ON DELETE RESTRICT ON UPDATE CASCADE,
    -- RNF-10: el precio debe ser mayor que cero
    CONSTRAINT chk_producto_precio CHECK (precio > 0),
    -- Regla de negocio: un producto inactivo no puede estar disponible
    CONSTRAINT chk_producto_inactivo_no_disponible CHECK (activo = TRUE OR disponibilidad = FALSE)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Índices de los campos más usados para buscar y filtrar (RNF-01, RNF-02)
CREATE INDEX idx_producto_nombre ON producto (nombre);
CREATE INDEX idx_producto_categoria ON producto (categoria_id);
CREATE INDEX idx_producto_estado ON producto (activo, disponibilidad);

CREATE TABLE usuario (
    id_usuario      BIGINT       NOT NULL AUTO_INCREMENT,
    usuario         VARCHAR(50)  NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_usuario UNIQUE (usuario),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'CONSULTA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Registro de actividades (RNF-18). No tiene llave foránea a propósito:
-- la auditoría nunca debe impedir una operación ni perderse si algo cambia.
CREATE TABLE auditoria_producto (
    id_auditoria    BIGINT       NOT NULL AUTO_INCREMENT,
    fecha           DATETIME     NOT NULL,
    usuario         VARCHAR(50)  NOT NULL,
    operacion       VARCHAR(30)  NOT NULL,
    producto_id     BIGINT       NULL,
    nombre_producto VARCHAR(100) NULL,
    resultado       VARCHAR(15)  NOT NULL,
    detalle         VARCHAR(255) NULL,
    CONSTRAINT pk_auditoria_producto PRIMARY KEY (id_auditoria)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_auditoria_fecha ON auditoria_producto (fecha);
CREATE INDEX idx_auditoria_producto ON auditoria_producto (producto_id);
