import { test } from 'node:test'
import assert from 'node:assert/strict'
import { legacyPanelUrl, loginRedirectUri, panelPath } from '../src/routes/paths.ts'

const origin = 'http://localhost:5173'

test('las rutas limpias conservan el módulo y la raíz muestra inicio', () => {
  assert.equal(panelPath('/'), '/inicio')
  assert.equal(panelPath('/ventas'), '/ventas')
  assert.equal(panelPath('/productos/categorias/'), '/productos/categorias')
  assert.equal(panelPath('/ruta-desconocida'), '/ruta-desconocida')
})

test('los enlaces antiguos del panel se convierten a rutas limpias', () => {
  assert.equal(legacyPanelUrl({ origin, hash: '#/ventas' }), '/ventas')
  assert.equal(legacyPanelUrl({ origin, hash: '#/productos/categorias?page=2' }), '/productos/categorias?page=2')
})

test('la migración nunca modifica los fragmentos de respuesta de Keycloak', () => {
  for (const hash of ['', '#state=prueba&session_state=prueba&code=prueba', '#error=access_denied', '#main-content']) {
    assert.equal(legacyPanelUrl({ origin, hash }), null)
  }
})

test('un fragmento antiguo no puede redirigir a otro dominio', () => {
  assert.equal(legacyPanelUrl({ origin, hash: '#//example.com/ventas' }), null)
  assert.equal(legacyPanelUrl({ origin, hash: '#/\\example.com/ventas' }), null)
})

test('el regreso de Keycloak conserva el módulo sin copiar parámetros del callback', () => {
  const callback = new URL(`${origin}/ventas?code=ejemplo&state=ejemplo#session_state=ejemplo`)
  assert.equal(loginRedirectUri(callback), `${origin}/ventas`)
  assert.equal(loginRedirectUri(new URL(`${origin}/productos/categorias`)), `${origin}/productos/categorias`)
  assert.equal(loginRedirectUri(new URL(origin)), `${origin}/`)
})
