import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In dev, /api and /ws are proxied to the API Gateway so the browser sees one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
      "/ws": { target: "ws://localhost:8080", ws: true },
    },
  },
});
