import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'
  const aiTarget = env.VITE_AI_PROXY_TARGET || 'https://dashscope.aliyuncs.com/compatible-mode/v1'
  const aiApiKey = env.VITE_DASHSCOPE_API_KEY || ''

  return {
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'react-vendor': ['react', 'react-dom', 'react-router-dom'],
            'motion-vendor': ['framer-motion'],
            'network-vendor': ['axios', '@stomp/stompjs', 'sockjs-client'],
          },
        },
      },
    },
    server: {
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
        },
        '/ws': {
          target: apiTarget,
          changeOrigin: true,
          ws: true,
        },
        '/ai-api': {
          target: aiTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/ai-api/, ''),
          configure: proxy => {
            proxy.on('proxyReq', proxyReq => {
              if (aiApiKey) {
                proxyReq.setHeader('Authorization', `Bearer ${aiApiKey}`)
              }
            })
          },
        },
      },
    },
  }
})
