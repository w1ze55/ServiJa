import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  publicDir: 'public', // Garante que arquivos da pasta public sejam copiados
  build: {
    assetsInlineLimit: 0, // Não fazer inline de assets pequenos
    rollupOptions: {
      output: {
        assetFileNames: (assetInfo) => {
          // Manter favicon.ico na raiz do dist
          if (assetInfo.name === 'favicon.ico') {
            return '[name][extname]'
          }
          return 'assets/[name]-[hash][extname]'
        }
      }
    }
  }
})

