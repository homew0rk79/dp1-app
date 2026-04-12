import { useMemo, useState } from 'react'
import {
  Search,
  Plus,
  Download,
  FileText,
  Package,
  AlertTriangle,
  Plane,
  CheckCircle2,
  MapPin,
  Clock3,
  ChevronRight,
  PlaneTakeoff,
  Building2,
  X,
  Filter,
  Pencil,
  Trash2,
} from 'lucide-react'

import { ESTADOS_ENVIO } from '../../constants/estados'
import styles from './GestionMaletasModule.module.css'

const ENVIOS_INICIALES = [
  {
    id: 'TRK-2026-001',
    aerolinea: 'LATAM Airlines',
    origen: 'LIM',
    destino: 'MAD',
    origenCiudad: 'Lima',
    destinoCiudad: 'Madrid',
    cantidad: 45,
    estado: ESTADOS_ENVIO.TRANSITO,
    riesgo: 'verde',
    ubicacion: 'En vuelo (Océano Atlántico)',
    plazo: '48h',
    registro: '29/03/2026 08:00',
    estimada: '30/03/2026 22:00',
    progreso: 65,
    ruta: ['LIM', 'BOG', 'MAD'],
    historial: [
      { etapa: 'Registro inicial', detalle: 'Carga registrada en Lima', hora: '29/03/2026 08:00' },
      { etapa: 'Salida', detalle: 'Vuelo LIM → BOG', hora: '29/03/2026 12:30' },
      { etapa: 'Conexión', detalle: 'Nodo intermedio BOG', hora: '29/03/2026 18:40' },
      { etapa: 'En vuelo', detalle: 'Vuelo BOG → MAD', hora: '30/03/2026 02:20' },
    ],
  },
  {
    id: 'TRK-2026-002',
    aerolinea: 'Iberia',
    origen: 'BOG',
    destino: 'UIO',
    origenCiudad: 'Bogotá',
    destinoCiudad: 'Quito',
    cantidad: 120,
    estado: ESTADOS_ENVIO.ALMACEN,
    riesgo: 'ambar',
    ubicacion: 'UIO - Almacén Central',
    plazo: '24h',
    registro: '30/03/2026 02:00',
    estimada: '30/03/2026 20:00',
    progreso: 85,
    ruta: ['BOG', 'UIO'],
    historial: [
      { etapa: 'Registro inicial', detalle: 'Carga registrada en Bogotá', hora: '30/03/2026 02:00' },
      { etapa: 'Arribo', detalle: 'Ingreso a UIO', hora: '30/03/2026 10:15' },
      { etapa: 'Almacén', detalle: 'En espera de salida local', hora: '30/03/2026 11:00' },
    ],
  },
  {
    id: 'TRK-2026-003',
    aerolinea: 'Lufthansa',
    origen: 'FRA',
    destino: 'EZE',
    origenCiudad: 'Frankfurt',
    destinoCiudad: 'Buenos Aires',
    cantidad: 210,
    estado: ESTADOS_ENVIO.DEMORADO,
    riesgo: 'rojo',
    ubicacion: 'FRA - Nodo de Conexión',
    plazo: '48h',
    registro: '27/03/2026 10:00',
    estimada: '29/03/2026 19:00',
    progreso: 100,
    ruta: ['FRA', 'GRU', 'EZE'],
    historial: [
      { etapa: 'Registro inicial', detalle: 'Carga registrada en Frankfurt', hora: '27/03/2026 10:00' },
      { etapa: 'Incidencia', detalle: 'Cancelación de tramo FRA → GRU', hora: '27/03/2026 17:30' },
      { etapa: 'Revisión', detalle: 'Pendiente de replanificación', hora: '28/03/2026 06:45' },
      { etapa: 'Demora', detalle: 'Plazo comprometido excedido', hora: '29/03/2026 20:10' },
    ],
  },
  {
    id: 'TRK-2026-004',
    aerolinea: 'Avianca',
    origen: 'SAL',
    destino: 'MIA',
    origenCiudad: 'San Salvador',
    destinoCiudad: 'Miami',
    cantidad: 60,
    estado: ESTADOS_ENVIO.REPLANIFICADO,
    riesgo: 'ambar',
    ubicacion: 'SAL - Zona de Espera',
    plazo: '24h',
    registro: '29/03/2026 20:00',
    estimada: '30/03/2026 18:20',
    progreso: 45,
    ruta: ['SAL', 'MIA'],
    historial: [
      { etapa: 'Registro inicial', detalle: 'Carga registrada en San Salvador', hora: '29/03/2026 20:00' },
      { etapa: 'Incidencia', detalle: 'Cambio de vuelo asignado', hora: '30/03/2026 03:10' },
      { etapa: 'Replanificación', detalle: 'Ruta ajustada por capacidad', hora: '30/03/2026 03:30' },
    ],
  },
  {
    id: 'TRK-2026-005',
    aerolinea: 'Air France',
    origen: 'CDG',
    destino: 'JFK',
    origenCiudad: 'París',
    destinoCiudad: 'Nueva York',
    cantidad: 88,
    estado: ESTADOS_ENVIO.ENTREGADO,
    riesgo: 'verde',
    ubicacion: 'JFK - Entregado',
    plazo: '48h',
    registro: '28/03/2026 07:20',
    estimada: '29/03/2026 16:40',
    progreso: 100,
    ruta: ['CDG', 'JFK'],
    historial: [
      { etapa: 'Registro inicial', detalle: 'Carga registrada en París', hora: '28/03/2026 07:20' },
      { etapa: 'Salida', detalle: 'Vuelo CDG → JFK', hora: '28/03/2026 14:10' },
      { etapa: 'Entrega', detalle: 'Carga entregada en JFK', hora: '29/03/2026 11:30' },
    ],
  },
]

