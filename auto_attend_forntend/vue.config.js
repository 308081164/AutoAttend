const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8849,
    proxy: {
      '/api': {
        target: 'http://localhost:8848',
        changeOrigin: true
      }
    }
  }
})
