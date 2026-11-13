-- Carga 5.000 productos de prueba para las pruebas de rendimiento (RNF-01).
-- Úsalo solo en una base de datos de pruebas, nunca en producción.
-- Ejecutar: docker exec -i almibar-mysql mysql -u root -p almibar < scripts/semilla-5000-productos.sql

SET SESSION cte_max_recursion_depth = 6000;

INSERT INTO producto (nombre, descripcion, categoria_id, precio, disponibilidad, activo, imagen_url, fecha_registro, fecha_actualizacion)
SELECT
    CONCAT(ELT(1 + (n MOD 8), 'Torta', 'Tarta', 'Galleta', 'Alfajor', 'Cheesecake', 'Rollo', 'Postre de leche', 'Brownie'), ' de prueba ', n),
    'Producto generado para pruebas de carga.',
    (SELECT MIN(id_categoria) FROM categoria) + (n MOD 5),
    ROUND(3000 + (n MOD 200) * 450, 2),
    (n MOD 4) <> 0,
    TRUE,
    NULL,
    NOW(),
    NOW()
FROM (
    WITH RECURSIVE numeros (n) AS (
        SELECT 1
        UNION ALL
        SELECT n + 1 FROM numeros WHERE n < 5000
    )
    SELECT n FROM numeros
) AS serie;
