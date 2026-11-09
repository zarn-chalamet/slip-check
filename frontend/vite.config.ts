import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Backend API during development
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})