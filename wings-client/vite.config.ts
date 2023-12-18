import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		proxy: {
			'/api/event' :  {
				target: 'http://localhost:8081',
				ws: false
			},
			'/websocket' :  {
				target:  'ws://localhost:8081',
				ws: true
			}
		}
	},
	test: {
		include: ['src/**/*.{test,spec}.{js,ts}']
	}
});
