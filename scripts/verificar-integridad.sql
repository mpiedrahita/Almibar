-- Verificación de integridad después de una prueba de estrés (RNF-04) o de una restauración (RNF-17).
-- Cada consulta debe devolver 0.
-- Ejecutar: docker exec -i almibar-mysql mysql -u root -p almibar < scripts/verificar-integridad.sql

SELECT 'Productos inactivos marcados como disponibles' AS revision, COUNT(*) AS problemas
FROM producto WHERE activo = FALSE AND disponibilidad = TRUE
UNION ALL
SELECT 'Productos con precio menor o igual a cero', COUNT(*) FROM producto WHERE precio <= 0
UNION ALL
SELECT 'Productos con una categoría inexistente', COUNT(*)
FROM producto p LEFT JOIN categoria c ON c.id_categoria = p.categoria_id WHERE c.id_categoria IS NULL
UNION ALL
SELECT 'Productos sin nombre', COUNT(*) FROM producto WHERE nombre IS NULL OR TRIM(nombre) = ''
UNION ALL
SELECT 'Productos con fecha de actualización anterior al registro', COUNT(*) FROM producto WHERE fecha_actualizacion < fecha_registro;
