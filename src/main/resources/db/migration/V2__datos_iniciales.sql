-- Almíbar · Módulo de productos
-- V2: datos iniciales (RF-P01 y RNF-P03)

-- Categorías precargadas
INSERT INTO categoria (nombre) VALUES
    ('Tortas'),
    ('Tartas'),
    ('Postres fríos'),
    ('Galletas y alfajores'),
    ('Panadería dulce');

-- Usuarios de demostración. Las contraseñas están guardadas con BCrypt (RNF-09);
-- en texto plano solo aparecen en el README, para poder probar el sistema.
INSERT INTO usuario (usuario, nombre, contrasena_hash, rol, activo) VALUES
    ('admin',    'Ana',    '$2a$10$S2uxHfh3svnpMXnV2wbCNeeWWs/DLshkvaHxrHlL.g2kJzmPcnLHC', 'ADMIN',    TRUE),
    ('consulta', 'Camilo', '$2a$10$IlS898YgtocaV/nuBN1gcOSX2ouM4Uq6wREprzHr2/78ui5sc2APm', 'CONSULTA', TRUE);

-- Productos de ejemplo
INSERT INTO producto (nombre, descripcion, categoria_id, precio, disponibilidad, activo, imagen_url, fecha_registro, fecha_actualizacion)
SELECT p.nombre, p.descripcion, c.id_categoria, p.precio, p.disponibilidad, p.activo, p.imagen, NOW(), NOW()
FROM (
    SELECT 'Alfajores de pistacho' AS nombre, 'Galleta de mantequilla, relleno de pistacho y baño de chocolate blanco.' AS descripcion,
           'Galletas y alfajores' AS categoria, 6500.00 AS precio, TRUE AS disponibilidad, TRUE AS activo,
           '/img/productos/alfajores-pistacho.webp' AS imagen
    UNION ALL SELECT 'Torta de chocolate y frambuesa', 'Bizcocho de cacao, ganache y mermelada de frambuesa. Para 12 porciones.', 'Tortas', 78000.00, TRUE, TRUE, NULL
    UNION ALL SELECT 'Cheesecake de maracuyá', 'Base de galleta, crema de queso y salsa de maracuyá.', 'Postres fríos', 12000.00, FALSE, TRUE, NULL
    UNION ALL SELECT 'Tarta de manzana y canela', 'Masa quebrada, manzana caramelizada y un toque de canela.', 'Tartas', 58000.00, TRUE, TRUE, NULL
    UNION ALL SELECT 'Rollos de canela', 'Masa suave con canela y glaseado de queso crema.', 'Panadería dulce', 7500.00, TRUE, TRUE, NULL
    UNION ALL SELECT 'Tres leches', 'Bizcocho humedecido en tres leches con merengue.', 'Postres fríos', 9500.00, FALSE, TRUE, NULL
    UNION ALL SELECT 'Galletas de avena y arándanos', 'Paquete de seis galletas horneadas el mismo día.', 'Galletas y alfajores', 14000.00, TRUE, TRUE, NULL
    UNION ALL SELECT 'Torta negra', 'Receta de temporada con frutas maceradas en vino.', 'Tortas', 65000.00, FALSE, FALSE, NULL
) AS p
JOIN categoria c ON c.nombre = p.categoria;
