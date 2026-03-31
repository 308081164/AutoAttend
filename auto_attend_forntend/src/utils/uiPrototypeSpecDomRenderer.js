const SPACING_TOKENS = {
  'space-4': 4,
  'space-8': 8,
  'space-12': 12,
  'space-16': 16,
  'space-24': 24
}

const RADIUS_TOKENS = {
  'r-8': 8,
  'r-10': 10,
  'r-12': 12
}

const COLOR_TOKENS = {
  primary: '#2563eb',
  'primary-strong': '#1d4ed8',
  text: '#0f172a',
  'text-muted': '#64748b',
  bg: '#ffffff',
  'border-muted': '#e5e7eb',
  success: '#22c55e',
  info: '#0ea5e9',
  warn: '#f59e0b'
}

const SHADOW_TOKENS = {
  'shadow-soft': '0 4px 24px rgba(15, 23, 42, 0.06)',
  'shadow-none': 'none'
}

function applyTokenStyle (el, style) {
  if (!style || typeof style !== 'object') return
  if (style.padding && SPACING_TOKENS[style.padding] != null) {
    el.style.padding = SPACING_TOKENS[style.padding] + 'px'
  }
  if (style.radius && RADIUS_TOKENS[style.radius] != null) {
    el.style.borderRadius = RADIUS_TOKENS[style.radius] + 'px'
  }
  if (style.bg && COLOR_TOKENS[style.bg]) {
    el.style.background = COLOR_TOKENS[style.bg]
  }
  if (style.border && COLOR_TOKENS[style.border]) {
    el.style.border = '1px solid ' + COLOR_TOKENS[style.border]
  }
  if (style.shadow && SHADOW_TOKENS[style.shadow]) {
    el.style.boxShadow = SHADOW_TOKENS[style.shadow]
  }
}

function createBaseNodeStyle (type, el) {
  if (type === 'Card') {
    el.style.background = '#ffffff'
    el.style.border = '1px solid #e5e7eb'
    el.style.borderRadius = '12px'
    el.style.padding = '16px'
    el.style.boxShadow = 'none'
  } else if (type === 'Panel') {
    el.style.border = '1px solid #e5e7eb'
    el.style.borderRadius = '12px'
    el.style.padding = '12px'
    el.style.background = '#ffffff'
  } else if (type === 'Button') {
    el.style.border = '1px solid #1d4ed8'
    el.style.background = '#1d4ed8'
    el.style.color = '#ffffff'
    el.style.padding = '8px 12px'
    el.style.borderRadius = '10px'
    el.style.cursor = 'pointer'
    el.style.fontWeight = '700'
    el.style.fontSize = '13px'
  } else if (type === 'Badge') {
    el.style.display = 'inline-block'
    el.style.padding = '2px 8px'
    el.style.borderRadius = '999px'
    el.style.background = '#eef2ff'
    el.style.color = '#3730a3'
    el.style.fontWeight = '800'
    el.style.fontSize = '11px'
  }
}

