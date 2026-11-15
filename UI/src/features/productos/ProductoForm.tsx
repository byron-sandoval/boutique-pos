import { useEffect, useRef, useState } from 'react'
import { CloudUpload, FolderOpen, Save, Trash2 } from 'lucide-react'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { useSave } from '@/features/empresa/hooks'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { guardarProducto } from './data'
import type { Producto, Referencia, ReferenciasProducto } from './data'
import './producto-form.css'

const formatos = ['image/png', 'image/jpeg', 'image/webp']
const maxImagenBytes = 2 * 1024 * 1024

function texto(form: FormData, campo: string) {
  return String(form.get(campo) ?? '').trim()
}

function importe(form: FormData, campo: string, etiqueta: string) {
  const valor = texto(form, campo)
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(valor)) {
    throw new Error(`${etiqueta} debe ser un importe válido de hasta dos decimales.`)
  }
  return Number(valor)
}

function opciones(referencias: Referencia[]) {
  return referencias.map(referencia => <option key={referencia.id} value={referencia.id}>
    {referencia.nombre}{!referencia.activo ? ' (inactiva)' : ''}
  </option>)
}

export function ProductoForm({ producto, referencias, onClose, onSaved }: {
  producto: Producto | null
  referencias: ReferenciasProducto
  onClose: () => void
  onSaved: (producto: Producto) => void
}) {
  const { busy, error, save } = useSave()
  const entradaImagen = useRef<HTMLInputElement>(null)
  const [seleccion, setSeleccion] = useState<{ archivo: File; url: string } | null>(null)
  const [quitarFotoActual, setQuitarFotoActual] = useState(false)
  const [errorImagen, setErrorImagen] = useState('')
  const vistaPrevia = seleccion?.url ?? (quitarFotoActual ? null : producto?.imagenUrl)
  const etiquetaQuitarFoto = seleccion ? 'Quitar foto seleccionada' : 'Quitar foto actual'
  const categoriasDisponibles = referencias.categorias.filter(categoria => categoria.activo || categoria.id === producto?.categoriaId)
  const unidadesDisponibles = referencias.unidades.filter(unidad => unidad.activo || unidad.id === producto?.unidadMedidaId)

  useEffect(() => {
    if (seleccion) return () => URL.revokeObjectURL(seleccion.url)
  }, [seleccion])

  function seleccionarImagen(archivos: FileList | null) {
    if (!archivos?.length) return
    const archivo = archivos[0]
    if (archivos.length !== 1 || !formatos.includes(archivo.type) || archivo.size === 0 || archivo.size > maxImagenBytes) {
      setSeleccion(null)
      setErrorImagen('Elige una sola imagen PNG, JPG o WebP de hasta 2 MB.')
      return
    }
    setErrorImagen('')
    setSeleccion({ archivo, url: URL.createObjectURL(archivo) })
  }

  function quitarSeleccion() {
    setSeleccion(null)
    setErrorImagen('')
  }

  async function guardar(form: FormData) {
    const codigo = texto(form, 'codigo')
    const nombre = texto(form, 'nombre')
    if (!codigo || !nombre) throw new Error('Escribe el código y el nombre del producto.')
    const categoriaId = Number(form.get('categoriaId'))
    const unidadMedidaId = Number(form.get('unidadMedidaId'))
    if (!categoriasDisponibles.some(categoria => categoria.id === categoriaId)
      || !unidadesDisponibles.some(unidad => unidad.id === unidadMedidaId)) {
      throw new Error('Selecciona una categoría y una unidad de medida válidas.')
    }
    const imagen = seleccion?.archivo
    try {
      return await guardarProducto({
        categoriaId,
        unidadMedidaId,
        codigo,
        nombre,
        descripcion: texto(form, 'descripcion') || null,
        marca: texto(form, 'marca') || null,
        precio: importe(form, 'precio', 'El precio'),
        costo: importe(form, 'costo', 'El costo'),
        ultimoCosto: producto?.ultimoCosto ?? null,
        activo: form.get('activo') === 'on',
      }, imagen, producto?.id, quitarFotoActual)
    } catch (error) {
      if (imagen && error instanceof ApiError && error.status === 503) {
        throw new Error('No se pudo subir la foto a Supabase. Intenta nuevamente.', { cause: error })
      }
      if (imagen && error instanceof ApiError && error.status === 413) {
        throw new Error('La foto debe pesar como máximo 2 MB.', { cause: error })
      }
      throw error
    }
  }

  return <RecordDialog title={producto ? 'Editar producto' : 'Nuevo producto'}
    description="Completa los datos del catálogo. El precio se registra en córdobas."
    className="product-form-dialog" busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      if (!errorImagen) void save(() => guardar(form), onSaved)
    }}>
      <fieldset disabled={busy}>
        <legend>Datos del producto</legend>
        <div className="business-fields">
          <label>Código<input name="codigo" required maxLength={50} defaultValue={producto?.codigo} autoFocus /></label>
          <label>Nombre<input name="nombre" required maxLength={100} defaultValue={producto?.nombre} /></label>
          <label>Categoría
            <select name="categoriaId" required defaultValue={producto?.categoriaId ?? ''}>
              <option value="" disabled>Selecciona una categoría</option>
              {opciones(categoriasDisponibles)}
            </select>
          </label>
          <label>Unidad de medida
            <select name="unidadMedidaId" required defaultValue={producto?.unidadMedidaId ?? ''}>
              <option value="" disabled>Selecciona una unidad</option>
              {opciones(unidadesDisponibles.map(unidad => ({
                ...unidad, nombre: unidad.simbolo ? `${unidad.nombre} (${unidad.simbolo})` : unidad.nombre,
              })))}
            </select>
          </label>
          <label>Marca<input name="marca" maxLength={100} defaultValue={producto?.marca ?? ''} /></label>
          <label className="business-checkbox"><input name="activo" type="checkbox" defaultChecked={producto?.activo ?? true} />Producto activo</label>
          <label className="business-field-wide">Descripción<textarea name="descripcion" maxLength={255} rows={2} defaultValue={producto?.descripcion ?? ''} /></label>
        </div>
      </fieldset>
      <fieldset disabled={busy}>
        <legend>Precios en córdobas</legend>
        <div className="business-fields">
          <label>Precio de venta<input name="precio" type="number" required min="0" max="9999999999.99" step="0.01" defaultValue={producto?.precio} /></label>
          <label>Costo<input name="costo" type="number" required min="0" max="9999999999.99" step="0.01" defaultValue={producto?.costo} /></label>
        </div>
      </fieldset>
      <fieldset disabled={busy}>
        <legend>Foto del producto</legend>
        <div className="product-upload">
          <div className="product-upload-zone" onDragOver={event => {
            if (!event.dataTransfer.types.includes('Files')) return
            event.preventDefault()
            event.dataTransfer.dropEffect = busy ? 'none' : 'copy'
          }} onDrop={event => {
            event.preventDefault()
            if (!busy) seleccionarImagen(event.dataTransfer.files)
          }}>
            <CloudUpload className="product-upload-icon" size={23} aria-hidden="true" />
            <div className="product-upload-copy">
              <strong>Arrastra y suelta una imagen</strong>
              <span className="business-help">PNG, JPG o WebP · Máximo 2 MB</span>
              <input ref={entradaImagen} name="imagen" type="file" accept={formatos.join(',')} hidden
                onChange={event => {
                  seleccionarImagen(event.currentTarget.files)
                  event.currentTarget.value = ''
                }} />
              <Button type="button" size="sm" onClick={() => entradaImagen.current?.click()}>
                <FolderOpen size={14} aria-hidden="true" />{vistaPrevia ? 'Cambiar archivo' : 'Seleccionar archivo'}
              </Button>
            </div>
          </div>
          {vistaPrevia && <div className="product-upload-preview">
            <span>{seleccion ? 'Vista previa' : 'Foto actual'}</span>
            <div className="product-upload-thumbnail">
              <img src={vistaPrevia} alt="Foto del producto" />
              <Button type="button" variant="destructive" size="icon-xs"
                title={etiquetaQuitarFoto} aria-label={etiquetaQuitarFoto} onClick={() => {
                  if (!seleccion) setQuitarFotoActual(true)
                  quitarSeleccion()
                }}>
                <Trash2 size={13} aria-hidden="true" />
              </Button>
            </div>
          </div>}
        </div>
        {errorImagen && <div className="notice error mt-2" role="alert">
          <p>{errorImagen}</p>
          <Button type="button" variant="outline" size="sm" className="mt-2" onClick={quitarSeleccion}>Quitar selección</Button>
        </div>}
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy || Boolean(errorImagen)}><Save size={15} />{busy ? 'Guardando…' : 'Guardar producto'}</Button>
      </div>
    </form>
  </RecordDialog>
}
