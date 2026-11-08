/* Mejora visual: conserva intactos los formularios y flujos de Keycloak. */
(() => {
  function initializeBrandPanel() {
    const login = document.querySelector('.login-pf-page')
    if (!login || document.querySelector('.boutique-story')) return

    const shoppingBag = '<svg viewBox="0 0 48 42" fill="none" aria-hidden="true"><path d="M12 12h24l3 26H9l3-26Z" stroke="currentColor" stroke-width="2.4" stroke-linejoin="round"/><path d="M18 15V9a6 6 0 0 1 12 0v6" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>'

    const panel = document.createElement('aside')
    panel.className = 'boutique-story'
    panel.setAttribute('aria-label', 'Boutique POS')
    panel.innerHTML = `
      <div class="boutique-photo" aria-hidden="true"></div>
      <div class="boutique-wordmark">${shoppingBag}<span>Boutique <b>POS</b></span></div>
      <div class="boutique-story-content">
        <p class="boutique-eyebrow">SISTEMA DE GESTIÓN PARA BOUTIQUES</p>
        <h1>Tu colección.<br>Tu negocio.<br><em>En armonía.</em></h1>
        <p class="boutique-description">Ventas, inventario, caja y clientes<br>en un solo lugar.</p>
        <div class="boutique-features">
          <div><span><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M3 3h2l3 12h10l3-8H6M9 19h.01M18 19h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg></span>Ventas<br>rápidas</div>
          <div><span><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m12 2 9 5v10l-9 5-9-5V7l9-5Zm0 10 9-5M12 12 3 7m9 5v10M7.5 4.5l9 5" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg></span>Control de<br>inventario</div>
          <div><span><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 14h3v7H4v-7Zm7-6h3v13h-3V8Zm7-5h3v18h-3V3Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg></span>Resumen<br>de ventas</div>
        </div>
      </div>
    `

    login.before(panel)
    document.body.classList.add('boutique-split-layout')

    const header = document.getElementById('kc-header-wrapper')
    if (header) {
      header.classList.add('boutique-login-brand')
      header.innerHTML = '<span>Boutique <b>POS</b></span>'
      const card = login.querySelector('.card-pf')
      const brandHeader = document.getElementById('kc-header')
      if (card && brandHeader) card.prepend(brandHeader)
    }

    const form = document.getElementById('kc-form-login')
    if (form) {
      const title = document.getElementById('kc-page-title')
      const subtitle = document.createElement('p')
      subtitle.className = 'boutique-login-subtitle'
      subtitle.textContent = 'Inicia sesión para gestionar tu boutique'
      title?.after(subtitle)
      const username = document.getElementById('username')
      const password = document.getElementById('password')
      username?.setAttribute('placeholder', 'Ingresa tu usuario o correo')
      password?.setAttribute('placeholder', 'Ingresa tu contraseña')
    }

    const card = login.querySelector('.card-pf')
    if (card) {
      const footer = document.createElement('div')
      footer.className = 'boutique-login-footer'
      footer.innerHTML = `<div>${shoppingBag}<strong>Boutique POS</strong></div><p>Tu boutique, siempre bajo control.</p>`
      card.append(footer)
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeBrandPanel, { once: true })
  } else {
    initializeBrandPanel()
  }
})()
