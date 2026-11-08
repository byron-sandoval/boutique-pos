import { test } from 'node:test'
import assert from 'node:assert/strict'
import { findPanelPage, navigation, visibleNavigation } from '../src/routes/navigation.ts'

test('ADMIN conserva todos los módulos y submenús', () => {
  assert.equal(visibleNavigation(['ADMIN']).length, navigation.length)
  for (const path of ['/usuarios', '/proveedores', '/ventas', '/ventas/nueva', '/compras', '/compras/nueva', '/configuracion/empresa']) {
    assert.ok(findPanelPage(path, ['ADMIN']))
  }
})

test('CAJERO no obtiene rutas administrativas aunque escriba su dirección', () => {
  for (const path of ['/usuarios', '/proveedores', '/compras', '/compras/nueva', '/configuracion/empresa', '/configuracion/monedas']) {
    assert.equal(findPanelPage(path, ['CAJERO']), undefined)
  }
  for (const path of ['/ventas', '/ventas/nueva', '/caja', '/productos/categorias', '/inventario', '/inventario/alertas', '/clientes']) {
    assert.ok(findPanelPage(path, ['CAJERO']))
  }
})

test('una cuenta sin roles reconocidos no obtiene navegación', () => {
  assert.deepEqual(visibleNavigation([]), [])
  assert.deepEqual(visibleNavigation(['USER']), [])
  assert.equal(findPanelPage('/inicio', []), undefined)
})

test('las rutas son únicas y una ruta desconocida no abre otro módulo', () => {
  const paths = navigation.flatMap(item => item.links.map(link => link.path))
  assert.equal(new Set(paths).size, paths.length)
  assert.equal(findPanelPage('/no-existe', ['ADMIN']), undefined)
})
