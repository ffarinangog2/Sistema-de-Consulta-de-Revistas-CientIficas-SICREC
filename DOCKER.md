# Despliegue completo de SICREC con Docker

## Requisitos

- Docker Desktop en Windows 10/11, o Docker Engine con el complemento Compose en Linux.
- Al menos 4 GB de RAM disponibles y conexión a internet durante la primera construcción.
- Puerto 80 libre. Para usar ngrok, SICREC debe estar publicado localmente en ese puerto.

## Instalación en otra computadora

1. Copie la carpeta completa del proyecto, incluyendo `.env`, `crecuteq-backend` y `docker`.
2. Instale Docker y confirme que el motor esté iniciado.
3. Revise `.env` y cambie las contraseñas/API keys cuando corresponda. No comparta ni publique este archivo.
4. Abra PowerShell o una terminal en la raíz del proyecto.
5. Ejecute:

   ```bash
   docker compose up -d --build
   ```

6. Espere a que PostgreSQL restaure el respaldo inicial. Compruebe el estado con:

   ```bash
   docker compose ps
   docker compose logs -f postgres backend
   ```

7. Abra `http://localhost` en el navegador. Nginx envía internamente las solicitudes `/api` al backend; el backend y PostgreSQL no se publican directamente en el equipo anfitrión.

## Persistencia y restauración inicial

- PostgreSQL usa el volumen Docker `sicrec_postgres_data`.
- `data`, `backups` y `documents` se conservan dentro de `crecuteq-backend` y se montan en el backend.
- En una computadora nueva, si el volumen está vacío, PostgreSQL restaura automáticamente `crecuteq-backend/backups/sicrec_db_dockerizacion_20260811.dump`.
- La restauración inicial se ejecuta una sola vez. Reiniciar o recrear contenedores no vuelve a importar ni reemplaza la base existente.
- `docker compose down` elimina contenedores y red, pero conserva el volumen. Nunca use `docker compose down -v` si desea conservar PostgreSQL.

## Operaciones habituales

```bash
docker compose up -d
docker compose ps
docker compose logs -f
docker compose restart
docker compose down
```

## Crear un respaldo nuevo

```bash
docker compose exec postgres pg_dump -U postgres -d sicrec_db -Fc -f /backups/sicrec_db_manual.dump
```

Si se cambia `POSTGRES_USER` o `POSTGRES_DB` en `.env`, use esos valores en el comando de respaldo.

## Acceso con ngrok

1. Levante SICREC normalmente en el puerto 80.
2. En otra terminal ejecute exclusivamente:

   ```bash
   ngrok http 80
   ```

3. Copie la URL HTTPS indicada por ngrok en `PUBLIC_BASE_URL` dentro de `.env`, sin `/` al final.
4. Aplique esa variable sin tocar PostgreSQL:

   ```bash
   docker compose up -d backend
   ```

Los enlaces nuevos de recuperación de contraseña usarán esa URL. Nginx conserva el encabezado `X-Forwarded-Proto: https` que envía ngrok.

## Puerto configurable

- `FRONTEND_PORT`: interfaz web, predeterminado 80.

El backend y PostgreSQL no se publican hacia el equipo anfitrión: solamente los contenedores de la red privada `sicrec_internal` pueden accederlos.

Si un puerto está ocupado, cambie solamente el valor correspondiente en `.env` y vuelva a ejecutar `docker compose up -d`.
