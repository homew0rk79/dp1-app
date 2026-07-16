import { useState } from 'react'

function formatearEstado(estado) {
  return String(estado || '-').replace(/_/g, ' ').toUpperCase()
}

function estadoEfectivo(tramo, tiempoActualAbs) {
  if (tiempoActualAbs == null || tramo.salidaAbs == null) return tramo.estado
  if (tramo.salidaAbs > tiempoActualAbs) return 'planificado'
  if (tramo.llegadaAbs != null && tramo.llegadaAbs > tiempoActualAbs) return 'en_transito'
  return 'entregado'
}

function estadoRutaEfectivo(ruta, tiempoActualAbs) {
  if (!ruta?.tramos?.length || tiempoActualAbs == null) return ruta?.estado
  const primerSalida = ruta.tramos[0]?.salidaAbs
  const ultimaLlegada = ruta.tramos[ruta.tramos.length - 1]?.llegadaAbs
  if (primerSalida != null && primerSalida > tiempoActualAbs) return 'planificado'
  if (ultimaLlegada != null && ultimaLlegada <= tiempoActualAbs) return 'entregado'
  return 'en_transito'
}

function obtenerNumeroMaleta(idMaleta) {
  const partes = String(idMaleta || '').split('-')
  return partes.length > 0 ? partes[partes.length - 1] : '-'
}

function TramosRuta({ titulo, ruta, mensajeVacio, styles, etiquetaMaletas = 'Maletas', tiempoActualAbs }) {
  if (!ruta) return <p className={styles.rutaMensajeVacio}>{mensajeVacio}</p>

  return (
    <section className={styles.rutaResultadoBloque}>
      <div className={styles.rutaResultadoHeader}>
        <h4>{titulo}</h4>
        <span>{ruta.origen} - {ruta.destino}</span>
      </div>
      <div className={styles.rutaResumenGrid}>
        <span>Estado</span>
        <strong>{formatearEstado(estadoRutaEfectivo(ruta, tiempoActualAbs))}</strong>
        <span>Escalas</span>
        <strong>{Math.max(0, (ruta.escalas?.length || 0) - 2)}</strong>
        <span>{etiquetaMaletas}</span>
        <strong>{ruta.cantidadMaletas ?? '-'}</strong>
      </div>

      <div className={styles.rutaEscalas}>
        {(ruta.escalas || []).map((codigo, index) => (
          <span key={`${codigo}-${index}`}>{codigo}</span>
        ))}
      </div>

      <div className={styles.rutaTabla}>
        <div className={styles.rutaTablaHeader}>
          <span>Tramo</span>
          <span>Salida</span>
          <span>Llegada</span>
          <span>Estado</span>
        </div>
        {(ruta.tramos || []).map((tramo) => (
          <div className={styles.rutaTablaFila} key={tramo.id ?? `${tramo.origen}-${tramo.destino}-${tramo.orden}`}>
            <span>{tramo.origen} - {tramo.destino}</span>
            <span>{tramo.salida || '-'}</span>
            <span>{tramo.llegada || '-'}</span>
            <span className={styles.rutaEstadoBadge} title={formatearEstado(estadoEfectivo(tramo, tiempoActualAbs))}>
              {formatearEstado(estadoEfectivo(tramo, tiempoActualAbs))}
            </span>
          </div>
        ))}
      </div>
    </section>
  )
}

function esIdRutaMock(id) {
  return /^RUT-/i.test(String(id || '').trim())
}

function ResumenEnvio({ resultado, styles }) {
  return (
    <div className={styles.rutaResumenGrid}>
      <span>Origen</span>
      <strong>{resultado.origen || '-'}</strong>
      <span>ID de envío</span>
      <strong>{resultado.idEnvio || '-'}</strong>
      <span>Destino</span>
      <strong>{resultado.destino || '-'}</strong>
      <span>Cliente</span>
      <strong>{resultado.idCliente || '-'}</strong>
      <span>Cantidad de maletas</span>
      <strong>{resultado.cantidadMaletas ?? '-'}</strong>
    </div>
  )
}

