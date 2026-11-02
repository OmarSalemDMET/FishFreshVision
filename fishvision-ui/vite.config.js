import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  server: {
    proxy: {
      // Forward these paths to the Clojure backend
      '/api': 'http://localhost:3000',
      '/uploads': 'http://localhost:3000',
      '/analyze': 'http://localhost:3000'
    }
  }
})
