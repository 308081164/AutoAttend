import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue2'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      vue: 'vue/dist/vue.esm.js'
    }
  },
  server: {
    port: 8850,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8848',
        changeOrigin: true
      }
    }
  }
})
