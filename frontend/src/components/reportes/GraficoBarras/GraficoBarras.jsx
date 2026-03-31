import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import styles from './GraficoBarras.module.css'

/**
 * Gráfico de barras reutilizable.
 * @param {string}  titulo
 * @param {Array}   datos          - Array de objetos; cada uno tiene una clave "etiqueta" y las claves de las series
 * @param {Array}   series         - [{ clave, nombre, color }]
 * @param {number}  altura
 */
function GraficoBarras({ titulo, datos = [], series = [], altura = 300 }) {
  return (
    <div className={styles.wrapper}>
      {titulo && <h3 className={styles.titulo}>{titulo}</h3>}
      <ResponsiveContainer width="100%" height={altura}>
        <BarChart data={datos} margin={{ top: 8, right: 16, left: -10, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="etiqueta" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
          <YAxis tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
          <Tooltip
            contentStyle={{ fontSize: 12, borderRadius: 8, border: '1px solid #e2e8f0' }}
            cursor={{ fill: 'rgba(37, 99, 235, 0.05)' }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          {series.map(({ clave, nombre, color }) => (
            <Bar
              key={clave}
              dataKey={clave}
              name={nombre}
              fill={color}
              radius={[4, 4, 0, 0]}
              maxBarSize={48}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export default GraficoBarras
