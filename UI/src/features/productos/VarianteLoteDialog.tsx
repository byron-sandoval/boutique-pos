import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useSave } from '@/features/empresa/hooks'
import { coloresVariante } from './colores-variantes'
import { guardarLoteVariantes } from './data'
import type { ProductoReferencia } from './data'

type Fila = { id: number; talla: string | null; color: string | null; sku: string }

function valoresUnicos(valores: string[]) {
  const unicos = new Map<string, string>()
  for (const valor of valores) {
    const texto = valor.trim()
    const clave = texto.toLocaleLowerCase('es-NI')
    if (texto && !unicos.has(clave)) unicos.set(clave, texto)
  }
  return [...unicos.values()]
}

export function VarianteLoteDialog({ productos, productoInicial, onClose, onSaved }: {
  productos: ProductoReferencia[]
  productoInicial: string
  onClose: () => void
  onSaved: (productoId: number, cantidad: number) => void
}) {
  const [productoId, setProductoId] = useState(productos.some(producto => String(producto.id) === productoInicial) ? productoInicial : '')
  const [seleccionados, setSeleccionados] = useState<string[]>([])
  const [otrosColores, setOtrosColores] = useState('')
  const [tallas, setTallas] = useState('')
  const [filas, setFilas] = useState<Fila[] | null>(null)
  const [validacion, setValidacion] = useState('')
  const { busy, error, save, clearError } = useSave()
  const producto = productos.find(item => String(item.id) === productoId)

  function prepararLote() {
    const coloresLote = valoresUnicos([...seleccionados, ...otrosColores.split(',')])
    const tallasLote = valoresUnicos(tallas.split(','))
    if (!producto || (!coloresLote.length && !tallasLote.length)) {
      setValidacion('Selecciona un producto y agrega al menos un color o una talla.')
      return
    }
    if (coloresLote.some(color => color.length > 50) || tallasLote.some(talla => talla.length > 20)) {
      setValidacion('Los colores admiten hasta 50 caracteres y las tallas hasta 20.')
      return
    }
    const listaTallas = tallasLote.length ? tallasLote : [null]
    const listaColores = coloresLote.length ? coloresLote : [null]
    if (listaTallas.length * listaColores.length > 100) {
      setValidacion('Puedes registrar hasta 100 combinaciones por lote.')
      return
    }
    const combinaciones = listaTallas.flatMap(talla => listaColores.map(color => {
      const sufijo = [talla, color].filter(Boolean).join('-').toLocaleUpperCase('es-NI').replace(/\s+/g, '-')
      const sku = `${producto.codigo}-${sufijo}`
      return { talla, color, sku: sku.length <= 50 ? sku : '' }
    }))
    setFilas(combinaciones.map((fila, id) => ({ ...fila, id })))
    setValidacion('')
  }

  return <RecordDialog title="Crear variantes en lote" busy={busy} onClose={onClose}
    description={filas ? `Revisa las combinaciones de ${producto?.nombre} antes de guardar.` : 'Elige un producto y los colores o tallas que quieres registrar.'}>
    {filas === null ? <form className="business-form" onSubmit={event => {
      event.preventDefault()
      prepararLote()
    }}>
      <fieldset className="space-y-4">
        <div className="business-fields">
          <label className="business-field-wide">Producto
            <select value={productoId} onChange={event => setProductoId(event.currentTarget.value)} required>
              <option value="">Selecciona un producto</option>
              {productos.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}
            </select>
          </label>
        </div>
        <div className="space-y-2">
          <p id="lote-colores" className="text-xs font-medium">Colores</p>
          <div role="group" aria-labelledby="lote-colores" className="flex flex-wrap gap-2">
            {coloresVariante.map(([nombre, muestra]) => {
              const marcado = seleccionados.includes(nombre)
              return <Button key={nombre} type="button" variant="outline" className="variante-color-opcion"
                aria-pressed={marcado} onClick={() => setSeleccionados(actual => marcado ? actual.filter(color => color !== nombre) : [...actual, nombre])}>
                <span className="variante-color-muestra" style={{ backgroundColor: muestra }} aria-hidden="true" />
                {nombre}
                <Check className="variante-color-check size-3.5" aria-hidden="true" />
              </Button>
            })}
          </div>
        </div>
        <div className="business-fields">
          <label>Otros colores <input value={otrosColores} onChange={event => setOtrosColores(event.currentTarget.value)} placeholder="Vino, crema" /></label>
          <label>Tallas <input value={tallas} onChange={event => setTallas(event.currentTarget.value)} placeholder="S, M, L" /></label>
        </div>
        <p className="business-help">Separa los valores con comas. Puedes dejar las tallas en blanco.</p>
      </fieldset>
      {validacion && <p className="notice error" role="alert">{validacion}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
        <Button type="submit">Revisar combinaciones</Button>
      </div>
    </form> : <form className="business-form" onSubmit={event => {
      event.preventDefault()
      if (!producto || !filas.length) return
      const form = new FormData(event.currentTarget)
      const variantes = filas.map(fila => ({
        talla: fila.talla, color: fila.color,
        sku: String(form.get(`sku-${fila.id}`) ?? '').trim(),
        codigoBarras: String(form.get(`barras-${fila.id}`) ?? '').trim() || null,
      }))
      void save(() => guardarLoteVariantes(producto.id, variantes), guardadas => onSaved(producto.id, guardadas.length))
    }}>
      <fieldset disabled={busy}>
        <legend>Combinaciones a registrar <span className="ml-1 text-muted-foreground">· {filas.length}</span></legend>
        {filas.length === 0 ? <p>Vuelve al paso anterior para generar combinaciones.</p> :
          <ul aria-label="Combinaciones del lote" className="divide-y divide-border">
            {filas.map(fila => {
              const etiqueta = [fila.color, fila.talla].filter(Boolean).join(' / ')
              return <li key={fila.id} className="grid gap-3 py-3 sm:grid-cols-[minmax(0,1fr)_repeat(2,minmax(0,1.4fr))] sm:items-end">
                <div className="flex min-w-0 items-center justify-between gap-2 sm:self-end">
                  <span className="text-sm font-semibold wrap-anywhere">{etiqueta}</span>
                  <Button type="button" variant="ghost" size="icon-sm" aria-label={`Quitar ${etiqueta}`}
                    onClick={() => setFilas(filas.filter(item => item.id !== fila.id))}><X size={14} aria-hidden="true" /></Button>
                </div>
                <label className="grid gap-1.5 text-xs font-medium">SKU
                  <input name={`sku-${fila.id}`} aria-label={`SKU de ${etiqueta}`} required pattern={String.raw`.*\S.*`} maxLength={50} defaultValue={fila.sku} />
                </label>
                <label className="grid gap-1.5 text-xs font-medium">Código de barras
                  <input name={`barras-${fila.id}`} aria-label={`Código de barras de ${etiqueta}`} maxLength={50} />
                </label>
              </li>
            })}
          </ul>}
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={() => {
          clearError()
          setFilas(null)
        }}>Volver</Button>
        <Button type="submit" disabled={busy || !filas.length}>{busy ? 'Guardando…' : 'Guardar lote'}</Button>
      </div>
    </form>}
  </RecordDialog>
}