const FORM_VACIO = {
  aerolinea: '',
  origen: '',
  origenCiudad: '',
  destino: '',
  destinoCiudad: '',
  cantidad: '',
  estado: ESTADOS_ENVIO.TRANSITO,
  riesgo: 'verde',
  plazo: '48h',
  ubicacion: '',
  estimada: '',
}

// ─── Sub-componentes de UI ───────────────────────────────────────────────────

function KpiCard({ titulo, valor, subtitulo, icono, variante = 'default' }) {
  return (
    <article className={`${styles.kpiCard} ${styles[`kpiCard--${variante}`]}`}>
      <div className={styles.kpiHeader}>
        <span className={styles.kpiLabel}>{titulo}</span>
        <div className={styles.kpiIcono}>{icono}</div>
      </div>
      <div className={styles.kpiValor}>{valor}</div>
      <div className={styles.kpiSubtitulo}>{subtitulo}</div>
    </article>
  )
}

function BadgeEstado({ estado }) {
  const mapa = {
    [ESTADOS_ENVIO.TRANSITO]: styles.badgeInfo,
    [ESTADOS_ENVIO.ALMACEN]: styles.badgeNeutral,
    [ESTADOS_ENVIO.REPLANIFICADO]: styles.badgeWarningSoft,
    [ESTADOS_ENVIO.DEMORADO]: styles.badgeDanger,
    [ESTADOS_ENVIO.ENTREGADO]: styles.badgeSuccess,
  }

  return (
    <span className={`${styles.badge} ${mapa[estado] || styles.badgeNeutral}`}>
      {estado}
    </span>
  )
}

function BadgeRiesgo({ riesgo }) {
  const mapa = {
    verde: styles.riesgoVerde,
    ambar: styles.riesgoAmbar,
    rojo: styles.riesgoRojo,
  }

  return <span className={`${styles.riesgoDot} ${mapa[riesgo] || ''}`} />
}

