import { useEffect, useState } from 'react'

const TABS = [
  { id: 'mapa', label: 'Mapa' },
  { id: 'almacenes', label: 'Almacenes' },
  { id: 'ut', label: 'UT' },
  { id: 'tramos', label: 'Tramos' },
]

const REGISTROS_POR_PAGINA = 10

const almacenVacio = {
  id: null,
  codigo: '',
  nombre: '',
  ciudad: '',
  pais: '',
  continente: '',
  lat: '',
  lng: '',
  capacidadMax: '',
  ocupacionActual: 0,
}

const unidadVacia = {
  id: null,
  codigo: '',
  tipo: 'Avión',
  ubicacionActual: '',
  capacidadMax: '',
  estado: 'Disponible',
}

const tramoVacio = {
  id: null,
  utAsignada: '',
  origen: '',
  destino: '',
  horaSalida: '',
  horaLlegada: '',
  estado: 'Programado',
}

function toDatetimeLocal(value) {
  if (!value) return ''
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(value) && !value.endsWith('Z')) {
    return value.slice(0, 16)
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 16)
}

function parseNumero(value) {
  if (value === '' || value === null || value === undefined) return NaN
  return Number(value)
}

function mensajeError(err, fallback) {
  const data = err?.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data?.message) return data.message
  if (data?.error && data?.path) return `${data.error}: ${data.path}`
  if (err?.message) return err.message
  return fallback
}

