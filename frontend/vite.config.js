import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const adminProxyTarget = process.env.VITE_DEV_ADMIN_PROXY || 'http://127.0.0.1:8092'
const appProxyTarget = process.env.VITE_DEV_APP_PROXY || 'http://127.0.0.1:8093'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: ['.cpolar.cn', '.cpolar.top'],
    proxy: {
      '/admin': adminProxyTarget,
      '/app': appProxyTarget
    }
  }
})