function renderNodeDom ({ container, spec, state, interactionsBySource, nodeId, rerender }) {
  const node = spec.nodes[nodeId]
  if (!node) return document.createTextNode('—')

  const type = node.type
  const props = node.props || {}
  const style = node.style || {}
  const children = Array.isArray(node.children) ? node.children : []

  if (type === 'Page') {
    const root = document.createElement('div')
    root.style.width = '100%'
    root.style.boxSizing = 'border-box'
    root.style.padding = '12px'
    if (children.length) {
      children.forEach(cid => root.appendChild(renderNodeDom({ container, spec, state, interactionsBySource, nodeId: cid, rerender })))
    }
    return root
  }

  if (type === 'Container' || type === 'Card') {
    const el = document.createElement('div')
    el.style.boxSizing = 'border-box'
    createBaseNodeStyle(type, el)
    applyTokenStyle(el, style)
    if (children.length) {
      children.forEach(cid => el.appendChild(renderNodeDom({ container, spec, state, interactionsBySource, nodeId: cid, rerender })))
    }
    return el
  }

  if (type === 'Grid') {
    const el = document.createElement('div')
    el.style.display = 'grid'
    el.style.boxSizing = 'border-box'
    const cols = props.columns && Number.isInteger(props.columns) ? props.columns : 2
    el.style.gridTemplateColumns = `repeat(${cols}, minmax(0, 1fr))`
    el.style.gap = '12px'
    applyTokenStyle(el, style)
    children.forEach(cid => el.appendChild(renderNodeDom({ container, spec, state, interactionsBySource, nodeId: cid, rerender })))
    return el
  }

  if (type === 'Text') {
    const el = document.createElement('div')
    el.textContent = props.text != null ? String(props.text) : ''
    el.style.color = '#0f172a'
    el.style.fontSize = '14px'
    applyTokenStyle(el, style)
    return el
  }

  if (type === 'Button') {
    const el = document.createElement('button')
    el.type = 'button'
    el.textContent = props.label != null ? String(props.label) : ''
    createBaseNodeStyle(type, el)
    applyTokenStyle(el, style)
    el.addEventListener('click', () => {
      const interactions = interactionsBySource[nodeId] || []
      interactions.forEach(it => {
        if (it.type === 'togglePanel') {
          state.panelOpen[it.targetId] = !!it.params.open
        } else if (it.type === 'setTab') {
          state.tabsActive[it.targetId] = String(it.params.tabKey)
        }
      })
      rerender()
    })
    return el
  }

  if (type === 'Badge') {
    const el = document.createElement('span')
    el.textContent = props.text != null ? String(props.text) : ''
    createBaseNodeStyle(type, el)
    applyTokenStyle(el, style)
    return el
  }

  if (type === 'Panel') {
    const el = document.createElement('div')
    createBaseNodeStyle(type, el)
    applyTokenStyle(el, style)
    const open = state.panelOpen[nodeId] != null ? state.panelOpen[nodeId] : !!props.defaultOpen
    el.style.display = open ? 'block' : 'none'
    children.forEach(cid => el.appendChild(renderNodeDom({ container, spec, state, interactionsBySource, nodeId: cid, rerender })))
    return el
  }

  if (type === 'Tabs') {
    const el = document.createElement('div')
    el.style.boxSizing = 'border-box'
    const tabItems = Array.isArray(props.tabItems) ? props.tabItems : []

    const activeKey = state.tabsActive[nodeId] != null
      ? state.tabsActive[nodeId]
      : (tabItems[0] && tabItems[0].key ? tabItems[0].key : '')

    const header = document.createElement('div')
    header.style.display = 'flex'
    header.style.flexWrap = 'wrap'
    header.style.gap = '8px'
    header.style.marginBottom = '10px'

    tabItems.forEach(ti => {
      const key = ti.key
      const btn = document.createElement('button')
      btn.type = 'button'
      btn.textContent = ti.label != null ? String(ti.label) : ''
      btn.style.border = '1px solid #e5e7eb'
      btn.style.background = key === activeKey ? '#eef2ff' : '#f8fafc'
      btn.style.color = key === activeKey ? '#3730a3' : '#334155'
      btn.style.padding = '8px 10px'
      btn.style.borderRadius = '10px'
      btn.style.cursor = 'pointer'
      btn.style.fontWeight = '800'
      btn.style.fontSize = '13px'
      btn.addEventListener('click', () => {
        state.tabsActive[nodeId] = String(key)
        rerender()
      })
      header.appendChild(btn)
    })

    const body = document.createElement('div')
    tabItems.forEach(ti => {
      if (String(ti.key) === String(activeKey)) {
        body.appendChild(renderNodeDom({ container, spec, state, interactionsBySource, nodeId: ti.contentId, rerender }))
      }
    })

    el.appendChild(header)
    el.appendChild(body)
    return el
  }

  return document.createTextNode('—')
}

export function renderUiPrototypeSpecToDom (containerEl, spec) {
  if (!containerEl || !spec || !spec.nodes || !spec.layout || !spec.layout.root) return

  const interactions = Array.isArray(spec.interactions) ? spec.interactions : []
  const interactionsBySource = {}
  interactions.forEach(it => {
    if (!it || typeof it !== 'object') return
    const sourceId = it.sourceId
    if (!sourceId) return
    if (!interactionsBySource[sourceId]) interactionsBySource[sourceId] = []
    interactionsBySource[sourceId].push(it)
  })

  const state = { panelOpen: {}, tabsActive: {} }
  // 初始化 Panel/Tabs 状态
  Object.keys(spec.nodes).forEach(id => {
    const n = spec.nodes[id]
    if (!n || !n.type) return
    if (n.type === 'Panel') state.panelOpen[id] = !!(n.props && n.props.defaultOpen)
    if (n.type === 'Tabs') {
      const tabItems = n.props && Array.isArray(n.props.tabItems) ? n.props.tabItems : []
      if (n.props && n.props.defaultTabKey) state.tabsActive[id] = String(n.props.defaultTabKey)
      else if (tabItems[0] && tabItems[0].key) state.tabsActive[id] = String(tabItems[0].key)
    }
  })

  const renderAll = () => {
    containerEl.innerHTML = ''
    const rootId = spec.layout.root
    containerEl.appendChild(renderNodeDom({
      container: containerEl,
      spec,
      state,
      interactionsBySource,
      nodeId: rootId,
      rerender: renderAll
    }))
  }

  renderAll()
}

