const { defineConfig } = require('@vue/cli-service')
// 客户阅览看板「复制链接」的公网前缀：与当前浏览器 origin 不一致时（反代、域名访问），在 .env.production 设置 VUE_APP_PUBLIC_ORIGIN=https://你的域名
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
