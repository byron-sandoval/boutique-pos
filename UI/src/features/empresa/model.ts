export type EmpresaRequest = {
  nombre: string
  ruc: string | null
  regimenFiscal: string | null
  direccion: string
  telefono: string | null
  correo: string | null
  logoUrl: string | null
  eslogan: string | null
  porcentajeIva: number
}
export type Empresa = EmpresaRequest & { id: number }
export type SucursalRequest = {
  empresaId: number
  nombre: string
  direccion: string
  telefono: string | null
  activa: boolean
}
export type Sucursal = SucursalRequest & { id: number }
export type Page<T> = { content: T[]; page: { number: number; totalPages: number; totalElements: number } }

function text(form: FormData, key: string, max: number, required = false) {
  const value = String(form.get(key) ?? '').trim()
  if (required && !value) throw new Error('Completa los campos obligatorios con un valor válido.')
  if (value.length > max) throw new Error(`El campo ${key} admite hasta ${max} caracteres.`)
  return value
}

export function empresaRequest(form: FormData): EmpresaRequest {
  const iva = String(form.get('porcentajeIva') ?? '').trim()
  if (!/^\d{1,3}(\.\d{1,2})?$/.test(iva) || Number(iva) > 100) {
    throw new Error('El IVA debe estar entre 0 y 100, con un máximo de dos decimales.')
  }
  const correo = text(form, 'correo', 100)
  if (correo && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) throw new Error('Escribe un correo válido.')
  return {
    nombre: text(form, 'nombre', 100, true), direccion: text(form, 'direccion', 255, true),
    ruc: text(form, 'ruc', 20) || null, regimenFiscal: text(form, 'regimenFiscal', 100) || null,
    telefono: text(form, 'telefono', 20) || null, correo: correo || null,
    logoUrl: text(form, 'logoUrl', 500) || null, eslogan: text(form, 'eslogan', 150) || null,
    porcentajeIva: Number(iva),
  }
}

export function sucursalRequest(form: FormData): SucursalRequest {
  const empresaId = Number(form.get('empresaId'))
  if (!Number.isSafeInteger(empresaId) || empresaId <= 0) throw new Error('Selecciona una empresa válida.')
  return {
    empresaId, nombre: text(form, 'nombre', 100, true), direccion: text(form, 'direccion', 255, true),
    telefono: text(form, 'telefono', 20) || null, activa: form.get('activa') === 'on',
  }
}
