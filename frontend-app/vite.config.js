import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5174,
    allowedHosts: [
      '.cpolar.cn',
      '.cpolar.top'
    ],
    proxy: {
      '/app': 'http://localhost:8081'
    }
  }
})
