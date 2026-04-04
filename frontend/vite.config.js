import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: [
      '.cpolar.cn',
      '.cpolar.top'
    ],
    proxy: {
      '/admin': 'http://localhost:8080',
      '/app': 'http://localhost:8081'
    }
  }
})
