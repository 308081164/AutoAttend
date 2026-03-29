const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  chainWebpack: config => {
    config.plugin('html').tap(args => {
      args[0].title = '流帮Project'
      return args
    })
  },
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
