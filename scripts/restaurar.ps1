# Restauración de un respaldo (RNF-17) para Windows (PowerShell).
# Ejecutar:  powershell -ExecutionPolicy Bypass -File scripts\restaurar.ps1 respaldos\almibar-20261112-0200.sql
param([Parameter(Mandatory = $true)][string]$Archivo)
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Get-Content (Join-Path $raiz ".env") | Where-Object { $_ -match "^\s*[^#].*=" } | ForEach-Object {
    $partes = $_ -split "=", 2
    [Environment]::SetEnvironmentVariable($partes[0].Trim(), $partes[1].Trim())
}
$inicio = Get-Date
docker cp $Archivo almibar-mysql:/tmp/restaurar.sql
docker exec -e MYSQL_PWD=$env:DB_ROOT_PASSWORD almibar-mysql sh -c "mysql -u root $env:DB_NAME < /tmp/restaurar.sql"
$duracion = (Get-Date) - $inicio
Write-Host ("Restauración terminada en {0:N1} minutos. Ahora ejecuta scripts\verificar-integridad.sql" -f $duracion.TotalMinutes)
