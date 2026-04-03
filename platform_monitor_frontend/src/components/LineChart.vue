<template>
  <div class="chart-wrap">
    <canvas ref="canvas"></canvas>
  </div>
</template>

<script>
import Chart from 'chart.js/auto'

export default {
  name: 'LineChart',
  props: {
    labels: { type: Array, required: true },
    values: { type: Array, required: true }
  },
  data () {
    return { chart: null }
  },
  mounted () {
    this.renderChart()
  },
  watch: {
    labels () { this.renderChart() },
    values () { this.renderChart() }
  },
  beforeDestroy () {
    if (this.chart) this.chart.destroy()
  },
  methods: {
    renderChart () {
      if (!this.$refs.canvas) return
      const ctx = this.$refs.canvas.getContext('2d')
      if (this.chart) this.chart.destroy()

      this.chart = new Chart(ctx, {
        type: 'line',
        data: {
          labels: this.labels,
          datasets: [
            {
              label: 'DAU',
              data: this.values,
              borderColor: '#60a5fa',
              backgroundColor: 'rgba(96, 165, 250, 0.15)',
              tension: 0.25,
              pointRadius: 3,
              pointHoverRadius: 4
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            tooltip: { mode: 'index', intersect: false }
          },
          interaction: { mode: 'nearest', axis: 'x', intersect: false },
          scales: {
            x: { grid: { display: false }, ticks: { maxTicksLimit: 8 } },
            y: { beginAtZero: true }
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.chart-wrap {
  width: 100%;
  height: 320px;
}
</style>