function ResumenMaleta({ resultado, styles }) {
  return (
    <>
      <div className={styles.rutaResumenGrid}>
        <span>ID de maleta consultada</span>
        <strong>{resultado.idMaleta || resultado.idBuscado}</strong>
        <span>N.º de maleta</span>
        <strong>{obtenerNumeroMaleta(resultado.idMaleta)}</strong>
        <span>Pertenece al envío</span>
        <strong>{resultado.idEnvio || '-'}</strong>
        <span>Origen del envío</span>
        <strong>{resultado.origen || '-'}</strong>
        <span>Destino del envío</span>
        <strong>{resultado.destino || '-'}</strong>
        <span>Cliente</span>
        <strong>{resultado.idCliente || '-'}</strong>
        <span>Total de maletas del envío</span>
        <strong>{resultado.cantidadMaletas ?? '-'}</strong>
      </div>
      <p className={styles.rutaNota}>
        Esta maleta hereda la ruta del envío al que pertenece.
      </p>
    </>
  )
}

function PanelBusquedaRuta({
  abierto,
  onCerrar,
  onBuscarMaleta,
  onBuscarEnvio,
  onLimpiar,
  resultado,
  cargando,
  error,
  idsPrueba,
  tiempoActualAbs,
  styles,
}) {
  const [tipoBusqueda, setTipoBusqueda] = useState('envio')
  const [origen, setOrigen] = useState('')
  const [idEnvio, setIdEnvio] = useState('')
  const [numeroMaleta, setNumeroMaleta] = useState('')
  const [validacion, setValidacion] = useState('')
  const [mostrarIds, setMostrarIds] = useState(false)

  if (!abierto) return null

  const origenes = idsPrueba?.origenes || []
  const ejemplos = idsPrueba?.ejemplos || []
  const ejemplo = ejemplos[0]
  const esMaletaResultado = resultado?.tipo === 'maleta'
  const mensajeAnterior = esMaletaResultado
    ? 'No existe ruta anterior registrada para esta maleta.'
    : 'No existen rutas anteriores registradas para este envío.'

  function limpiarFormulario() {
    setOrigen('')
    setIdEnvio('')
    setNumeroMaleta('')
    setValidacion('')
    onLimpiar()
  }

  function cambiarTipo(tipo) {
    setTipoBusqueda(tipo)
    limpiarFormulario()
  }

  function validarBase(id) {
    if (!origen) return 'Seleccione el aeropuerto origen.'
    if (!id) return 'Ingrese el ID del envío.'
    if (esIdRutaMock(id)) return 'RUT-001 corresponde a una ruta de prueba, no a un envío real.'
    if (!/^\d{8,9}$/.test(id)) return 'Formato inválido. Use un ID de envío como 000000001.'
    return ''
  }

  async function buscar(event) {
    event.preventDefault()
    const id = idEnvio.trim()
    const errorBase = validarBase(id)
    if (errorBase) {
      setValidacion(errorBase)
      return
    }
    if (tipoBusqueda === 'maleta') {
      const numero = numeroMaleta.trim()
      if (!numero) {
        setValidacion('Ingrese el número de maleta.')
        return
      }
      if (!/^\d{3}$/.test(numero)) {
        setValidacion('Formato inválido. Use un número de maleta como 001.')
        return
      }
      setValidacion('')
      await onBuscarMaleta({ origen, idEnvio: id, numeroMaleta: numero })
      return
    }

    setValidacion('')
    await onBuscarEnvio({ origen, idEnvio: id })
  }

  return (
    <aside className={styles.rutaPanel} aria-label="Buscar por ID">
      <header className={styles.configHeader}>
        <div>
          <span>Panel de rutas</span>
          <h3>Buscar por ID</h3>
        </div>
        <button type="button" onClick={onCerrar} aria-label="Cerrar búsqueda de rutas">x</button>
      </header>

      <div className={styles.rutaPanelContenido}>
        <form className={styles.rutaBuscadorUnico} onSubmit={buscar}>
          <div className={styles.rutaTipoSelector} role="radiogroup" aria-label="Tipo de búsqueda">
            <label>
              <input
                type="radio"
                name="tipoBusquedaRuta"
                value="maleta"
                checked={tipoBusqueda === 'maleta'}
                onChange={() => cambiarTipo('maleta')}
              />
              Maleta
            </label>
            <label>
              <input
                type="radio"
                name="tipoBusquedaRuta"
                value="envio"
                checked={tipoBusqueda === 'envio'}
                onChange={() => cambiarTipo('envio')}
              />
              Envío
            </label>
          </div>

          <label className={styles.rutaCampoId}>
            <span>Aeropuerto origen</span>
            <select value={origen} onChange={(event) => setOrigen(event.target.value)}>
              <option value="">Seleccionar</option>
              {origenes.map((codigo) => (
                <option key={codigo} value={codigo}>{codigo}</option>
              ))}
            </select>
          </label>

          <label className={styles.rutaCampoId}>
            <span>ID del envío</span>
            <input
              value={idEnvio}
              onChange={(event) => setIdEnvio(event.target.value)}
              placeholder={ejemplo?.idEnvio || '000000001'}
              aria-label="ID del envío"
            />
          </label>

          {tipoBusqueda === 'maleta' && (
            <>
              <p className={styles.rutaAyuda}>
                Las maletas se identifican dentro de un envío.
              </p>
              <label className={styles.rutaCampoId}>
                <span>N.º de maleta</span>
                <input
                  value={numeroMaleta}
                  onChange={(event) => setNumeroMaleta(event.target.value)}
                  placeholder="001"
                  aria-label="Número de maleta"
                />
              </label>
            </>
          )}

          <button type="submit" className={styles.btnPrimario} disabled={cargando}>
            Buscar
          </button>
        </form>

        <section className={styles.rutaIdsPrueba}>
          <button type="button" className={styles.btnSecundario} onClick={() => setMostrarIds((v) => !v)}>
            {mostrarIds ? 'Ocultar IDs reales de prueba' : 'Ver IDs reales de prueba'}
          </button>
          {mostrarIds && (
            <div className={styles.rutaIdsLista}>
              <p>{idsPrueba?.mensaje || 'IDs reales con rutas persistidas disponibles.'}</p>
              {ejemplos.length > 0 ? (
                <div>
                  <strong>Ejemplos reales</strong>
                  <div className={styles.rutaEscalas}>
                    {ejemplos.map((item) => (
                      <span key={item.idCompuesto}>{item.idCompuesto}</span>
                    ))}
                  </div>
                </div>
              ) : (
                <p>No hay IDs de envío con ruta persistida en la base de datos.</p>
              )}
            </div>
          )}
        </section>

        {validacion && <p className={styles.error}>{validacion}</p>}
        {error && <p className={styles.error}>{error}</p>}
        {cargando && <p className={styles.rutaEstado}>Consultando ruta...</p>}

        {resultado && (
          <section className={styles.rutaResultado}>
            <div className={styles.rutaResultadoTitulo}>
              <div>
                <span>Tipo de consulta: {esMaletaResultado ? 'Maleta' : 'Envío'}</span>
                <strong>{resultado.idBuscado}</strong>
              </div>
              <button type="button" className={styles.btnSecundario} onClick={limpiarFormulario}>
                Limpiar ruta
              </button>
            </div>

            {esMaletaResultado ? (
              <ResumenMaleta resultado={resultado} styles={styles} />
            ) : (
              <ResumenEnvio resultado={resultado} styles={styles} />
            )}

            <TramosRuta
              titulo={esMaletaResultado ? 'Ruta heredada del envío' : 'Ruta actual'}
              ruta={resultado.rutaActual}
              mensajeVacio="El envío existe, pero aún no tiene ruta registrada."
              styles={styles}
              etiquetaMaletas={esMaletaResultado ? 'Total de maletas del envío' : 'Maletas'}
              tiempoActualAbs={tiempoActualAbs}
            />
            <TramosRuta
              titulo="Ruta anterior"
              ruta={resultado.rutaAnterior}
              mensajeVacio={mensajeAnterior}
              styles={styles}
              etiquetaMaletas={esMaletaResultado ? 'Total de maletas del envío' : 'Maletas'}
              tiempoActualAbs={tiempoActualAbs}
            />
          </section>
        )}
      </div>
    </aside>
  )
}

export default PanelBusquedaRuta
