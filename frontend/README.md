# frontend

React + TypeScript (Vite). Talks only to the API Gateway.

    npm install
    npm run dev        # http://localhost:5173 (proxies /api and /ws to localhost:8080)

Folders under `src/`: `api/` (single API client), `auth/` (token handling, route guards),
`components/`, `pages/`, `hooks/`, `types/`.
