import {defineConfig} from 'vite'
import {svelte} from '@sveltejs/vite-plugin-svelte'

// https://vite.dev/config/
export default defineConfig({
    base: process.env.BASE_URL || '/',
    plugins: [svelte()],
    server: {
        port: 3001,
        open: true, // Automatically opens browser
        strictPort: true, // Use fixed port for predictable WS URL
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true,
                secure: false
            },
            '/ws': {
                target: 'http://127.0.0.1:8080', // use HTTP target, ws upgrade handled by ws:true
                changeOrigin: true,
                ws: true,
                secure: false
            }
        }
    }
})
