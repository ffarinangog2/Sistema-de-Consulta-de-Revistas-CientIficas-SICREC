#!/bin/sh
set -eu

BACKUP_FILE="/backups/sicrec_db_dockerizacion_20260811.dump"
MARKER_FILE="/var/lib/postgresql/data/.sicrec-init-complete"

if [ -s "$BACKUP_FILE" ]; then
  echo "Restaurando respaldo inicial de SICREC..."
  pg_restore \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --no-owner \
    --no-privileges \
    --exit-on-error \
    "$BACKUP_FILE"
  echo "Respaldo inicial de SICREC restaurado."
else
  echo "No se encontró respaldo inicial; PostgreSQL conservará la base nueva."
fi

touch "$MARKER_FILE"
