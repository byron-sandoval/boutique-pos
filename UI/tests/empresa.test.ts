import { test } from 'node:test'
import assert from 'node:assert/strict'
import { empresaRequest, sucursalRequest } from '../src/features/empresa/model.ts'
import { createEmpresaService } from '../src/features/empresa/service.ts'
import { createApiClient } from '../src/api/client.ts'

function form(values: Record<string, string>) {
  const data = new FormData()
  Object.entries(values).forEach(([key, value]) => data.set(key, value))
  return data
}

test('rechaza nombres vacíos e IVA fuera de rango o con precisión que PostgreSQL no admite', () => {
  const valid = { nombre: 'David', direccion: 'Centro', porcentajeIva: '15.00' }
  for (const iva of ['', '-1', '100.01', '15.123', 'NaN']) {
    assert.throws(() => empresaRequest(form({ ...valid, porcentajeIva: iva })), /IVA/)
  }
  assert.throws(() => empresaRequest(form({ ...valid, nombre: '  ' })), /obligatorios/)
  assert.equal(empresaRequest(form({ ...valid, porcentajeIva: '0' })).porcentajeIva, 0)
  assert.equal(empresaRequest(form({ ...valid, porcentajeIva: '100.00' })).porcentajeIva, 100)
})

test('normaliza opcionales y envía solo campos admitidos, sin mezclar id ni datos de Keycloak', () => {
  const result = empresaRequest(form({ nombre: ' David ', direccion: ' Centro ', porcentajeIva: '15', ruc: '  ', id: '1', clientSecret: 'no-enviar' }))
  assert.equal(result.nombre, 'David')
  assert.equal(result.direccion, 'Centro')
  assert.equal(result.ruc, null)
  assert.equal(result.correo, null)
  assert.equal('id' in result, false)
  assert.equal('clientSecret' in result, false)
})

test('una sucursal requiere empresa válida y conserva explícitamente el estado inactivo', () => {
  const values = { empresaId: '42', nombre: 'Sucursal centro', direccion: 'Centro' }
  assert.equal(sucursalRequest(form(values)).activa, false)
  assert.equal(sucursalRequest(form({ ...values, activa: 'on' })).activa, true)
  assert.equal(sucursalRequest(form(values)).empresaId, 42)
  for (const empresaId of ['', '0', '-1', '1.5', 'abc']) {
    assert.throws(() => sucursalRequest(form({ ...values, empresaId })), /empresa válida/)
  }
})

test('recupera la única empresa sin asumir ID 1 y propaga la cancelación', async () => {
  const controller = new AbortController()
  const request = createApiClient({ baseUrl: 'http://localhost:8080/api', getToken: async () => 'fake-test-token', onUnauthorized: () => {}, fetcher: async (url, options) => {
    assert.equal(new URL(String(url)).searchParams.get('size'), '2')
    assert.equal(options?.signal, controller.signal)
    return Response.json({ content: [{ id: 42 }], page: { number: 0, totalPages: 1, totalElements: 1 } })
  } })
  const company = await createEmpresaService(request).empresa(controller.signal)
  assert.equal(company?.id, 42)
})

test('si aún no hay empresa devuelve null para permitir el registro inicial', async () => {
  const request = createApiClient({ baseUrl: 'http://localhost:8080/api', getToken: async () => 'fake-test-token', onUnauthorized: () => {}, fetcher: async () => Response.json({ content: [], page: { totalElements: 0 } }) })
  assert.equal(await createEmpresaService(request).empresa(), null)
})

test('si hay empresas duplicadas no elige una arbitrariamente ni mezcla sucursales', async () => {
  const request = createApiClient({ baseUrl: 'http://localhost:8080/api', getToken: async () => 'fake-test-token', onUnauthorized: () => {}, fetcher: async () => Response.json({ content: [{ id: 42 }, { id: 84 }], page: { totalElements: 2 } }) })
  await assert.rejects(createEmpresaService(request).empresa(), /más de una empresa/)
})

test('envía los datos y el logo juntos y deja que el navegador genere el límite multipart', async () => {
  const logo = new File(['logo'], 'logo.png', { type: 'image/png' })
  const company = empresaRequest(form({ nombre: 'David', direccion: 'Centro', porcentajeIva: '15' }))
  const request = createApiClient({ baseUrl: 'http://localhost:8080/api', getToken: async () => 'fake-test-token', onUnauthorized: () => {}, fetcher: async (url, options) => {
    assert.equal(new URL(String(url)).pathname, '/api/empresas/42')
    assert.equal(options?.method, 'PUT')
    assert.equal(new Headers(options.headers).has('Content-Type'), false)
    assert.ok(options.body instanceof FormData)
    const data = options.body.get('empresa')
    assert.ok(data instanceof Blob)
    assert.equal(data.type, 'application/json')
    assert.deepEqual(JSON.parse(await data.text()), company)
    assert.equal(options.body.get('logo'), logo)
    return Response.json({ id: 42 })
  } })
  await createEmpresaService(request).guardarEmpresa(company, 42, { logo })
})

test('creación, edición, filtro y estado usan los contratos REST del backend con IDs reales del registro', async () => {
  const calls: { url: URL; method: string; body: unknown }[] = []
  const request = createApiClient({ baseUrl: 'http://localhost:8080/api', getToken: async () => 'fake-test-token', onUnauthorized: () => {}, fetcher: async (url, options) => {
    const route = new URL(String(url))
    const method = options?.method ?? 'GET'
    calls.push({ url: route, method, body: options?.body ? JSON.parse(String(options.body)) : null })
    if (method === 'GET' && route.pathname === '/api/sucursales') {
      const page = Number(route.searchParams.get('page'))
      return Response.json({ content: [{ id: page === 0 ? 84 : 85 }], page: { totalPages: 2 } })
    }
    return Response.json({ id: 84 })
  } })
  const service = createEmpresaService(request)
  const company = empresaRequest(form({ nombre: 'David', direccion: 'Centro', porcentajeIva: '15' }))
  await service.guardarEmpresa(company)
  await service.guardarEmpresa(company, 42)
  const branch = sucursalRequest(form({ empresaId: '42', nombre: 'Centro', direccion: 'Centro', activa: 'on' }))
  await service.guardarSucursal(branch)
  await service.guardarSucursal(branch, 84)
  const branches = await service.sucursales(42)
  await service.estadoSucursal(84, false)
  assert.deepEqual(calls.map(call => [call.method, call.url.pathname]), [
    ['POST', '/api/empresas'], ['PUT', '/api/empresas/42'], ['POST', '/api/sucursales'], ['PUT', '/api/sucursales/84'], ['GET', '/api/sucursales'], ['GET', '/api/sucursales'], ['PATCH', '/api/sucursales/84/estado'],
  ])
  assert.equal(calls[4].url.searchParams.get('empresaId'), '42')
  assert.equal(calls[4].url.searchParams.get('page'), '0')
  assert.equal(calls[5].url.searchParams.get('empresaId'), '42')
  assert.equal(calls[5].url.searchParams.get('page'), '1')
  assert.deepEqual(branches.map(branch => branch.id), [84, 85])
  assert.deepEqual(calls[6].body, { activa: false })
  assert.deepEqual(calls[1].body, company)
})
