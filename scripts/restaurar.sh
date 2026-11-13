#!/usr/bin/env bash
# Restauración de un respaldo (RNF-17) para Linux o macOS.
# Uso: ./scripts/restaurar.sh respaldos/almibar-20261112-0200.sql
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
inicio=$(date +%s)
docker exec -i almibar-mysql mysql -u root -p"$DB_ROOT_PASSWORD" "$DB_NAME" < "$1"
echo "Restauración terminada en $(( ($(date +%s) - inicio) / 60 )) minutos. Ahora ejecuta scripts/verificar-integridad.sql"
