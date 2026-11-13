# Respaldo de la base de datos (RNF-17) para Windows (PowerShell).
# Ejecutar desde la carpeta del proyecto:  powershell -ExecutionPolicy Bypass -File scripts\respaldo.ps1
# Respaldo diario: Programador de tareas > Crear tarea básica > Diariamente > ejecutar este mismo comando.
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Get-Content (Join-Path $raiz ".env") | Where-Object { $_ -match "^\s*[^#].*=" } | ForEach-Object {
    $partes = $_ -split "=", 2
    [Environment]::SetEnvironmentVariable($partes[0].Trim(), $partes[1].Trim())
}
$carpeta = Join-Path $raiz "respaldos"
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
$archivo = Join-Path $carpeta ("almibar-" + (Get-Date -Format "yyyyMMdd-HHmm") + ".sql")
# El respaldo se genera dentro del contenedor y luego se copia, así se conservan las tildes.
docker exec -e MYSQL_PWD=$env:DB_ROOT_PASSWORD almibar-mysql sh -c "mysqldump -u root --single-transaction --routines --triggers $env:DB_NAME > /tmp/respaldo.sql"
docker cp almibar-mysql:/tmp/respaldo.sql $archivo
# Conserva los últimos 14 respaldos
Get-ChildItem $carpeta -Filter "almibar-*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -Skip 14 | Remove-Item
Write-Host "Respaldo guardado en $archivo"
