import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import styles from './GraficoLinea.module.css'

/**
 * Gráfico de línea reutilizable para evolución temporal.
 * @param {string}  titulo
 * @param {Array}   datos          - Array de objetos con clave "etiqueta" y las claves de series
 * @param {Array}   series         - [{ clave, nombre, color }]
 * @param {number}  altura
 */
function GraficoLinea({ titulo, datos = [], series = [], altura = 300 }) {
  return (
    <div className={styles.wrapper}>
      {titulo && <h3 className={styles.titulo}>{titulo}</h3>}
      <ResponsiveContainer width="100%" height={altura}>
        <LineChart data={datos} margin={{ top: 8, right: 16, left: -10, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="etiqueta" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
          <YAxis tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
          <Tooltip
            contentStyle={{ fontSize: 12, borderRadius: 8, border: '1px solid #e2e8f0' }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          {series.map(({ clave, nombre, color }) => (
            <Line
              key={clave}
              type="monotone"
              dataKey={clave}
              name={nombre}
              stroke={color}
              strokeWidth={2}
              dot={{ r: 4, fill: color }}
              activeDot={{ r: 6 }}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

export default GraficoLinea