function BotonAccion({ icono, children, primario = false, onClick }) {
  return (
    <button className={primario ? styles.botonPrimario : styles.botonSecundario} onClick={onClick}>
      {icono}
      <span>{children}</span>
    </button>
  )
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

// ─── Modal de formulario (crear / editar) ────────────────────────────────────

function ModalFormulario({ modo, formData, onChange, onGuardar, onCerrar }) {
  const esCrea = modo === 'crear'

  function handleChange(e) {
    const { name, value } = e.target
    onChange({ ...formData, [name]: value })
  }

  function handleSubmit(e) {
    e.preventDefault()
    onGuardar()
  }

  return (
    <div className={styles.modalOverlay} onClick={onCerrar}>
      <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <div>
            <p className={styles.sideEyebrow}>{esCrea ? 'Nuevo envío' : 'Editar envío'}</p>
            <h2 className={styles.sideTitle}>
              {esCrea ? 'Registrar envío' : formData._id}
            </h2>
          </div>
          <button className={styles.iconButton} onClick={onCerrar}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className={styles.modalBody}>
          <div className={styles.formGrid}>
            <div className={`${styles.formGroup} ${styles.formGroupFull}`}>
              <label className={styles.formLabel}>Aerolínea</label>
              <input
                name="aerolinea"
                value={formData.aerolinea}
                onChange={handleChange}
                placeholder="Ej: LATAM Airlines"
                className={styles.input}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Código origen (IATA)</label>
              <input
                name="origen"
                value={formData.origen}
                onChange={handleChange}
                placeholder="Ej: LIM"
                className={styles.input}
                maxLength={3}
                style={{ textTransform: 'uppercase' }}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Ciudad origen</label>
              <input
                name="origenCiudad"
                value={formData.origenCiudad}
                onChange={handleChange}
                placeholder="Ej: Lima"
                className={styles.input}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Código destino (IATA)</label>
              <input
                name="destino"
                value={formData.destino}
                onChange={handleChange}
                placeholder="Ej: MAD"
                className={styles.input}
                maxLength={3}
                style={{ textTransform: 'uppercase' }}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Ciudad destino</label>
              <input
                name="destinoCiudad"
                value={formData.destinoCiudad}
                onChange={handleChange}
                placeholder="Ej: Madrid"
                className={styles.input}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Cantidad de maletas</label>
              <input
                name="cantidad"
                type="number"
                min="1"
                value={formData.cantidad}
                onChange={handleChange}
                placeholder="Ej: 45"
                className={styles.input}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Plazo comprometido</label>
              <input
                name="plazo"
                value={formData.plazo}
                onChange={handleChange}
                placeholder="Ej: 48h"
                className={styles.input}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Estado</label>
              <select
                name="estado"
                value={formData.estado}
                onChange={handleChange}
                className={styles.select}
              >
                <option value={ESTADOS_ENVIO.TRANSITO}>{ESTADOS_ENVIO.TRANSITO}</option>
                <option value={ESTADOS_ENVIO.ALMACEN}>{ESTADOS_ENVIO.ALMACEN}</option>
                <option value={ESTADOS_ENVIO.REPLANIFICADO}>{ESTADOS_ENVIO.REPLANIFICADO}</option>
                <option value={ESTADOS_ENVIO.DEMORADO}>{ESTADOS_ENVIO.DEMORADO}</option>
                <option value={ESTADOS_ENVIO.ENTREGADO}>{ESTADOS_ENVIO.ENTREGADO}</option>
                <option value={ESTADOS_ENVIO.PENDIENTE}>{ESTADOS_ENVIO.PENDIENTE}</option>
              </select>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Nivel de riesgo</label>
              <select
                name="riesgo"
                value={formData.riesgo}
                onChange={handleChange}
                className={styles.select}
              >
                <option value="verde">Verde — Normal</option>
                <option value="ambar">Ámbar — Alerta</option>
                <option value="rojo">Rojo — Crítico</option>
              </select>
            </div>

            <div className={`${styles.formGroup} ${styles.formGroupFull}`}>
              <label className={styles.formLabel}>Ubicación actual</label>
              <input
                name="ubicacion"
                value={formData.ubicacion}
                onChange={handleChange}
                placeholder="Ej: En vuelo (Océano Atlántico)"
                className={styles.input}
              />
            </div>

            <div className={`${styles.formGroup} ${styles.formGroupFull}`}>
              <label className={styles.formLabel}>Entrega estimada</label>
              <input
                name="estimada"
                value={formData.estimada}
                onChange={handleChange}
                placeholder="Ej: 30/03/2026 22:00"
                className={styles.input}
              />
            </div>
          </div>

          <div className={styles.modalFooter}>
            <button type="button" className={styles.botonSecundario} onClick={onCerrar}>
              Cancelar
            </button>
            <button type="submit" className={styles.botonPrimario}>
              {esCrea ? 'Registrar envío' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ─── Modal de confirmación de eliminación ────────────────────────────────────

function ModalEliminar({ envio, onConfirmar, onCerrar }) {
  return (
    <div className={styles.modalOverlay} onClick={onCerrar}>
      <div className={`${styles.modalCard} ${styles.modalCardSmall}`} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <div>
            <p className={styles.sideEyebrow}>Confirmar acción</p>
            <h2 className={styles.sideTitle}>Eliminar envío</h2>
          </div>
          <button className={styles.iconButton} onClick={onCerrar}>
            <X size={18} />
          </button>
        </div>

        <div className={styles.modalBody}>
          <p className={styles.eliminarTexto}>
            ¿Estás seguro de que deseas eliminar el envío{' '}
            <strong>{envio.id}</strong>? Esta acción no se puede deshacer.
          </p>

          <div className={styles.modalFooter}>
            <button className={styles.botonSecundario} onClick={onCerrar}>
              Cancelar
            </button>
            <button className={styles.botonPeligro} onClick={onConfirmar}>
              <Trash2 size={15} />
              Eliminar
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

// ─── Página principal ────────────────────────────────────────────────────────

function GestionMaletasPage() {
  const [envios, setEnvios] = useState(ENVIOS_INICIALES)
  const [query, setQuery] = useState('')
  const [estado, setEstado] = useState('Todos')
  const [riesgo, setRiesgo] = useState('Todos')
  const [aerolinea, setAerolinea] = useState('Todas')
  const [selected, setSelected] = useState(ENVIOS_INICIALES[0])
  const [panelAbierto, setPanelAbierto] = useState(false)

  const [modalMode, setModalMode] = useState(null) // 'crear' | 'editar' | 'eliminar'
  const [formData, setFormData] = useState(FORM_VACIO)
  const [envioEliminar, setEnvioEliminar] = useState(null)

  const aerolineas = useMemo(
    () => ['Todas', ...new Set(envios.map((e) => e.aerolinea))],
    [envios]
  )

  const filtered = useMemo(() => {
    return envios.filter((envio) => {
      const q = query.trim().toLowerCase()

      const matchesQuery =
        !q ||
        envio.id.toLowerCase().includes(q) ||
        envio.aerolinea.toLowerCase().includes(q) ||
        envio.origenCiudad.toLowerCase().includes(q) ||
        envio.destinoCiudad.toLowerCase().includes(q) ||
        envio.origen.toLowerCase().includes(q) ||
        envio.destino.toLowerCase().includes(q)

      const matchesEstado = estado === 'Todos' || envio.estado === estado
      const matchesRiesgo = riesgo === 'Todos' || envio.riesgo === riesgo
      const matchesAerolinea = aerolinea === 'Todas' || envio.aerolinea === aerolinea

      return matchesQuery && matchesEstado && matchesRiesgo && matchesAerolinea
    })
  }, [query, estado, riesgo, aerolinea, envios])

  const kpis = useMemo(() => {
    const activos = envios.filter((e) => e.estado !== ESTADOS_ENVIO.ENTREGADO).length
    const enRiesgo = envios.filter((e) => e.riesgo !== 'verde').length
    const maletasTransito = envios.reduce((acc, e) => acc + Number(e.cantidad), 0)
    const entregados = envios.filter((e) => e.estado === ESTADOS_ENVIO.ENTREGADO).length

    return { activos, enRiesgo, maletasTransito, entregados }
  }, [envios])

  const seleccionarEnvio = (envio) => {
    setSelected(envio)
    setPanelAbierto(true)
  }

  const limpiarFiltros = () => {
    setQuery('')
    setEstado('Todos')
    setRiesgo('Todos')
    setAerolinea('Todas')
  }

  // ── CRUD handlers ────────────────────────────────────────────────────────

  function abrirCrear() {
    setFormData(FORM_VACIO)
    setModalMode('crear')
  }

  function abrirEditar(envio, e) {
    e.stopPropagation()
    setFormData({
      _id: envio.id,
      aerolinea: envio.aerolinea,
      origen: envio.origen,
      origenCiudad: envio.origenCiudad,
      destino: envio.destino,
      destinoCiudad: envio.destinoCiudad,
      cantidad: String(envio.cantidad),
      estado: envio.estado,
      riesgo: envio.riesgo,
      plazo: envio.plazo,
      ubicacion: envio.ubicacion,
      estimada: envio.estimada,
    })
    setModalMode('editar')
  }

  function abrirEliminar(envio, e) {
    e.stopPropagation()
    setEnvioEliminar(envio)
    setModalMode('eliminar')
  }

  function cerrarModal() {
    setModalMode(null)
    setEnvioEliminar(null)
  }

  function guardarCrear() {
    const ahora = new Date()
    const fechaStr = ahora.toLocaleDateString('es-PE', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    }) + ' ' + ahora.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })

    const nuevoId = `TRK-${ahora.getFullYear()}-${String(envios.length + 1).padStart(3, '0')}`
    const nuevo = {
      id: nuevoId,
      aerolinea: formData.aerolinea,
      origen: formData.origen.toUpperCase(),
      destino: formData.destino.toUpperCase(),
      origenCiudad: formData.origenCiudad,
      destinoCiudad: formData.destinoCiudad,
      cantidad: Number(formData.cantidad),
      estado: formData.estado,
      riesgo: formData.riesgo,
      ubicacion: formData.ubicacion || `${formData.origen.toUpperCase()} - Origen`,
      plazo: formData.plazo,
      registro: fechaStr,
      estimada: formData.estimada || '—',
      progreso: 0,
      ruta: [formData.origen.toUpperCase(), formData.destino.toUpperCase()],
      historial: [
        {
          etapa: 'Registro inicial',
          detalle: `Carga registrada en ${formData.origenCiudad}`,
          hora: fechaStr,
        },
      ],
    }

    setEnvios((prev) => [nuevo, ...prev])
    setSelected(nuevo)
    cerrarModal()
  }

  function guardarEditar() {
    setEnvios((prev) =>
      prev.map((e) => {
        if (e.id !== formData._id) return e
        return {
          ...e,
          aerolinea: formData.aerolinea,
          origen: formData.origen.toUpperCase(),
          destino: formData.destino.toUpperCase(),
          origenCiudad: formData.origenCiudad,
          destinoCiudad: formData.destinoCiudad,
          cantidad: Number(formData.cantidad),
          estado: formData.estado,
          riesgo: formData.riesgo,
          ubicacion: formData.ubicacion,
          plazo: formData.plazo,
          estimada: formData.estimada,
          ruta: [formData.origen.toUpperCase(), formData.destino.toUpperCase()],
        }
      })
    )
    if (selected?.id === formData._id) {
      setSelected((prev) => ({
        ...prev,
        aerolinea: formData.aerolinea,
        origen: formData.origen.toUpperCase(),
        destino: formData.destino.toUpperCase(),
        origenCiudad: formData.origenCiudad,
        destinoCiudad: formData.destinoCiudad,
        cantidad: Number(formData.cantidad),
        estado: formData.estado,
        riesgo: formData.riesgo,
        ubicacion: formData.ubicacion,
        plazo: formData.plazo,
        estimada: formData.estimada,
        ruta: [formData.origen.toUpperCase(), formData.destino.toUpperCase()],
      }))
    }
    cerrarModal()
  }

  function confirmarEliminar() {
    setEnvios((prev) => prev.filter((e) => e.id !== envioEliminar.id))
    if (selected?.id === envioEliminar.id) setSelected(null)
    cerrarModal()
  }

  return (
    <div className={styles.page}>
      <div className={styles.layout}>
        <div className={styles.mainColumn}>
          <section className={styles.header}>
            <div className={styles.headerTextos}>
              <h1 className={styles.titulo}>Gestión Operativa de Maletas</h1>
              <p className={styles.subtitulo}>
                Registro, monitoreo y seguimiento de envíos B2B dentro de la red Tasf.B2B.
              </p>
            </div>

            <div className={styles.headerAcciones}>
              <BotonAccion icono={<Download size={16} />}>Exportar</BotonAccion>
              <BotonAccion icono={<FileText size={16} />}>Carga masiva</BotonAccion>
              <BotonAccion icono={<Plus size={16} />} primario onClick={abrirCrear}>
                Registrar envío
              </BotonAccion>
            </div>
          </section>

          <section className={styles.kpisGrid}>
            <KpiCard
              titulo="Envíos activos"
              valor={kpis.activos}
              subtitulo="Operaciones actualmente en curso"
              icono={<Package size={18} />}
              variante="azul"
            />
            <KpiCard
              titulo="En riesgo"
              valor={kpis.enRiesgo}
              subtitulo="Envíos con advertencia o criticidad"
              icono={<AlertTriangle size={18} />}
            />
            <KpiCard
              titulo="Maletas en tránsito"
              valor={kpis.maletasTransito}
              subtitulo="Conteo total de maletas registradas"
              icono={<Plane size={18} />}
            />
            <KpiCard
              titulo="Entregados"
              valor={kpis.entregados}
              subtitulo="Envíos completados en el periodo"
              icono={<CheckCircle2 size={18} />}
              variante="oscuro"
            />
          </section>

          <section className={styles.filtrosCard}>
            <div className={styles.cardTitleRow}>
              <div className={styles.cardTitleIcon}>
                <Filter size={16} />
              </div>
              <div>
                <h2 className={styles.cardTitle}>Filtros de búsqueda</h2>
                <p className={styles.cardSubtitle}>
                  Busca por envío, aerolínea, ciudad o aplica filtros operativos.
                </p>
              </div>
            </div>

            <div className={styles.filtrosGrid}>
              <div className={styles.searchBox}>
                <Search size={18} className={styles.searchIcon} />
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="ID, aerolínea, origen o destino"
                  className={styles.input}
                />
              </div>

              <select
                className={styles.select}
                value={aerolinea}
                onChange={(e) => setAerolinea(e.target.value)}
              >
                {aerolineas.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>

              <select
                className={styles.select}
                value={estado}
                onChange={(e) => setEstado(e.target.value)}
              >
                <option>Todos</option>
                <option value={ESTADOS_ENVIO.TRANSITO}>{ESTADOS_ENVIO.TRANSITO}</option>
                <option value={ESTADOS_ENVIO.ALMACEN}>{ESTADOS_ENVIO.ALMACEN}</option>
                <option value={ESTADOS_ENVIO.REPLANIFICADO}>{ESTADOS_ENVIO.REPLANIFICADO}</option>
                <option value={ESTADOS_ENVIO.DEMORADO}>{ESTADOS_ENVIO.DEMORADO}</option>
                <option value={ESTADOS_ENVIO.ENTREGADO}>{ESTADOS_ENVIO.ENTREGADO}</option>
              </select>

              <select
                className={styles.select}
                value={riesgo}
                onChange={(e) => setRiesgo(e.target.value)}
              >
                <option>Todos</option>
                <option value="verde">Verde</option>
                <option value="ambar">Ámbar</option>
                <option value="rojo">Rojo</option>
              </select>

              <button className={styles.botonLimpiar} onClick={limpiarFiltros}>
                Limpiar
              </button>
            </div>
          </section>

          <section className={styles.tablaCard}>
            <div className={styles.tablaHeader}>
              <div>
                <h2 className={styles.cardTitle}>Envíos registrados</h2>
                <p className={styles.cardSubtitle}>
                  {filtered.length} resultado(s) en la vista actual
                </p>
              </div>

              <button
                className={styles.botonPanelMobile}
                onClick={() => setPanelAbierto(true)}
                disabled={!selected}
              >
                Ver detalle
              </button>
            </div>

            <div className={styles.tablaWrapper}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>ID envío</th>
                    <th>Aerolínea</th>
                    <th>Ruta</th>
                    <th>Cant.</th>
                    <th>Riesgo</th>
                    <th>Ubicación actual</th>
                    <th>Estado</th>
                    <th>Plazo</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((envio) => {
                    const isSelected = selected?.id === envio.id

                    return (
                      <tr
                        key={envio.id}
                        className={isSelected ? styles.rowSelected : ''}
                        onClick={() => seleccionarEnvio(envio)}
                      >
                        <td>
                          <div className={styles.idCell}>
                            <span className={styles.idPrimary}>{envio.id}</span>
                            <span className={styles.idSecondary}>Registro: {envio.registro}</span>
                          </div>
                        </td>

                        <td className={styles.strongCell}>{envio.aerolinea}</td>

                        <td>
                          <div className={styles.rutaCell}>
                            <span className={styles.rutaPill}>{envio.origen}</span>
                            <ChevronRight size={14} />
                            <span className={styles.rutaPill}>{envio.destino}</span>
                          </div>
                        </td>

                        <td className={styles.centerCell}>{envio.cantidad}</td>

                        <td className={styles.centerCell}>
                          <BadgeRiesgo riesgo={envio.riesgo} />
                        </td>

                        <td>
                          <div className={styles.ubicacionCell}>
                            <MapPin size={14} />
                            <span>{envio.ubicacion}</span>
                          </div>
                        </td>

                        <td>
                          <BadgeEstado estado={envio.estado} />
                        </td>

                        <td className={styles.centerCell}>{envio.plazo}</td>

                        <td className={styles.centerCell}>
                          <div className={styles.accionesCell}>
                            <button
                              className={styles.iconButton}
                              title="Editar envío"
                              onClick={(e) => abrirEditar(envio, e)}
                            >
                              <Pencil size={15} />
                            </button>
                            <button
                              className={`${styles.iconButton} ${styles.iconButtonDanger}`}
                              title="Eliminar envío"
                              onClick={(e) => abrirEliminar(envio, e)}
                            >
                              <Trash2 size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>

              {filtered.length === 0 && (
                <div className={styles.emptyState}>
                  <Search size={28} />
                  <h3>No se encontraron envíos</h3>
                  <p>Ajusta los filtros o cambia el término de búsqueda.</p>
                </div>
              )}
            </div>
          </section>
        </div>

        {/* Panel lateral de detalle */}
        <aside className={styles.sidePanel}>
          {selected ? (
            <>
              <div className={styles.sideHeader}>
                <div>
                  <p className={styles.sideEyebrow}>Detalle del envío</p>
                  <h2 className={styles.sideTitle}>{selected.id}</h2>
                </div>
                <div className={styles.sidePanelAcciones}>
                  <button
                    className={styles.iconButton}
                    title="Editar"
                    onClick={(e) => abrirEditar(selected, e)}
                  >
                    <Pencil size={16} />
                  </button>
                  <button
                    className={`${styles.iconButton} ${styles.iconButtonDanger}`}
                    title="Eliminar"
                    onClick={(e) => abrirEliminar(selected, e)}
                  >
                    <Trash2 size={16} />
                  </button>
                  <button className={styles.iconButton} onClick={() => setSelected(null)}>
                    <X size={18} />
                  </button>
                </div>
              </div>

              <div className={styles.sideBody}>
                <section className={styles.progresoCard}>
                  <div className={styles.progresoTop}>
                    <span>Progreso hacia destino</span>
                    <strong>{selected.progreso}%</strong>
                  </div>

                  <div className={styles.progressBar}>
                    <div
                      className={`${styles.progressValue} ${
                        selected.riesgo === 'rojo'
                          ? styles.progressRojo
                          : selected.riesgo === 'ambar'
                          ? styles.progressAmbar
                          : styles.progressAzul
                      }`}
                      style={{ width: `${selected.progreso}%` }}
                    />
                  </div>

                  <div className={styles.progresoBottom}>
                    <span>Plazo comprometido: {selected.plazo}</span>
                    <span
                      className={`${styles.badge} ${
                        selected.riesgo === 'rojo'
                          ? styles.badgeDanger
                          : selected.riesgo === 'ambar'
                          ? styles.badgeWarningSoft
                          : styles.badgeSuccess
                      }`}
                    >
                      {selected.riesgo === 'rojo'
                        ? 'Crítico'
                        : selected.riesgo === 'ambar'
                        ? 'Alerta'
                        : 'Normal'}
                    </span>
                  </div>
                </section>

                <div className={styles.detailGrid}>
                  <FilaDetalle
                    icono={<Building2 size={16} />}
                    label="Aerolínea"
                    value={selected.aerolinea}
                  />
                  <FilaDetalle
                    icono={<PlaneTakeoff size={16} />}
                    label="Ruta principal"
                    value={`${selected.origen} → ${selected.destino}`}
                  />
                  <FilaDetalle
                    icono={<Package size={16} />}
                    label="Cantidad de maletas"
                    value={`${selected.cantidad} maletas`}
                  />
                  <FilaDetalle
                    icono={<MapPin size={16} />}
                    label="Ubicación actual"
                    value={selected.ubicacion}
                  />
                  <FilaDetalle
                    icono={<Clock3 size={16} />}
                    label="Entrega estimada"
                    value={selected.estimada}
                  />
                </div>

                <section className={styles.infoCard}>
                  <div className={styles.infoHeader}>
                    <h3>Tramos de la ruta</h3>
                    <BadgeEstado estado={selected.estado} />
                  </div>

                  <div className={styles.tramos}>
                    {selected.ruta.map((stop, i) => (
                      <div className={styles.tramoItem} key={`${stop}-${i}`}>
                        <span className={styles.rutaPill}>{stop}</span>
                        {i < selected.ruta.length - 1 && <ChevronRight size={14} />}
                      </div>
                    ))}
                  </div>
                </section>

                <section className={styles.infoCard}>
                  <h3 className={styles.historialTitulo}>Historial operativo</h3>

                  <div className={styles.timeline}>
                    {selected.historial.map((item, idx) => (
                      <div className={styles.timelineItem} key={`${item.hora}-${idx}`}>
                        <div className={styles.timelineDot} />
                        <div className={styles.timelineContent}>
                          <div className={styles.timelineEtapa}>{item.etapa}</div>
                          <div className={styles.timelineDetalle}>{item.detalle}</div>
                          <div className={styles.timelineHora}>{item.hora}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>

                <button className={styles.botonPlan}>
                  <FileText size={16} />
                  <span>Ver plan de viaje completo</span>
                </button>
              </div>
            </>
          ) : (
            <div className={styles.emptyPanel}>
              <Package size={26} />
              <h3>Selecciona un envío</h3>
              <p>El detalle operativo aparecerá en este panel.</p>
            </div>
          )}
        </aside>
      </div>

      {/* Panel mobile overlay */}
      {panelAbierto && selected && (
        <div className={styles.mobileOverlay} onClick={() => setPanelAbierto(false)}>
          <div className={styles.mobilePanel} onClick={(e) => e.stopPropagation()}>
            <div className={styles.sideHeader}>
              <div>
                <p className={styles.sideEyebrow}>Detalle del envío</p>
                <h2 className={styles.sideTitle}>{selected.id}</h2>
              </div>
              <button className={styles.iconButton} onClick={() => setPanelAbierto(false)}>
                <X size={18} />
              </button>
            </div>

            <div className={styles.sideBody}>
              <section className={styles.progresoCard}>
                <div className={styles.progresoTop}>
                  <span>Progreso hacia destino</span>
                  <strong>{selected.progreso}%</strong>
                </div>

                <div className={styles.progressBar}>
                  <div
                    className={`${styles.progressValue} ${
                      selected.riesgo === 'rojo'
                        ? styles.progressRojo
                        : selected.riesgo === 'ambar'
                        ? styles.progressAmbar
                        : styles.progressAzul
                    }`}
                    style={{ width: `${selected.progreso}%` }}
                  />
                </div>

                <div className={styles.progresoBottom}>
                  <span>Plazo comprometido: {selected.plazo}</span>
                </div>
              </section>

              <div className={styles.detailGrid}>
                <FilaDetalle
                  icono={<Building2 size={16} />}
                  label="Aerolínea"
                  value={selected.aerolinea}
                />
                <FilaDetalle
                  icono={<PlaneTakeoff size={16} />}
                  label="Ruta principal"
                  value={`${selected.origen} → ${selected.destino}`}
                />
                <FilaDetalle
                  icono={<Package size={16} />}
                  label="Cantidad de maletas"
                  value={`${selected.cantidad} maletas`}
                />
                <FilaDetalle
                  icono={<MapPin size={16} />}
                  label="Ubicación actual"
                  value={selected.ubicacion}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modales CRUD */}
      {(modalMode === 'crear' || modalMode === 'editar') && (
        <ModalFormulario
          modo={modalMode}
          formData={formData}
          onChange={setFormData}
          onGuardar={modalMode === 'crear' ? guardarCrear : guardarEditar}
          onCerrar={cerrarModal}
        />
      )}

      {modalMode === 'eliminar' && envioEliminar && (
        <ModalEliminar
          envio={envioEliminar}
          onConfirmar={confirmarEliminar}
          onCerrar={cerrarModal}
        />
      )}
    </div>
  )
}

export default GestionMaletasPage
