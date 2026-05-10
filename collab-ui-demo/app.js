;(function () {
  const D = window.COLLAB_DEMO || {}

  function esc(s) {
    if (s == null) return ''
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/"/g, '&quot;')
  }

  function initTopbar() {
    const p = D.project || {}
    const nameEl = document.getElementById('demo-project-name')
    const subEl = document.getElementById('demo-project-sub')
    const envEl = document.getElementById('demo-env-pill')
    if (nameEl) nameEl.textContent = p.name || '协作看板'
    if (subEl) {
      const repo = p.repoFullName || '—'
      const br = p.branch || '—'
      const ph = p.phase || '—'
      subEl.textContent = repo + ' · ' + br + ' · ' + ph
    }
    if (envEl) envEl.textContent = p.envLabel || 'Demo'
  }

  function initSummary() {
    const p = D.project || {}
    const set = (id, v) => {
      const el = document.getElementById(id)
      if (el) el.textContent = v || '—'
    }
    set('summary-repo', p.repoFullName)
    set('summary-branch', p.branch)
    set('summary-phase', p.phase)
    set('summary-owner', p.owner)
    set('summary-created', p.createdAt)
  }

  function initKpis() {
    const row = document.getElementById('kpi-row')
    if (!row || !Array.isArray(D.kpis)) return
    row.innerHTML = D.kpis
      .map(
        (k, i) => `
      <article class="kpi-card">
        <div class="kpi-card__label">${esc(k.label)}</div>
        <div class="kpi-card__value">${esc(k.value)}</div>
        <div class="kpi-card__spark ${i % 2 ? 'kpi-card__spark--alt' : ''}" aria-hidden="true"></div>
      </article>`
      )
      .join('')
  }

  function initPortalItems() {
    const ul = document.getElementById('portal-items')
    if (!ul || !Array.isArray(D.portalLinks)) return
    ul.innerHTML = D.portalLinks
      .slice()
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      .map(
        (l) =>
          `<li><a href="${esc(l.url)}" target="_blank" rel="noopener noreferrer">${esc(l.label)}</a></li>`
      )
      .join('')
  }

  function initDaily() {
    const wrap = document.getElementById('daily-scroll')
    if (!wrap || !Array.isArray(D.dailySummaries)) return
    wrap.innerHTML = D.dailySummaries
      .map(
        (d) => `
      <article class="daily-card">
        <div class="daily-card__date">${esc(d.date)}</div>
        <h3 class="daily-card__title">${esc(d.title)}</h3>
        <p class="daily-card__summary">${esc(d.summary)}</p>
      </article>`
      )
      .join('')
  }

  function initEmbedDashboard() {
    const grid = document.getElementById('embed-dashboard-grid')
    if (!grid || !Array.isArray(D.embedDashboardTitles)) return
    grid.innerHTML = D.embedDashboardTitles
      .map(
        (t) => `
      <div class="embed-chart">
        <div class="embed-chart__title">${esc(t)}</div>
        <div class="embed-chart__fake" aria-hidden="true"></div>
      </div>`
      )
      .join('')
  }

  function fillChartGrid(elId) {
    const grid = document.getElementById(elId)
    if (!grid || !Array.isArray(D.embedDashboardTitles)) return
    grid.innerHTML = D.embedDashboardTitles
      .map(
        (t) => `
      <div class="chart-board-card">
        <h4>${esc(t)}</h4>
        <div class="embed-chart__fake" style="min-height:72px" aria-hidden="true"></div>
      </div>`
      )
      .join('')
  }

  function initIssueTable() {
    const tb = document.getElementById('issue-tbody')
    if (!tb || !Array.isArray(D.issueRows)) return
    tb.innerHTML = D.issueRows
      .map(
        (r) => `
      <tr>
        <td>${esc(r.problem)}</td>
        <td>${esc(r.module)}</td>
        <td>${esc(r.status)}</td>
        <td>${esc(r.accept)}</td>
        <td>${esc(r.important)}</td>
        <td>${esc(r.owner)}</td>
      </tr>`
      )
      .join('')
  }

  function initFeatureTable() {
    const tb = document.getElementById('feature-tbody')
    if (!tb || !Array.isArray(D.featureRows)) return
    tb.innerHTML = D.featureRows
      .map(
        (r) => `
      <tr>
        <td>${esc(r.name)}</td>
        <td>${esc(r.desc)}</td>
        <td>${esc(r.module)}</td>
        <td>${esc(r.progress)}</td>
        <td>${esc(r.important)}</td>
        <td>${esc(r.owner)}</td>
      </tr>`
      )
      .join('')
  }

  function initMailAiClientForms() {
    const m = D.mailNotify || {}
    const hint = document.getElementById('mail-smtp-hint')
    if (hint) hint.textContent = m.smtpHint || ''
    const repo = document.getElementById('mail-repo')
    if (repo) repo.value = m.repoFullName || ''
    const me = document.getElementById('mail-enabled')
    if (me) me.checked = !!m.enabled
    const mm = document.getElementById('mail-managers')
    if (mm) mm.checked = !!m.sendToManagers
    const md = document.getElementById('mail-devs')
    if (md) md.checked = !!m.sendToDevelopers
    const mt = document.getElementById('mail-managers-text')
    if (mt) mt.value = m.managerEmailsText || ''

    const ai = D.aiLinkage || {}
    const am = document.getElementById('ai-automation-mode')
    if (am) am.value = ai.automationMode || 'off'
    const mc = document.getElementById('ai-min-confidence')
    if (mc) mc.value = ai.minConfidence || 'medium'

    const c = D.clientBoard || {}
    const ce = document.getElementById('client-enabled')
    if (ce) ce.checked = !!c.enabled
    const cp = document.getElementById('client-show-progress')
    if (cp) cp.checked = !!c.showProgressDashboard
    const cb = document.getElementById('client-show-backlog')
    if (cb) cb.checked = !!c.showFeatureBacklog
    const ca = document.getElementById('client-show-ai')
    if (ca) ca.checked = !!c.showAiTableEntry
    const ap = document.getElementById('client-ai-purpose')
    if (ap) ap.value = c.aiPurpose || 'issue_tracking'
    const url = document.getElementById('client-public-url')
    if (url) url.value = c.publicUrl || ''
    const tok = document.getElementById('client-token')
    if (tok) tok.value = c.token || ''
  }

  function initPortalModal() {
    const modal = document.getElementById('portal-modal')
    const list = document.getElementById('portal-edit-list')
    const openBtn = document.getElementById('btn-open-portal-modal')
    const closeBtn = document.getElementById('portal-modal-close')
    const saveBtn = document.getElementById('portal-modal-save')
    const addBtn = document.getElementById('portal-modal-add')

    function portalById(id) {
      const n = Number(id)
      return (D.portalLinks || []).find((x) => Number(x.id) === n)
    }

    function renderEditList() {
      if (!list || !Array.isArray(D.portalLinks)) return
      list.innerHTML = D.portalLinks
        .slice()
        .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
        .map(
          (l) => `
        <li data-id="${esc(l.id)}">
          <label style="display:flex;flex-direction:column;gap:4px;font-size:12px;color:var(--text-muted)">标签
            <input type="text" class="pe-label" value="${esc(l.label)}" />
          </label>
          <label style="display:flex;flex-direction:column;gap:4px;font-size:12px;color:var(--text-muted)">URL
            <input type="text" class="pe-url" value="${esc(l.url)}" />
          </label>
          <button type="button" class="btn btn--danger btn--sm pe-del" data-id="${esc(l.id)}" title="Demo 移除">删</button>
        </li>`
        )
        .join('')

      list.querySelectorAll('.pe-del').forEach((btn) => {
        btn.addEventListener('click', function () {
          const id = Number(btn.getAttribute('data-id'))
          const arr = D.portalLinks
          const ix = arr.findIndex((x) => Number(x.id) === id)
          if (ix >= 0) {
            arr.splice(ix, 1)
            renderEditList()
          }
        })
      })
    }

    function syncFromDom() {
      if (!list) return
      list.querySelectorAll('li[data-id]').forEach((li) => {
        const id = Number(li.getAttribute('data-id'))
        const row = portalById(id)
        if (!row) return
        const lab = li.querySelector('.pe-label')
        const url = li.querySelector('.pe-url')
        if (lab && lab.value.trim()) row.label = lab.value.trim()
        if (url && url.value.trim()) row.url = url.value.trim()
      })
    }

    function openModal() {
      renderEditList()
      if (modal) {
        modal.classList.add('is-open')
        modal.setAttribute('aria-hidden', 'false')
      }
    }

    function closeModal() {
      syncFromDom()
      if (modal) {
        modal.classList.remove('is-open')
        modal.setAttribute('aria-hidden', 'true')
      }
      initPortalItems()
    }

    if (addBtn) {
      addBtn.addEventListener('click', function () {
        const arr = D.portalLinks
        if (!Array.isArray(arr)) return
        const maxId = arr.reduce((m, x) => Math.max(m, Number(x.id) || 0), 0)
        const maxOrd = arr.reduce((m, x) => Math.max(m, Number(x.sortOrder) || 0), 0)
        arr.push({ id: maxId + 1, label: '新链接', url: 'https://', sortOrder: maxOrd + 1 })
        renderEditList()
      })
    }

    if (openBtn) openBtn.addEventListener('click', openModal)
    if (closeBtn) closeBtn.addEventListener('click', closeModal)
    if (saveBtn) saveBtn.addEventListener('click', closeModal)
    if (modal) {
      modal.addEventListener('click', function (e) {
        if (e.target === modal) closeModal()
      })
    }

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && modal && modal.classList.contains('is-open')) {
        closeModal()
      }
    })
  }

  function initAdminSessionToggle() {
    const cb = document.getElementById('demo-admin-session')
    if (!cb) return
    function apply() {
      if (cb.checked) document.body.classList.add('demo-admin')
      else document.body.classList.remove('demo-admin')
      const hint = document.getElementById('demo-user-hint')
      if (hint) {
        hint.textContent = cb.checked
          ? '管理员会话 · 可编辑协作配置（邮件 / AI / 客户看板）'
          : '以成员身份浏览 · 顶栏 Tab 导航'
      }
    }
    cb.addEventListener('change', apply)
    apply()
  }

  function initMainTabs() {
    const tabBtns = document.querySelectorAll('.view-tabs__btn')
    const panels = {
      dashboard: document.getElementById('panel-dashboard'),
      adjust: document.getElementById('panel-adjust'),
      features: document.getElementById('panel-features')
    }

    function showView(view) {
      Object.keys(panels).forEach((k) => {
        const p = panels[k]
        if (!p) return
        if (k === view) {
          p.classList.add('is-visible')
          p.removeAttribute('hidden')
        } else {
          p.classList.remove('is-visible')
          p.setAttribute('hidden', '')
        }
      })
      tabBtns.forEach((b) => {
        const v = b.getAttribute('data-view')
        const on = v === view
        b.classList.toggle('is-active', on)
        b.setAttribute('aria-selected', on ? 'true' : 'false')
      })
    }

    tabBtns.forEach((btn) => {
      btn.addEventListener('click', function () {
        showView(btn.getAttribute('data-view'))
      })
    })

    document.querySelectorAll('[data-view-link]').forEach((a) => {
      a.addEventListener('click', function (e) {
        e.preventDefault()
        showView(a.getAttribute('data-view-link'))
      })
    })
  }

  function bindInnerTabs(panelKey) {
    const bar = document.querySelector('[data-inner-panel="' + panelKey + '"]')
    if (!bar) return
    const shell = bar.closest('.table-shell')
    if (!shell) return
    const prefix = panelKey === 'adjust' ? 'adjust' : 'features'
    const tabs = bar.querySelectorAll('button[data-inner]')
    const tablePanel = shell.querySelector('[data-inner-content="' + prefix + '-table"]')
    const boardPanel = shell.querySelector('[data-inner-content="' + prefix + '-board"]')

    function showInner(which) {
      tabs.forEach((t) => {
        const w = t.getAttribute('data-inner')
        t.classList.toggle('is-active', w === which)
      })
      if (tablePanel) {
        tablePanel.classList.toggle('is-visible', which === 'table')
      }
      if (boardPanel) {
        boardPanel.classList.toggle('is-visible', which === 'board')
      }
    }

    tabs.forEach((t) => {
      t.addEventListener('click', function () {
        showInner(t.getAttribute('data-inner'))
      })
    })
  }

  function init() {
    initTopbar()
    initSummary()
    initKpis()
    initPortalItems()
    initDaily()
    initEmbedDashboard()
    initIssueTable()
    initFeatureTable()
    initMailAiClientForms()
    initPortalModal()
    initAdminSessionToggle()
    initMainTabs()
    bindInnerTabs('adjust')
    bindInnerTabs('features')
    fillChartGrid('chart-adjust-grid')
    fillChartGrid('chart-features-grid')
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init)
  } else {
    init()
  }
})()
