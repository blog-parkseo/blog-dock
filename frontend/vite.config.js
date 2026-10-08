import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // 화면(5173)에서 부른 /api 요청을 백엔드(8080)로 넘긴다
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
