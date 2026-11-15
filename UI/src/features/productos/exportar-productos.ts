import escribirExcel from 'write-excel-file/browser'
import type { Producto, ReferenciasProducto } from './data'

export async function exportarProductos(productos: Producto[], referencias: ReferenciasProducto) {
  const categorias = new Map(referencias.categorias.map(categoria => [categoria.id, categoria.nombre]))
  const unidades = new Map(referencias.unidades.map(unidad => [unidad.id, unidad.simbolo || unidad.nombre]))
  const encabezados = ['Código', 'Producto', 'Marca', 'Categoría', 'Unidad', 'Precio (C$)', 'Estado']
    .map(value => ({ value, fontWeight: 'bold' as const }))
  const filas = productos.map(producto => [
    { value: producto.codigo, type: String },
    producto.nombre,
    producto.marca ?? '',
    categorias.get(producto.categoriaId) ?? String(producto.categoriaId),
    unidades.get(producto.unidadMedidaId) ?? String(producto.unidadMedidaId),
    { value: producto.precio, format: '#,##0.00' },
    producto.activo ? 'Activo' : 'Inactivo',
  ])
  await escribirExcel([encabezados, ...filas], {
    sheet: 'Productos',
    columns: [19, 32, 22, 24, 18, 16, 14].map(width => ({ width })),
    stickyRowsCount: 1,
  }).toFile('productos.xlsx')
}
