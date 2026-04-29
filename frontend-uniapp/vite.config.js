import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

const appProxyTarget = process.env.VITE_DEV_APP_PROXY || 'http://127.0.0.1:8081'

export default defineConfig({
  plugins: [uni()],
  server: {
    host: '0.0.0.0',
    port: 5175,
    proxy: {
      '/app': appProxyTarget
    }
  }
})
