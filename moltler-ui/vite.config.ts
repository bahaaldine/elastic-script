import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      // Proxy API requests to elastic-script backend
      '/api': {
        target: 'http://localhost:9200',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/_escript'),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            // Add basic auth header
            const auth = Buffer.from('elastic-admin:elastic-password').toString('base64')
            proxyReq.setHeader('Authorization', `Basic ${auth}`)
          })
        },
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
})