function PanelConfiguracionMapa({
  abierto,
  onCerrar,
  configMapa,
  onConfigMapaChange,
  onGuardarConfigMapa,
  almacenes,
  onGuardarAlmacen,
  unidades,
  onGuardarUnidad,
  tramos,
  onGuardarTramo,
  velocidad,
  onVelocidad,
  styles,
}) {
  const [tab, setTab] = useState('mapa')
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(null)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [confirmacion, setConfirmacion] = useState(null)
  const [paginas, setPaginas] = useState({
    almacenes: 1,
    ut: 1,
    tramos: 1,
  })
  const [busquedas, setBusquedas] = useState({
    almacenes: '',
    ut: '',
    tramos: '',
  })
  const [camposMapa, setCamposMapa] = useState({
    zoomInicial: String(configMapa.zoomInicial ?? ''),
    centroLat: String(configMapa.centroLat ?? ''),
    centroLng: String(configMapa.centroLng ?? ''),
  })

  useEffect(() => {
    setCamposMapa({
      zoomInicial: String(configMapa.zoomInicial ?? ''),
      centroLat: String(configMapa.centroLat ?? ''),
      centroLng: String(configMapa.centroLng ?? ''),
    })
  }, [configMapa.zoomInicial, configMapa.centroLat, configMapa.centroLng])

  const almacenesFiltrados = filtrarItems(almacenes, busquedas.almacenes, (a) => [
    a.codigo,
    a.nombre,
    a.ciudad,
    a.pais,
    a.continente,
    a.capacidadMax,
  ])
  const unidadesFiltradas = filtrarItems(unidades, busquedas.ut, (ut) => [
    ut.codigo,
    ut.tipo,
    ut.ubicacionActual,
    ut.estado,
    ut.capacidadMax,
  ])
  const tramosFiltrados = filtrarItems(tramos, busquedas.tramos, (tramo) => [
    tramo.utAsignada,
    tramo.origen,
    tramo.destino,
    tramo.estado,
    tramo.horaSalida,
    tramo.horaLlegada,
  ])

  if (!abierto) return null

  function actualizarConfig(campo, valor, persistir = true) {
    const siguiente = { ...configMapa, [campo]: valor }
    onConfigMapaChange(siguiente)
    if (persistir) onGuardarConfigMapa?.(siguiente)
  }

  function validarCampoMapa(campo) {
    const valor = String(camposMapa[campo] ?? '').trim()
    const numero = valor === '' ? 0 : Number(valor)
    if (!Number.isFinite(numero)) {
      setErrores({ mapa: 'El valor ingresado no es válido. Se usará 0 como respaldo.' })
      actualizarConfig(campo, 0)
      setCamposMapa((actual) => ({ ...actual, [campo]: '0' }))
      return
    }
    setErrores((actual) => ({ ...actual, mapa: null }))
    if (campo === 'zoomInicial') {
      const zoom = Math.max(0, Math.min(18, Math.round(numero)))
      actualizarConfig('zoomInicial', zoom)
      setCamposMapa((actual) => ({ ...actual, zoomInicial: String(zoom) }))
      return
    }
    actualizarConfig(campo, numero)
    setCamposMapa((actual) => ({ ...actual, [campo]: String(numero) }))
  }

  function confirmarCampoMapa(e, campo) {
    if (e.key === 'Enter') {
      e.preventDefault()
      validarCampoMapa(campo)
    }
  }

  function abrirModal(tipo, registro = null) {
    if (!registro) return
    setErrores({})
    setMensaje(null)
    setConfirmacion(null)
    setModal(tipo)

    if (tipo === 'almacen') {
      setForm({
        ...almacenVacio,
        ...registro,
        capacidadMax: registro.capacidadMax ?? '',
        lat: registro.lat ?? '',
        lng: registro.lng ?? '',
      })
    }
    if (tipo === 'ut') {
      setForm({ ...unidadVacia, ...registro, capacidadMax: registro.capacidadMax ?? '' })
    }
    if (tipo === 'tramo') {
      setForm({
        ...tramoVacio,
        ...registro,
        horaSalida: toDatetimeLocal(registro.horaSalida),
        horaLlegada: toDatetimeLocal(registro.horaLlegada),
      })
    }
  }

  function cerrarModal() {
    setModal(null)
    setForm(null)
    setErrores({})
    setMensaje(null)
    setConfirmacion(null)
  }

  function cerrarFormularioConExito(texto) {
    setModal(null)
    setForm(null)
    setErrores({})
    setConfirmacion(null)
    setMensaje({ tipo: 'exito', texto })
  }

  function cambiarPagina(tipo, siguiente) {
    setPaginas((actual) => ({ ...actual, [tipo]: siguiente }))
  }

  function actualizarBusqueda(tipo, valor) {
    setBusquedas((actual) => ({ ...actual, [tipo]: valor }))
    setPaginas((actual) => ({ ...actual, [tipo]: 1 }))
  }

  function guardarAlmacen(e) {
    e.preventDefault()
    const capacidadMax = parseNumero(form.capacidadMax)
    if (!Number.isFinite(capacidadMax) || capacidadMax <= 0) {
      setErrores({ modal: 'La capacidad es obligatoria y debe ser mayor a cero.' })
      return
    }
    setErrores({})
    setConfirmacion({
      mensaje: 'Modificar la capacidad del almacén puede afectar la simulación. ¿Deseas continuar?',
      accion: () => ejecutarGuardarAlmacen(capacidadMax),
    })
  }

  function guardarUnidad(e) {
    e.preventDefault()
    const capacidadMax = parseNumero(form.capacidadMax)
    if (!Number.isFinite(capacidadMax) || capacidadMax <= 0) {
      setErrores({ modal: 'La capacidad es obligatoria y debe ser mayor a cero.' })
      return
    }
    setErrores({})
    setConfirmacion({
      mensaje: 'Modificar la capacidad de la UT puede afectar la planificación de vuelos. ¿Deseas continuar?',
      accion: () => ejecutarGuardarUnidad(capacidadMax),
    })
  }

  function guardarTramo(e) {
    e.preventDefault()
    const origen = (form.origen || '').trim().toUpperCase()
    const destino = (form.destino || '').trim().toUpperCase()
    if (!origen) {
      setErrores({ modal: 'El código ICAO de origen es obligatorio.' })
      return
    }
    if (!destino) {
      setErrores({ modal: 'El código ICAO de destino es obligatorio.' })
      return
    }
    if (origen === destino) {
      setErrores({ modal: 'Origen y destino no pueden ser iguales.' })
      return
    }
    if (!form.horaSalida) {
      setErrores({ modal: 'La hora de salida es obligatoria.' })
      return
    }
    if (!form.horaLlegada) {
      setErrores({ modal: 'La hora de llegada es obligatoria.' })
      return
    }
    if (new Date(form.horaLlegada) <= new Date(form.horaSalida)) {
      setErrores({ modal: 'La llegada debe ser posterior a la salida.' })
      return
    }
    setErrores({})
    setConfirmacion({
      mensaje: 'Modificar el origen, destino u horarios puede afectar la simulación. ¿Deseas continuar?',
      accion: ejecutarGuardarTramo,
    })
  }

  async function ejecutarGuardarAlmacen(capacidadMax) {
    setErrores({})
    try {
      await onGuardarAlmacen({ ...form, capacidadMax })
      cerrarFormularioConExito('Capacidad del almacén guardada correctamente.')
    } catch (err) {
      setConfirmacion(null)
      setErrores({ modal: mensajeError(err, 'No se pudo guardar el almacén.') })
    }
  }

  async function ejecutarGuardarUnidad(capacidadMax) {
    setErrores({})
    try {
      await onGuardarUnidad({ ...form, capacidadMax })
      cerrarFormularioConExito('Capacidad de la UT guardada correctamente.')
    } catch (err) {
      setConfirmacion(null)
      setErrores({ modal: mensajeError(err, 'No se pudo guardar la UT.') })
    }
  }

  async function ejecutarGuardarTramo() {
    setErrores({})
    try {
      await onGuardarTramo({
        ...form,
        origen: (form.origen || '').trim().toUpperCase(),
        destino: (form.destino || '').trim().toUpperCase(),
      })
      cerrarFormularioConExito('Tramo guardado correctamente.')
    } catch (err) {
      setConfirmacion(null)
      setErrores({ modal: mensajeError(err, 'No se pudo guardar el tramo.') })
    }
  }

  return (
    <>
      <aside className={styles.configPanel} aria-label="Configuración y mantenimiento del mapa">
        <header className={styles.configHeader}>
          <div>
            <span>Panel principal</span>
            <h3>Configuración</h3>
          </div>
          <button type="button" onClick={onCerrar} aria-label="Cerrar configuración">x</button>
        </header>

        <div className={styles.configTabs}>
          {TABS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={tab === item.id ? styles.tabActivo : ''}
              onClick={() => setTab(item.id)}
            >
              {item.label}
            </button>
          ))}
        </div>

        {mensaje?.texto && <p className={styles.mensajeExito}>{mensaje.texto}</p>}

        {tab === 'mapa' && (
          <section className={styles.configSection}>
            <label className={styles.checkRow}>
              <input type="checkbox" checked={configMapa.mostrarAlmacenes} onChange={(e) => actualizarConfig('mostrarAlmacenes', e.target.checked)} />
              Mostrar aeropuertos o almacenes
            </label>
            <label className={styles.checkRow}>
              <input type="checkbox" checked={configMapa.mostrarUT} onChange={(e) => actualizarConfig('mostrarUT', e.target.checked)} />
              Mostrar UT en simulación
            </label>
            <label className={styles.checkRow}>
              <input type="checkbox" checked={configMapa.mostrarTramos} onChange={(e) => actualizarConfig('mostrarTramos', e.target.checked)} />
              Mostrar rutas o tramos
            </label>

            <div className={styles.formGrid}>
              <label>
                Zoom inicial
                <input
                  type="text"
                  inputMode="numeric"
                  value={camposMapa.zoomInicial}
                  onChange={(e) => setCamposMapa({ ...camposMapa, zoomInicial: e.target.value })}
                  onKeyDown={(e) => confirmarCampoMapa(e, 'zoomInicial')}
                  onBlur={() => validarCampoMapa('zoomInicial')}
                />
              </label>
              <label>
                Centro latitud
                <input
                  type="text"
                  inputMode="decimal"
                  value={camposMapa.centroLat}
                  onChange={(e) => setCamposMapa({ ...camposMapa, centroLat: e.target.value })}
                  onKeyDown={(e) => confirmarCampoMapa(e, 'centroLat')}
                  onBlur={() => validarCampoMapa('centroLat')}
                />
              </label>
              <label>
                Centro longitud
                <input
                  type="text"
                  inputMode="decimal"
                  value={camposMapa.centroLng}
                  onChange={(e) => setCamposMapa({ ...camposMapa, centroLng: e.target.value })}
                  onKeyDown={(e) => confirmarCampoMapa(e, 'centroLng')}
                  onBlur={() => validarCampoMapa('centroLng')}
                />
              </label>
            </div>
            {errores.mapa && <p className={styles.error}>{errores.mapa}</p>}

            <div className={styles.formGrid}>
              <label>
                Color de rutas/tramos
                <input type="color" value={configMapa.colorTramos} onChange={(e) => actualizarConfig('colorTramos', e.target.value)} />
              </label>
              <label>
                Color de almacenes
                <input type="color" value={configMapa.colorAlmacenes} onChange={(e) => actualizarConfig('colorAlmacenes', e.target.value)} />
              </label>
            </div>

            <label>
              Velocidad visual de simulación
              <input type="range" min="1" max="240" step="1" value={velocidad} onChange={(e) => onVelocidad(Number(e.target.value))} />
              <strong>{velocidad} min/s</strong>
            </label>
          </section>
        )}

        {tab === 'almacenes' && (
          <section className={styles.configSection}>
            <BuscadorMantenimiento
              valor={busquedas.almacenes}
              onChange={(valor) => actualizarBusqueda('almacenes', valor)}
              placeholder="Buscar almacén por código, ciudad o país"
              styles={styles}
            />
            <ListaPaginada
              items={almacenesFiltrados}
              pagina={paginas.almacenes}
              onPagina={(pagina) => cambiarPagina('almacenes', pagina)}
              styles={styles}
              render={(a) => (
                <>
                  <strong>{a.codigo} - {a.nombre || a.ciudad}</strong>
                  <span>{a.ciudad}, {a.pais} - Capacidad {a.capacidadMax}</span>
                  <button type="button" onClick={() => abrirModal('almacen', a)}>Editar</button>
                </>
              )}
            />
          </section>
        )}

        {tab === 'ut' && (
          <section className={styles.configSection}>
            <BuscadorMantenimiento
              valor={busquedas.ut}
              onChange={(valor) => actualizarBusqueda('ut', valor)}
              placeholder="Buscar UT por código, base o estado"
              styles={styles}
            />
            <ListaPaginada
              items={unidadesFiltradas}
              pagina={paginas.ut}
              onPagina={(pagina) => cambiarPagina('ut', pagina)}
              styles={styles}
              render={(ut) => (
                <>
                  <strong>{ut.codigo}</strong>
                  <span>{ut.tipo} - {ut.ubicacionActual} - Capacidad {ut.capacidadMax} - {ut.estado}</span>
                  <button type="button" onClick={() => abrirModal('ut', ut)}>Editar</button>
                </>
              )}
            />
          </section>
        )}

        {tab === 'tramos' && (
          <section className={styles.configSection}>
            <BuscadorMantenimiento
              valor={busquedas.tramos}
              onChange={(valor) => actualizarBusqueda('tramos', valor)}
              placeholder="Buscar tramo por UT, origen, destino o estado"
              styles={styles}
            />
            <ListaPaginada
              items={tramosFiltrados}
              pagina={paginas.tramos}
              onPagina={(pagina) => cambiarPagina('tramos', pagina)}
              styles={styles}
              render={(tramo) => (
                <>
                  <strong>{tramo.utAsignada}: {tramo.origen} - {tramo.destino}</strong>
                  <span>{toDatetimeLocal(tramo.horaSalida).replace('T', ' ')} / {toDatetimeLocal(tramo.horaLlegada).replace('T', ' ')} - {tramo.estado}</span>
                  <button type="button" onClick={() => abrirModal('tramo', tramo)}>Editar</button>
                </>
              )}
            />
          </section>
        )}
      </aside>

      {modal && (
        <div className={styles.modalOverlay} role="presentation">
          <div className={styles.modalConfig} role="dialog" aria-modal="true">
            <header className={styles.modalHeader}>
              <h3>{tituloModal(modal)}</h3>
              <button type="button" onClick={cerrarModal} aria-label="Cerrar formulario">x</button>
            </header>
            {modal === 'almacen' && (
              <form onSubmit={guardarAlmacen} className={styles.formMantenimiento}>
                <CamposAlmacen form={form} setForm={setForm} styles={styles} />
                <AccionesModal errores={errores} styles={styles} />
              </form>
            )}
            {modal === 'ut' && (
              <form onSubmit={guardarUnidad} className={styles.formMantenimiento}>
                <CamposUT form={form} setForm={setForm} styles={styles} />
                <AccionesModal errores={errores} styles={styles} />
              </form>
            )}
            {modal === 'tramo' && (
              <form onSubmit={guardarTramo} className={styles.formMantenimiento}>
                <CamposTramo form={form} setForm={setForm} styles={styles} />
                <AccionesModal errores={errores} styles={styles} />
              </form>
            )}
          </div>
        </div>
      )}

      {confirmacion && (
        <div className={styles.modalOverlay} role="presentation">
          <div className={styles.modalConfig} role="dialog" aria-modal="true">
            <header className={styles.modalHeader}>
              <h3>Confirmar modificación</h3>
              <button type="button" onClick={() => setConfirmacion(null)} aria-label="Cerrar confirmación">x</button>
            </header>
            <div className={styles.confirmacionContenido}>
              <p>{confirmacion.mensaje}</p>
              <div className={styles.modalActions}>
                <button type="button" className={styles.btnSecundario} onClick={() => setConfirmacion(null)}>Cancelar</button>
                <button type="button" className={styles.btnPrimario} onClick={confirmacion.accion}>Confirmar</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function tituloModal(tipo) {
  if (tipo === 'almacen') return 'Editar almacén'
  if (tipo === 'ut') return 'Editar UT'
  return 'Editar tramo'
}

function CampoBloqueado({ label, value, styles }) {
  return (
    <label className={styles.campoBloqueado}>
      <span className={styles.labelConCandado}>{label}<span aria-hidden="true">🔒</span></span>
      <input value={value ?? ''} readOnly aria-readonly="true" />
    </label>
  )
}

function CamposAlmacen({ form, setForm, styles }) {
  return (
    <div className={styles.formGrid}>
      <CampoBloqueado label="Código" value={form.codigo} styles={styles} />
      <CampoBloqueado label="Nombre" value={form.nombre} styles={styles} />
      <CampoBloqueado label="Ciudad" value={form.ciudad} styles={styles} />
      <CampoBloqueado label="País" value={form.pais} styles={styles} />
      <CampoBloqueado label="Latitud" value={form.lat} styles={styles} />
      <CampoBloqueado label="Longitud" value={form.lng} styles={styles} />
      <label>
        Capacidad
        <input
          type="text"
          inputMode="numeric"
          value={form.capacidadMax}
          onChange={(e) => setForm({ ...form, capacidadMax: e.target.value })}
        />
      </label>
      <CampoBloqueado label="Continente" value={form.continente} styles={styles} />
    </div>
  )
}

function CamposUT({ form, setForm, styles }) {
  return (
    <div className={styles.formGrid}>
      <CampoBloqueado label="Código" value={form.codigo} styles={styles} />
      <CampoBloqueado label="Tipo" value={form.tipo} styles={styles} />
      <CampoBloqueado label="Ubicación/base" value={form.ubicacionActual} styles={styles} />
      <label>
        Capacidad
        <input
          type="text"
          inputMode="numeric"
          value={form.capacidadMax}
          onChange={(e) => setForm({ ...form, capacidadMax: e.target.value })}
        />
      </label>
      <CampoBloqueado label="Estado" value={form.estado} styles={styles} />
    </div>
  )
}

function CamposTramo({ form, setForm, styles }) {
  return (
    <div className={styles.formGrid}>
      <CampoBloqueado label="UT asignada" value={form.utAsignada} styles={styles} />
      <label>
        Origen (ICAO)
        <input
          type="text"
          maxLength={4}
          value={form.origen}
          placeholder="ej: SKBO"
          onChange={(e) => setForm({ ...form, origen: e.target.value.toUpperCase() })}
        />
      </label>
      <label>
        Destino (ICAO)
        <input
          type="text"
          maxLength={4}
          value={form.destino}
          placeholder="ej: EHAM"
          onChange={(e) => setForm({ ...form, destino: e.target.value.toUpperCase() })}
        />
      </label>
      <label>
        Salida
        <input type="datetime-local" value={form.horaSalida} onChange={(e) => setForm({ ...form, horaSalida: e.target.value })} />
      </label>
      <label>
        Llegada
        <input type="datetime-local" value={form.horaLlegada} onChange={(e) => setForm({ ...form, horaLlegada: e.target.value })} />
      </label>
      <CampoBloqueado label="Estado" value={form.estado} styles={styles} />
    </div>
  )
}

function AccionesModal({ errores, styles }) {
  return (
    <>
      {errores.modal && <p className={styles.error}>{errores.modal}</p>}
      <div className={styles.modalActions}>
        <button type="submit" className={styles.btnPrimario}>Guardar cambios</button>
      </div>
    </>
  )
}

function normalizarBusqueda(valor) {
  return String(valor ?? '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

function filtrarItems(items, busqueda, campos) {
  const termino = normalizarBusqueda(busqueda).trim()
  if (!termino) return items
  return items.filter((item) => campos(item).some((campo) => normalizarBusqueda(campo).includes(termino)))
}

function BuscadorMantenimiento({ valor, onChange, placeholder, styles }) {
  return (
    <label className={styles.buscadorMantenimiento}>
      Buscar
      <input
        type="search"
        value={valor}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  )
}

function ListaPaginada({ items, pagina, onPagina, render, styles }) {
  const totalPaginas = Math.max(1, Math.ceil(items.length / REGISTROS_POR_PAGINA))
  const paginaActual = Math.min(Math.max(1, pagina), totalPaginas)
  const inicio = (paginaActual - 1) * REGISTROS_POR_PAGINA
  const visibles = items.slice(inicio, inicio + REGISTROS_POR_PAGINA)

  return (
    <>
      <div className={styles.listaMantenimiento}>
        {items.length === 0 ? (
          <p className={styles.vacio}>No hay registros.</p>
        ) : visibles.map((item, index) => (
          <div key={item.id || item.codigo || `${item.origen}-${item.destino}-${inicio + index}`} className={styles.itemMantenimiento}>
            {render(item)}
          </div>
        ))}
      </div>
      {items.length > 0 && (
        <div className={styles.paginacion}>
          <button type="button" onClick={() => onPagina(paginaActual - 1)} disabled={paginaActual === 1}>Anterior</button>
          <span>Página {paginaActual} de {totalPaginas}</span>
          <button type="button" onClick={() => onPagina(paginaActual + 1)} disabled={paginaActual === totalPaginas}>Siguiente</button>
        </div>
      )}
    </>
  )
}

export default PanelConfiguracionMapa
