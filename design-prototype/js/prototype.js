(function () {
  'use strict'

  function initMobileNav () {
    var toggle = document.getElementById('menuToggle')
    var panel = document.getElementById('mobileNav')
    if (!toggle || !panel) return

    toggle.addEventListener('click', function () {
      var open = panel.classList.toggle('open')
      panel.setAttribute('aria-hidden', open ? 'false' : 'true')
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false')
    })

    panel.querySelectorAll('a').forEach(function (link) {
      link.addEventListener('click', function () {
        panel.classList.remove('open')
        panel.setAttribute('aria-hidden', 'true')
        toggle.setAttribute('aria-expanded', 'false')
      })
    })
  }

  function initLoginMock () {
    var form = document.getElementById('loginForm')
    if (!form) return

    var err = document.getElementById('formError')
    var btn = document.getElementById('submitBtn')

    form.addEventListener('submit', function (e) {
      e.preventDefault()
      if (err) {
        err.textContent = ''
        err.classList.remove('visible')
      }

      var phone = (document.getElementById('phone') || {}).value || ''
      var pass = (document.getElementById('password') || {}).value || ''

      if (!phone.trim() || !pass) {
        if (err) {
          err.textContent = '请填写手机号与密码'
          err.classList.add('visible')
        }
        return
      }

      if (btn) {
        btn.disabled = true
        btn.textContent = '登录中…'
      }

      window.setTimeout(function () {
        if (btn) {
          btn.disabled = false
          btn.textContent = '登录'
        }
        if (err) {
          err.textContent = '（原型）登录成功：此处可接跳转到控制台路由'
          err.style.color = 'var(--text-secondary)'
          err.classList.add('visible')
        }
      }, 700)
    })
  }

  initMobileNav()
  initLoginMock()
})()
