import { useCallback, useState } from 'react'
import { Package, Pencil, Save, TriangleAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { RecordDialog } from '@/features/empresa/RecordDialog'
import { useRemote, useSave } from '@/features/empresa/hooks'
import { useFlashMessage } from '@/lib/use-flash-message'
import { PaginacionCatalogo } from '@/features/productos/PaginacionCatalogo'
import { guardarLimites, listarExistencias } from './data'
import type { Existencia } from './data'
import '@/features/empresa/empresa.css'

const maximoStock = 2_147_483_647

function etiquetaVariante(existencia: Existencia) {
  return [existencia.sku, existencia.talla, existencia.color].filter(Boolean).join(' · ') || `Variante #${existencia.varianteId}`
}

export function ExistenciasPage({ puedeEditar, soloAlertas = false }: { puedeEditar: boolean; soloAlertas?: boolean }) {
  const [pagina, setPagina] = useState(0)
  const [editando, setEditando] = useState<Existencia | null>(null)
  const [success, setSuccess] = useFlashMessage()
  const cargar = useCallback((signal: AbortSignal) => listarExistencias(pagina, signal, soloAlertas), [pagina, soloAlertas])
  const { result, reload } = useRemote(cargar)
  const titulo = soloAlertas ? 'Alertas de stock bajo' : 'Existencias'
  const Icono = soloAlertas ? TriangleAlert : Package

  return <div className="business-page">
    <div className="page-heading business-page-heading">
      <div><h1>{titulo}</h1><p>{soloAlertas
        ? 'Variantes con stock igual o inferior a su mínimo por sucursal.'
        : 'Consulta el stock por variante y sucursal.'}</p></div>
    </div>
    {success && <p className="business-success" role="status">{success}</p>}
    <Card className="business-table-card">
      <div className="business-table-toolbar">
        <div className="business-section-heading">
          <span className="business-symbol"><Icono size={20} aria-hidden="true" /></span>
          <h2>{soloAlertas ? 'Existencias para revisar' : 'Inventario'}</h2>
        </div>
      </div>
      {result.status === 'loading' && <p role="status">{soloAlertas ? 'Cargando alertas…' : 'Cargando existencias…'}</p>}
      {result.status === 'error' && <div>
        <p className="notice error" role="alert">{result.message}</p>
        <Button variant="outline" onClick={reload}>Reintentar</Button>
      </div>}
      {result.status === 'ready' && <>
        {result.data.content.length ? <div className="business-table-scroll" role="region" aria-label={titulo} tabIndex={0}>
          <table className="business-table">
            <thead><tr>
              <th scope="col">Producto</th><th scope="col">Variante</th><th scope="col">Sucursal</th><th scope="col">Stock actual</th>
              <th scope="col">Stock mínimo</th>
              {soloAlertas && <th scope="col">Estado</th>}
              {puedeEditar && <th scope="col">Acciones</th>}
            </tr></thead>
            <tbody>{result.data.content.map(existencia => <tr key={existencia.id}>
              <th scope="row">{existencia.producto || 'Producto no disponible'}</th>
              <td>{etiquetaVariante(existencia)}</td>
              <td>{existencia.sucursal || `Sucursal #${existencia.sucursalId}`}</td>
              <td className="tabular-nums font-semibold">{existencia.stock}</td>
              <td className="tabular-nums">{existencia.stockMinimo}</td>
              {soloAlertas && <td><span className="business-status is-inactive">
                {existencia.stock === 0 ? 'Agotado' : existencia.stock === existencia.stockMinimo ? 'En el mínimo' : 'Stock bajo'}
              </span></td>}
              {puedeEditar && <td><div className="business-table-actions">
                <Button variant="outline" size="icon-sm" data-table-action="edit" title="Editar límites de stock"
                  aria-label={`Editar límites de ${etiquetaVariante(existencia)} en ${existencia.sucursal || `sucursal #${existencia.sucursalId}`}`}
                  onClick={() => { setSuccess(''); setEditando(existencia) }}>
                  <Pencil size={14} aria-hidden="true" />
                </Button>
              </div></td>}
            </tr>)}</tbody>
          </table>
        </div> : <p>{soloAlertas ? 'No hay alertas de stock para mostrar.' : 'No hay existencias registradas en esta página.'}</p>}
        <PaginacionCatalogo total={result.data.page.totalElements} pagina={pagina} totalPaginas={result.data.page.totalPages}
          registros={soloAlertas ? 'alertas' : 'registros'} onChange={setPagina} />
      </>}
    </Card>
    {puedeEditar && editando && <LimitesDialog existencia={editando} onClose={() => setEditando(null)} onSaved={() => {
      setEditando(null)
      setSuccess('Límites de stock actualizados.')
      reload()
    }} />}
  </div>
}

function LimitesDialog({ existencia, onClose, onSaved }: { existencia: Existencia; onClose: () => void; onSaved: () => void }) {
  const { busy, error, save } = useSave()
  const descripcion = [existencia.producto, etiquetaVariante(existencia), existencia.sucursal || `Sucursal #${existencia.sucursalId}`]
    .filter(Boolean).join(' · ')

  return <RecordDialog title="Editar límites de stock" description={descripcion} busy={busy} onClose={onClose}>
    <form className="business-form" onSubmit={event => {
      event.preventDefault()
      const form = new FormData(event.currentTarget)
      const stockMinimo = Number(form.get('stockMinimo'))
      const stockMaximo = existencia.stockMaximo
      void save(async () => {
        if (stockMaximo !== null && stockMaximo < stockMinimo) {
          throw new Error(`El stock mínimo no puede superar el máximo guardado de ${stockMaximo}.`)
        }
        return guardarLimites(existencia.id, { stockMinimo, stockMaximo })
      }, onSaved)
    }}>
      <fieldset className="business-fields" disabled={busy}>
        <label>Stock actual<input type="text" readOnly value={existencia.stock} className="tabular-nums bg-muted!" /></label>
        <label>Stock mínimo<input name="stockMinimo" type="number" min={0} max={maximoStock} step={1}
          required defaultValue={existencia.stockMinimo} /></label>
      </fieldset>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="business-form-actions">
        <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancelar</Button>
        <Button type="submit" disabled={busy}><Save size={16} aria-hidden="true" />{busy ? 'Guardando…' : 'Guardar límites'}</Button>
      </div>
    </form>
  </RecordDialog>
}
