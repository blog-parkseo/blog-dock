import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// changeOrigin: false → 백엔드가 브라우저 주소(Host)를 그대로 본다.
// 그래서 쿠키와 카카오 "돌아올 주소"가 5173 기준으로 만들어진다
const backend = { target: 'http://localhost:8080', changeOrigin: false }

export default defineConfig({
  plugins: [react()],
  server: {
    // 블로그 서브도메인(주소.blogdock.localhost)으로도 열 수 있게 한다
    allowedHosts: ['localhost', '.localhost'],
    // 화면(5173)에서 부른 요청을 백엔드(8080)로 넘긴다
    proxy: {
      '/api': backend,
      '/files': backend,
      '/oauth2': backend,
      '/login/oauth2': backend,
    },
  },
})
