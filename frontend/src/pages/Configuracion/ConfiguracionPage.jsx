// Menú de configuración del sistema.
// Sub-páginas: aeropuertos, vuelos y rangos del semáforo.

import { useState } from 'react'
import {
  Save,
  RotateCcw,
  Settings,
  Clock3,
  Warehouse,
  Plane,
  MapPinned,
  SlidersHorizontal,
  ShieldCheck,
  BrainCircuit,
  Database,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react'

import styles from './ConfiguracionPage.module.css'

function SectionCard({ icono, titulo, subtitulo, children }) {
  return (
    <section className={styles.sectionCard}>
      <div className={styles.sectionHeader}>
        <div className={styles.sectionIcon}>{icono}</div>
        <div>
          <h2 className={styles.sectionTitle}>{titulo}</h2>
          <p className={styles.sectionSubtitle}>{subtitulo}</p>
        </div>
      </div>
      <div className={styles.sectionBody}>{children}</div>
    </section>
  )
}

function Campo({ label, hint, children }) {
  return (
    <div className={styles.campo}>
      <label className={styles.label}>{label}</label>
      {children}
      {hint ? <span className={styles.hint}>{hint}</span> : null}
    </div>
  )
}

function KpiMini({ titulo, valor, estado = 'neutral' }) {
  return (
    <article className={`${styles.kpiMini} ${styles[`kpiMini--${estado}`]}`}>
      <span className={styles.kpiMiniTitulo}>{titulo}</span>
      <strong className={styles.kpiMiniValor}>{valor}</strong>
    </article>
  )
}

function ConfiguracionPage() {
  const [config, setConfig] = useState({
    plazoMismoContinente: 1,
    plazoIntercontinental: 2,
    trasladoMismoContinente: 12,
    trasladoIntercontinental: 24,

    capacidadVueloMismoMin: 150,
    capacidadVueloMismoMax: 250,
    capacidadVueloInterMin: 150,
    capacidadVueloInterMax: 400,

    capacidadAlmacenMin: 500,
    capacidadAlmacenMax: 800,
    capacidadGlobal: 12000,

    frecuenciaMismoContinente: 2,
    frecuenciaIntercontinental: 1,

    aeropuertoDefault: 'Lima',
    zonaHoraria: 'UTC',
    escenarioActivo: 'Tiempo real',

    semaforoVerdeMax: 60,
    semaforoAmbarMax: 85,
    alertaPreventiva: 90,

    algoritmoActivo: 'Algoritmo A',
    validacionDatos: true,
    cambiosSinReinicio: true,
    cargaMasiva: true,
  })

  const actualizar = (campo, valor) => {
    setConfig((prev) => ({
      ...prev,
      [campo]: valor,
    }))
  }

  const onCheckbox = (campo) => {
    setConfig((prev) => ({
      ...prev,
      [campo]: !prev[campo],
    }))
  }

  const onSubmit = (e) => {
    e.preventDefault()
    alert('Configuración guardada correctamente')
  }

  const onReset = () => {
    alert('Configuración restablecida a valores iniciales')
  }

  return (
    <div className={styles.page}>
      <div className={styles.layout}>
        <div className={styles.mainColumn}>
          <section className={styles.header}>
            <div className={styles.headerTextos}>
              <h1 className={styles.titulo}>Configuración General</h1>
              <p className={styles.subtitulo}>
                Administra parámetros operativos, capacidades, semáforos y comportamiento general del sistema Tasf.B2B.
              </p>
            </div>

            <div className={styles.headerAcciones}>
              <button className={styles.botonSecundario} type="button" onClick={onReset}>
                <RotateCcw size={16} />
                <span>Restablecer</span>
              </button>

              <button className={styles.botonPrimario} type="submit" form="form-configuracion">
                <Save size={16} />
                <span>Guardar cambios</span>
              </button>
            </div>
          </section>

          <section className={styles.kpisGrid}>
            <KpiMini titulo="Escenario activo" valor={config.escenarioActivo} estado="azul" />
            <KpiMini titulo="Algoritmo" valor={config.algoritmoActivo} />
            <KpiMini titulo="Zona horaria" valor={config.zonaHoraria} />
            <KpiMini titulo="Alerta preventiva" valor={`${config.alertaPreventiva}%`} estado="oscuro" />
          </section>

          <form id="form-configuracion" className={styles.formGrid} onSubmit={onSubmit}>
            <SectionCard
              icono={<Clock3 size={18} />}
              titulo="Tiempos y plazos"
              subtitulo="Define plazos máximos de entrega y tiempos base de traslado."
            >
              <div className={styles.grid2}>
                <Campo label="Plazo máximo mismo continente (días)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.plazoMismoContinente}
                    onChange={(e) => actualizar('plazoMismoContinente', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Plazo máximo intercontinental (días)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.plazoIntercontinental}
                    onChange={(e) => actualizar('plazoIntercontinental', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Traslado base mismo continente (horas)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.trasladoMismoContinente}
                    onChange={(e) => actualizar('trasladoMismoContinente', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Traslado base intercontinental (horas)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.trasladoIntercontinental}
                    onChange={(e) => actualizar('trasladoIntercontinental', Number(e.target.value))}
                  />
                </Campo>
              </div>
            </SectionCard>

            <SectionCard
              icono={<Plane size={18} />}
              titulo="Capacidades de vuelo"
              subtitulo="Configura límites mínimos y máximos de transporte por tipo de ruta."
            >
              <div className={styles.grid2}>
                <Campo label="Vuelo mismo continente - mínimo">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.capacidadVueloMismoMin}
                    onChange={(e) => actualizar('capacidadVueloMismoMin', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Vuelo mismo continente - máximo">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.capacidadVueloMismoMax}
                    onChange={(e) => actualizar('capacidadVueloMismoMax', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Vuelo intercontinental - mínimo">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.capacidadVueloInterMin}
                    onChange={(e) => actualizar('capacidadVueloInterMin', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Vuelo intercontinental - máximo">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.capacidadVueloInterMax}
                    onChange={(e) => actualizar('capacidadVueloInterMax', Number(e.target.value))}
                  />
                </Campo>
              </div>
            </SectionCard>

            <SectionCard
              icono={<Warehouse size={18} />}
              titulo="Capacidad de almacenes"
              subtitulo="Controla límites por aeropuerto y capacidad global del sistema."
            >
              <div className={styles.grid2}>
                <Campo label="Almacén por aeropuerto - mínimo">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.capacidadAlmacenMin}
                    onChange={(e) => actualizar('capacidadAlmacenMin', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Almacén por aeropuerto - máximo">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.capacidadAlmacenMax}
                    onChange={(e) => actualizar('capacidadAlmacenMax', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Capacidad global del sistema">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.capacidadGlobal}
                    onChange={(e) => actualizar('capacidadGlobal', Number(e.target.value))}
                  />
                </Campo>
              </div>
            </SectionCard>

            <SectionCard
              icono={<MapPinned size={18} />}
              titulo="Red operativa"
              subtitulo="Configura aeropuertos base, frecuencia de vuelos y zona horaria."
            >
              <div className={styles.grid2}>
                <Campo label="Aeropuerto principal por defecto">
                  <select
                    className={styles.select}
                    value={config.aeropuertoDefault}
                    onChange={(e) => actualizar('aeropuertoDefault', e.target.value)}
                  >
                    <option>Lima</option>
                    <option>Madrid</option>
                    <option>Tokio</option>
                    <option>Nueva York</option>
                    <option>Sao Paulo</option>
                  </select>
                </Campo>

                <Campo label="Zona horaria estándar">
                  <select
                    className={styles.select}
                    value={config.zonaHoraria}
                    onChange={(e) => actualizar('zonaHoraria', e.target.value)}
                  >
                    <option>UTC</option>
                    <option>GMT-5</option>
                    <option>GMT+1</option>
                    <option>GMT+9</option>
                  </select>
                </Campo>

                <Campo label="Frecuencia rutas mismo continente (vuelos/día)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.frecuenciaMismoContinente}
                    onChange={(e) => actualizar('frecuenciaMismoContinente', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Frecuencia rutas intercontinentales (vuelos/día)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.frecuenciaIntercontinental}
                    onChange={(e) => actualizar('frecuenciaIntercontinental', Number(e.target.value))}
                  />
                </Campo>
              </div>
            </SectionCard>

            <SectionCard
              icono={<SlidersHorizontal size={18} />}
              titulo="Semáforos y alertas"
              subtitulo="Define rangos visuales y disparadores preventivos de alerta."
            >
              <div className={styles.grid2}>
                <Campo label="Verde hasta (%)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.semaforoVerdeMax}
                    onChange={(e) => actualizar('semaforoVerdeMax', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Ámbar hasta (%)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.semaforoAmbarMax}
                    onChange={(e) => actualizar('semaforoAmbarMax', Number(e.target.value))}
                  />
                </Campo>

                <Campo label="Alerta preventiva (%)">
                  <input
                    className={styles.input}
                    type="number"
                    value={config.alertaPreventiva}
                    onChange={(e) => actualizar('alertaPreventiva', Number(e.target.value))}
                  />
                </Campo>
              </div>

              <div className={styles.semaforoPreview}>
                <div className={styles.previewItem}>
                  <span className={`${styles.previewDot} ${styles.dotVerde}`} />
                  <span>Normal: 0% - {config.semaforoVerdeMax}%</span>
                </div>
                <div className={styles.previewItem}>
                  <span className={`${styles.previewDot} ${styles.dotAmbar}`} />
                  <span>Alerta: {config.semaforoVerdeMax + 1}% - {config.semaforoAmbarMax}%</span>
                </div>
                <div className={styles.previewItem}>
                  <span className={`${styles.previewDot} ${styles.dotRojo}`} />
                  <span>Crítico: {config.semaforoAmbarMax + 1}% - 100%</span>
                </div>
              </div>
            </SectionCard>

            <SectionCard
              icono={<BrainCircuit size={18} />}
              titulo="Planificador y simulación"
              subtitulo="Controla algoritmo activo y escenario operativo de ejecución."
            >
              <div className={styles.grid2}>
                <Campo label="Algoritmo activo">
                  <select
                    className={styles.select}
                    value={config.algoritmoActivo}
                    onChange={(e) => actualizar('algoritmoActivo', e.target.value)}
                  >
                    <option>Algoritmo A</option>
                    <option>Algoritmo B</option>
                  </select>
                </Campo>

                <Campo label="Escenario operativo">
                  <select
                    className={styles.select}
                    value={config.escenarioActivo}
                    onChange={(e) => actualizar('escenarioActivo', e.target.value)}
                  >
                    <option>Tiempo real</option>
                    <option>Simulación por periodo</option>
                    <option>Simulación hasta colapso</option>
                  </select>
                </Campo>
              </div>
            </SectionCard>

            <SectionCard
              icono={<ShieldCheck size={18} />}
              titulo="Validación y comportamiento del sistema"
              subtitulo="Habilita opciones de validación, carga y actualización dinámica."
            >
              <div className={styles.checkGrid}>
                <label className={styles.checkItem}>
                  <input
                    type="checkbox"
                    checked={config.validacionDatos}
                    onChange={() => onCheckbox('validacionDatos')}
                  />
                  <span>Validar datos antes del procesamiento</span>
                </label>

                <label className={styles.checkItem}>
                  <input
                    type="checkbox"
                    checked={config.cambiosSinReinicio}
                    onChange={() => onCheckbox('cambiosSinReinicio')}
                  />
                  <span>Permitir cambios sin reiniciar la aplicación</span>
                </label>

                <label className={styles.checkItem}>
                  <input
                    type="checkbox"
                    checked={config.cargaMasiva}
                    onChange={() => onCheckbox('cargaMasiva')}
                  />
                  <span>Habilitar carga masiva de datos</span>
                </label>
              </div>
            </SectionCard>
          </form>
        </div>

        <aside className={styles.sidePanel}>
          <div className={styles.sideHeader}>
            <div>
              <p className={styles.sideEyebrow}>Resumen de configuración</p>
              <h2 className={styles.sideTitle}>Estado actual</h2>
            </div>
            <div className={styles.sideIcon}>
              <Settings size={18} />
            </div>
          </div>

          <div className={styles.sideBody}>
            <div className={styles.estadoCard}>
              <div className={styles.estadoHeader}>
                <CheckCircle2 size={16} />
                <span>Configuración consistente</span>
              </div>
              <p>
                Los rangos y capacidades cargados son válidos para la operación actual.
              </p>
            </div>

            <div className={styles.resumenBloque}>
              <h3>Parámetros clave</h3>
              <ul className={styles.resumenLista}>
                <li>
                  <span>Plazo mismo continente</span>
                  <strong>{config.plazoMismoContinente} día</strong>
                </li>
                <li>
                  <span>Plazo intercontinental</span>
                  <strong>{config.plazoIntercontinental} días</strong>
                </li>
                <li>
                  <span>Vuelo intercontinental máx.</span>
                  <strong>{config.capacidadVueloInterMax} maletas</strong>
                </li>
                <li>
                  <span>Capacidad almacén máx.</span>
                  <strong>{config.capacidadAlmacenMax} maletas</strong>
                </li>
              </ul>
            </div>

            <div className={styles.resumenBloque}>
              <h3>Alertas</h3>
              <div className={styles.alertaItem}>
                <AlertTriangle size={15} />
                <span>Advertencia preventiva al {config.alertaPreventiva}%</span>
              </div>
            </div>

            <div className={styles.resumenBloque}>
              <h3>Persistencia</h3>
              <div className={styles.persistenciaRow}>
                <Database size={15} />
                <span>Cambios listos para guardar en parámetros del sistema</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}

export default ConfiguracionPage