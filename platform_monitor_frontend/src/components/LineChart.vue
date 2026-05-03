<template>
  <div class="line-chart-wrapper">
    <canvas ref="canvas" style="width: 100%; height: 240px;"></canvas>
  </div>
</template>

<script>
export default {
  name: 'LineChart',
  props: {
    labels: { type: Array, default: () => [] },
    values: { type: Array, default: () => [] }
  },
  watch: {
    labels: 'draw',
    values: 'draw'
  },
  mounted () {
    this.draw()
  },
  methods: {
    draw () {
      const canvas = this.$refs.canvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const rect = canvas.parentElement.getBoundingClientRect()
      const dpr = window.devicePixelRatio || 1
      canvas.width = (rect.width || 600) * dpr
      canvas.height = 240 * dpr
      canvas.style.width = (rect.width || 600) + 'px'
      canvas.style.height = '240px'
      ctx.scale(dpr, dpr)
      const w = canvas.width / dpr
      const h = 240

      ctx.clearRect(0, 0, w, h)

      const labels = this.labels || []
      const values = this.values || []
      if (!labels.length || !values.length) {
        ctx.fillStyle = '#c0c4cc'
        ctx.font = '14px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('暂无数据', w / 2, h / 2)
        return
      }

      const pad = { top: 20, right: 20, bottom: 30, left: 40 }
      const chartW = w - pad.left - pad.right
      const chartH = h - pad.top - pad.bottom

      const maxVal = Math.max(...values, 1)
      const minVal = Math.min(...values, 0)
      const range = maxVal - minVal || 1

      const xs = labels.map((_, i) => pad.left + (i / (labels.length - 1 || 1)) * chartW)
      const ys = values.map(v => pad.top + chartH - ((v - minVal) / range) * chartH)

      // grid lines
      ctx.strokeStyle = '#f0f0f0'
      ctx.lineWidth = 1
      for (let i = 0; i <= 4; i++) {
        const y = pad.top + (i / 4) * chartH
        ctx.beginPath()
        ctx.moveTo(pad.left, y)
        ctx.lineTo(w - pad.right, y)
        ctx.stroke()
      }

      // line
      ctx.beginPath()
      ctx.strokeStyle = '#409eff'
      ctx.lineWidth = 2
      ctx.lineJoin = 'round'
      ctx.lineCap = 'round'
      xs.forEach((x, i) => {
        if (i === 0) ctx.moveTo(x, ys[i])
        else ctx.lineTo(x, ys[i])
      })
      ctx.stroke()

      // dots
      ctx.fillStyle = '#409eff'
      xs.forEach((x, i) => {
        ctx.beginPath()
        ctx.arc(x, ys[i], 3, 0, Math.PI * 2)
        ctx.fill()
      })

      // labels
      ctx.fillStyle = '#909399'
      ctx.font = '11px sans-serif'
      ctx.textAlign = 'center'
      const step = Math.max(1, Math.floor(labels.length / 10))
      labels.forEach((label, i) => {
        if (i % step === 0 || i === labels.length - 1) {
          ctx.fillText(label, xs[i], h - 8)
        }
      })

      // y-axis labels
      ctx.textAlign = 'right'
      ctx.textBaseline = 'middle'
      for (let i = 0; i <= 4; i++) {
        const val = minVal + (range * (4 - i)) / 4
        const y = pad.top + (i / 4) * chartH
        ctx.fillText(Math.round(val).toString(), pad.left - 8, y)
      }
    }
  }
}
</script>

<style scoped>
.line-chart-wrapper {
  width: 100%;
}
</style>
