import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  MapPin,
  Clock3,
  Plane,
  ChevronRight,
} from 'lucide-react'

import BarraProgreso from '../../components/common/BarraProgreso/BarraProgreso'
import Badge from '../../components/common/Badge/Badge'
import { obtenerDetalleRuta } from '../../services/rutasService'
import styles from './GestionRutasModule.module.css'

function textoEstado(estado) {
  const m = {
    pendiente: 'Pendiente',
    en_transito: 'En tránsito',
    completado: 'Completado',
  }
  return m[estado] ?? estado
}

function varianteBarra(cumplimiento) {
  if (cumplimiento === 'rojo') return 'rojo'
  if (cumplimiento === 'ambar') return 'ambar'
  return 'azul'
}

function FilaDetalle({ icono, label, value }) {
  return (
    <div className={styles.detailRow}>
      <div className={styles.detailIcon}>{icono}</div>
      <div>
        <div className={styles.detailLabel}>{label}</div>
        <div className={styles.detailValue}>{value}</div>
      </div>
    </div>
  )
}

function DetalleRuta() {
  const { id } = useParams()
  const [detalle, setDetalle] = useState(null)
  const [cargando, setCargando] = useState(true)

  const cargar = useCallback(async () => {
    if (!id) return
    setCargando(true)
    try {
      const d = await obtenerDetalleRuta(id)
      setDetalle(d)
    } finally {
      setCargando(false)
    }
  }, [id])

  useEffect(() => {
    cargar()
  }, [cargar])

  if (cargando) {
    return (
      <div className={styles.page}>
        <div className={styles.singleColumn}>
          <p style={{ color: '#64748b' }}>Cargando detalle de ruta…</p>
        </div>
      </div>
    )
  }

  if (!detalle) {
    return (
      <div className={styles.page}>
        <div className={styles.singleColumn}>
          <Link className={styles.backLink} to="/gestion-rutas">
            <ArrowLeft size={18} />
            Volver al listado
          </Link>
          <p style={{ color: '#b91c1c', fontWeight: 600 }}>No se encontró la ruta solicitada.</p>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <div className={styles.singleColumn}>
        <header className={styles.detalleHeader}>
          <Link className={styles.backLink} to="/gestion-rutas">
            <ArrowLeft size={18} />
            Volver a gestión de rutas
          </Link>
          <h1 className={styles.detalleTitulo}>Detalle de ruta · {detalle.id}</h1>
          <p className={styles.subtitulo} style={{ margin: 0 }}>
            {detalle.origenCiudad} ({detalle.origen}) → {detalle.destinoCiudad} ({detalle.destino})
          </p>
        </header>

        <section className={styles.progresoCard} style={{ marginBottom: 16 }}>
          <div className={styles.progresoTop}>
            <span>Progreso hacia destino</span>
            <strong>{detalle.progreso}%</strong>
          </div>
          <BarraProgreso porcentaje={detalle.progreso} variante={varianteBarra(detalle.cumplimiento)} />
          <div className={styles.progresoBottom}>
            <span>{detalle.plazoCompromiso}</span>
            <Badge tipo={detalle.cumplimiento === 'rojo' ? 'rojo' : detalle.cumplimiento === 'ambar' ? 'ambar' : 'verde'}>
              Semáforo de cumplimiento
            </Badge>
          </div>
        </section>

        <section className={styles.infoCard} style={{ marginBottom: 16 }}>
          <div className={styles.infoHeader}>
            <h3>Información general</h3>
            <Badge tipo="info">{textoEstado(detalle.estado)}</Badge>
          </div>
          <div className={styles.detailGrid}>
            <FilaDetalle
              icono={<MapPin size={16} />}
              label="Origen y destino"
              value={`${detalle.origen} → ${detalle.destino}`}
            />
            <FilaDetalle icono={<Clock3 size={16} />} label="Plazo comprometido" value={detalle.plazoCompromiso} />
            <FilaDetalle icono={<Clock3 size={16} />} label="Ingreso / límite" value={`${detalle.fechaIngreso} · ${detalle.fechaLimite}`} />
            <FilaDetalle
              icono={<Plane size={16} />}
              label="Tiempo estimado restante"
              value={detalle.tiempoEstimado}
            />
          </div>
          {detalle.planAlternativoLabel ? (
            <p style={{ margin: '12px 0 0', fontSize: '0.88rem', color: '#c2410c' }}>
              Plan alternativo activo: <strong>{detalle.planAlternativoLabel}</strong>
            </p>
          ) : null}
        </section>

        <section className={styles.infoCard}>
          <div className={styles.infoHeader}>
            <h3>Tramos</h3>
          </div>
          <table className={styles.tramosInnerTable}>
            <thead>
              <tr>
                <th>Vuelo</th>
                <th>Capacidad</th>
                <th>Salida</th>
                <th>Llegada</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {detalle.tramos.map((t) => (
                <tr key={t.id}>
                  <td>
                    <span className={styles.rutaPill} style={{ fontFamily: 'monospace' }}>
                      {t.vuelo}
                    </span>
                  </td>
                  <td>
                    {t.ocupacion} / {t.capacidadMax}
                  </td>
                  <td>{t.salida}</td>
                  <td>{t.llegada}</td>
                  <td>{textoEstado(t.estado)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className={styles.infoCard} style={{ marginTop: 16 }}>
          <h3 style={{ margin: '0 0 10px', fontSize: '0.95rem', fontWeight: 700, color: '#0f172a' }}>
            Resumen de la secuencia
          </h3>
          <div className={styles.tramos}>
            <span className={styles.rutaPill}>{detalle.origen}</span>
            <ChevronRight size={14} color="#94a3b8" />
            <span className={styles.rutaPill}>{detalle.destino}</span>
          </div>
          <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '10px 0 0' }}>
            Los vuelos por tramo se detallan en la tabla superior.
          </p>
        </section>

      </div>
    </div>
  )
}

export default DetalleRuta
