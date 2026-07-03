import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 서버(5173)에서 /api 요청을 백엔드(8080)로 프록시한다. 같은 오리진처럼 동작해
// CORS 가 필요 없고, 운영(nginx)과 호출 방식이 동일해진다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
