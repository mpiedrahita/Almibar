#!/usr/bin/env bash
# Respaldo de la base de datos (RNF-17) para Linux o macOS.
# Respaldo diario con cron (2:00 a. m.):  0 2 * * * cd /ruta/al/proyecto && ./scripts/respaldo.sh
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
mkdir -p respaldos
archivo="respaldos/almibar-$(date +%Y%m%d-%H%M).sql"
docker exec almibar-mysql mysqldump -u root -p"$DB_ROOT_PASSWORD" --single-transaction --routines --triggers "$DB_NAME" > "$archivo"
ls -1t respaldos/almibar-*.sql | tail -n +15 | xargs -r rm --
echo "Respaldo guardado en $archivo"
