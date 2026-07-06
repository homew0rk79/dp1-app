/**
 * Utilidades de filtrado por texto/patrón para los paneles del visualizador.
 */

/**
 * Verifica si un texto coincide con una consulta que puede incluir el comodín `*`.
 * Sin comodín se comporta como "contiene" (case-insensitive).
 * Con comodín: `SK*` → empieza con SK; `*IM` → termina en IM; `S*M` → patrón.
 *
 * @param {string} texto   texto a evaluar
 * @param {string} query   consulta del usuario
 * @returns {boolean}
 */
export function matchPatron(texto, query) {
  if (!query) return true
  if (!texto) return false
  const t = texto.toLowerCase()
  const q = query.trim().toLowerCase()
  if (!q.includes('*')) return t.includes(q)

  // Convertir el patrón con * a regex, escapando el resto de caracteres
  const escapado = q.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*')
  try {
    return new RegExp(`^${escapado}$`).test(t)
  } catch {
    return t.includes(q.replace(/\*/g, ''))
  }
}

/**
 * Coincidencia de un patrón contra varios campos: retorna true si al menos
 * uno coincide.
 */
export function matchPatronCampos(query, ...campos) {
  if (!query) return true
  return campos.some((c) => matchPatron(c ?? '', query))
}
