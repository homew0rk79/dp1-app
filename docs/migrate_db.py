#!/usr/bin/env python3
"""
Migración masiva TXT -> PostgreSQL RDS  —  Tasf.B2B / DP1 PUCP
Usa PostgreSQL COPY para carga masiva (100x más rápido que INSERT por JPA).

Requisito: pip3 install psycopg2-binary

Uso:
  python3 migrate_db.py              # migra vuelos + envios
  python3 migrate_db.py --vuelos     # solo vuelos
  python3 migrate_db.py --envios     # solo envios
"""

import psycopg2
import sys
import os
import re
import time
from io import StringIO

# ── Configuración ─────────────────────────────────────────────────────────────
DB_HOST = "tasfb2b.ccu5flzm04pi.us-east-1.rds.amazonaws.com"
DB_PORT = 5432
DB_NAME = "tasfb2b"
DB_USER = "postgres"
DB_PASS = "dp1tasfb2b"

PLANES_VUELO = "/home/1inf54.984.6f/app/data/planes_vuelo.txt"
ENVIOS_DIR   = "/home/1inf54.984.6f/app/data/_envios_preliminar_"

BATCH = 100_000  # filas por batch de COPY (ajustar si hay problemas de memoria)
# ─────────────────────────────────────────────────────────────────────────────


def conectar():
    print("Conectando a RDS...", end=" ", flush=True)
    conn = psycopg2.connect(
        host=DB_HOST, port=DB_PORT, dbname=DB_NAME,
        user=DB_USER, password=DB_PASS, connect_timeout=30
    )
    conn.autocommit = False
    print("OK")
    return conn


# ── Vuelos ────────────────────────────────────────────────────────────────────

def parsear_hora(s):
    h, m = s.split(":")
    return int(h) * 60 + int(m)


def migrar_vuelos(conn):
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM vuelo")
    if cur.fetchone()[0] > 0:
        print("Vuelos: ya existen en DB, saltando.")
        cur.close()
        return

    print("Vuelos: parseando archivo...", end=" ", flush=True)
    buf = StringIO()
    n = 0
    with open(PLANES_VUELO, encoding="ascii") as f:
        for linea in f:
            linea = linea.strip()
            if not linea:
                continue
            p = linea.split("-")
            if len(p) < 5:
                continue
            origen    = p[0]
            destino   = p[1]
            salida    = parsear_hora(p[2])
            llegada   = parsear_hora(p[3])
            capacidad = int(p[4])
            buf.write(f"{origen}\t{destino}\t{salida}\t{llegada}\t{capacidad}\t0\n")
            n += 1

    buf.seek(0)
    cur.copy_from(buf, "vuelo",
                  columns=("origen", "destino", "salida_minutos",
                            "llegada_minutos", "capacidad_max", "ocupacion"))
    conn.commit()
    cur.close()
    print(f"{n:,} vuelos insertados.")


# ── Envíos ────────────────────────────────────────────────────────────────────

PATRON_ARCHIVO = re.compile(r"_envios_([A-Z]{4})_\.txt$")


def migrar_envios(conn):
    archivos = sorted([
        f for f in os.listdir(ENVIOS_DIR) if PATRON_ARCHIVO.match(f)
    ])
    if not archivos:
        print(f"ERROR: No se encontraron archivos de envíos en {ENVIOS_DIR}")
        sys.exit(1)

    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM envio")
    n_existentes = cur.fetchone()[0]
    cur.close()
    print(f"Envíos actuales en DB: {n_existentes:,}")
    print(f"Archivos a procesar:   {len(archivos)}")
    print()

    total = 0
    for nombre in archivos:
        origen = PATRON_ARCHIVO.match(nombre).group(1)
        ruta   = os.path.join(ENVIOS_DIR, nombre)
        n, t   = _migrar_archivo_envios(conn, ruta, origen)
        total += n
        print(f"  {nombre}: {n:,} filas  ({t:.1f}s)")

    print(f"\nTotal envíos insertados: {total:,}")


def _migrar_archivo_envios(conn, ruta, origen):
    """Carga un archivo de envíos en envio_tmp con COPY y luego inserta ignorando duplicados."""
    cur = conn.cursor()

    # Tabla temporal por archivo; se borra al hacer COMMIT.
    cur.execute("""
        CREATE TEMP TABLE envio_tmp (
            id_envio             TEXT,
            id_original          TEXT,
            origen               TEXT,
            destino              TEXT,
            fecha_hora_registro  TIMESTAMP,
            cantidad             INT,
            id_cliente           TEXT,
            entregado            BOOLEAN,
            plazo_maximo_minutos INT
        ) ON COMMIT DROP
    """)

    t0 = time.time()
    n_total = 0
    buf     = StringIO()
    n_buf   = 0

    def flush_buf():
        nonlocal n_buf
        if n_buf == 0:
            return
        buf.seek(0)
        cur.copy_from(buf, "envio_tmp", columns=(
            "id_envio", "id_original", "origen", "destino",
            "fecha_hora_registro", "cantidad", "id_cliente",
            "entregado", "plazo_maximo_minutos"
        ))
        buf.seek(0)
        buf.truncate()
        n_buf = 0

    with open(ruta, encoding="ascii") as f:
        for linea in f:
            linea = linea.strip()
            if not linea:
                continue
            p = linea.split("-")
            if len(p) < 7:
                continue
            try:
                id_original = p[0]
                fecha       = p[1]  # "20260102"
                hh          = int(p[2])
                mm          = int(p[3])
                destino     = p[4]
                cantidad    = int(p[5])
                id_cliente  = p[6]
                id_envio    = f"{origen}-{id_original}"
                ts          = f"{fecha[:4]}-{fecha[4:6]}-{fecha[6:8]} {hh:02d}:{mm:02d}:00"
                buf.write(
                    f"{id_envio}\t{id_original}\t{origen}\t{destino}"
                    f"\t{ts}\t{cantidad}\t{id_cliente}\tfalse\t0\n"
                )
                n_buf   += 1
                n_total += 1
            except (ValueError, IndexError):
                continue

            if n_buf >= BATCH:
                flush_buf()

    flush_buf()

    # Insertar desde tmp ignorando duplicados (idempotente)
    cur.execute("""
        INSERT INTO envio
            (id_envio, id_original, origen, destino,
             fecha_hora_registro, cantidad, id_cliente,
             entregado, plazo_maximo_minutos)
        SELECT id_envio, id_original, origen, destino,
               fecha_hora_registro, cantidad, id_cliente,
               entregado, plazo_maximo_minutos
        FROM envio_tmp
        ON CONFLICT (id_envio) DO NOTHING
    """)
    conn.commit()  # aquí se borra envio_tmp (ON COMMIT DROP)
    cur.close()
    return n_total, time.time() - t0


# ── Main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    args   = sys.argv[1:]
    vuelos = not args or "--vuelos" in args
    envios = not args or "--envios" in args

    conn = conectar()

    if vuelos:
        migrar_vuelos(conn)

    if envios:
        if vuelos:
            print()
        migrar_envios(conn)

    conn.close()
    print("\nMigración completada.")
