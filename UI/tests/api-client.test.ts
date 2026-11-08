import { test } from 'node:test'
import assert from 'node:assert/strict'
import { ApiError, createApiClient } from '../src/api/client.ts'

test('envía el token renovado a la API y omite cookies', async () => {
  const order: string[] = []
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => { order.push('token'); return 'fresh-token' },
    onUnauthorized: () => assert.fail('No debe cerrar la sesión'),
    fetcher: async (input, options) => {
      order.push('fetch')
      assert.equal(String(input), 'http://localhost:8080/api/categorias?page=0')
      assert.equal(new Headers(options?.headers).get('Authorization'), 'Bearer fresh-token')
      assert.equal(options?.credentials, 'omit')
      assert.equal(options?.redirect, 'error')
      return Response.json({ content: [] })
    },
  })
  assert.deepEqual(await api('categorias?page=0'), { content: [] })
  assert.deepEqual(order, ['token', 'fetch'])
})

test('bloquea destinos externos y rutas fuera de /api antes de obtener un token', async () => {
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => assert.fail('No debe pedir un token'),
    onUnauthorized: () => {},
    fetcher: async () => assert.fail('No debe enviar una petición'),
  })
  for (const path of ['https://example.com/steal', '//example.com/steal', '../usuarios', '/fuera', '']) {
    await assert.rejects(api(path), /ruta debe pertenecer/)
  }
})

test('una renovación fallida impide enviar la petición', async () => {
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => { throw new Error('Sesión vencida') },
    onUnauthorized: () => {},
    fetcher: async () => assert.fail('No debe enviar una petición'),
  })
  await assert.rejects(api('categorias'), /Sesión vencida/)
})

test('401 termina la sesión y no reintenta una escritura', async () => {
  let requests = 0
  let expired = 0
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => 'token',
    onUnauthorized: () => { expired++ },
    fetcher: async () => { requests++; return new Response(null, { status: 401 }) },
  })
  await assert.rejects(api('categorias', { method: 'POST', body: '{}' }), error => error instanceof ApiError && error.status === 401)
  assert.equal(requests, 1)
  assert.equal(expired, 1)
})

test('403 informa falta de permiso sin cerrar una sesión válida', async () => {
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => 'token',
    onUnauthorized: () => assert.fail('403 no debe cerrar sesión'),
    fetcher: async () => new Response(null, { status: 403 }),
  })
  await assert.rejects(api('usuarios'), error => error instanceof ApiError && error.status === 403)
})

test('respuestas 204 se aceptan sin intentar leer JSON', async () => {
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => 'token',
    onUnauthorized: () => {},
    fetcher: async (_input, options) => {
      assert.equal(new Headers(options?.headers).get('Content-Type'), 'application/json')
      return new Response(null, { status: 204 })
    },
  })
  assert.equal(await api('categorias/1', { method: 'PUT', body: '{}' }), undefined)
})

test('un error de red se presenta sin filtrar datos de la petición', async () => {
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => 'token',
    onUnauthorized: () => {},
    fetcher: async () => { throw new TypeError('Failed to fetch') },
  })
  await assert.rejects(api('categorias'), error => error instanceof ApiError && error.status === 0)
})

test('cancelar una petición conserva el error de cancelación', async () => {
  const controller = new AbortController()
  const abort = new DOMException('Cancelled', 'AbortError')
  const api = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    getToken: async () => 'token',
    onUnauthorized: () => {},
    fetcher: async () => { controller.abort(); throw abort },
  })
  await assert.rejects(api('categorias', { signal: controller.signal }), error => error === abort)
})
