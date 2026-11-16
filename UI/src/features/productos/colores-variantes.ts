export const coloresVariante = [
  ['Blanco', '#ffffff'], ['Negro', '#151518'], ['Gris', '#787983'], ['Beige', '#d1b69a'],
  ['Rojo', '#c83e48'], ['Rosa', '#d681a3'], ['Amarillo', '#e8b534'], ['Verde', '#428553'],
  ['Azul', '#3874bd'], ['Morado', '#8253a9'], ['Café', '#865c47'],
] as const

export function tonoVariante(color: string | null) {
  if (!color) return undefined
  return coloresVariante.find(([nombre]) => nombre.localeCompare(color.trim(), 'es-NI', { sensitivity: 'base' }) === 0)?.[1]
}
