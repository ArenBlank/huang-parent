import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const appProxyTarget = process.env.VITE_DEV_APP_PROXY || 'http://127.0.0.1:8081'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5174,
    allowedHosts: ['.cpolar.cn', '.cpolar.top'],
    proxy: {
      '/app': appProxyTarget
    }
  }
})
