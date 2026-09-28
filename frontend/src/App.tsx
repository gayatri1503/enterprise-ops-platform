import { useEffect, useState } from "react";

// Smoke test: proves the browser -> gateway -> each service path works.
// Replace this with the real app shell (router, auth, layout) in Sprint 1.
const CHECKS = [
  { name: "auth-service", url: "/api/auth/ping" },
  { name: "user-service", url: "/api/users/ping" },
  { name: "core-service", url: "/api/work-items/ping" },
  { name: "analytics-service", url: "/api/analytics/ping" },
  { name: "notification-service", url: "/api/notifications/ping" },
];

export default function App() {
  const [status, setStatus] = useState<Record<string, string>>({});

  useEffect(() => {
    CHECKS.forEach(async ({ name, url }) => {
      try {
        const res = await fetch(url);
        setStatus((s) => ({ ...s, [name]: res.ok ? "UP" : `HTTP ${res.status}` }));
      } catch {
        setStatus((s) => ({ ...s, [name]: "DOWN" }));
      }
    });
  }, []);

  return (
    <main style={{ fontFamily: "sans-serif", padding: 24 }}>
      <h1>Enterprise Operations Portal</h1>
      <h2>Service status (via gateway)</h2>
      <ul>
        {CHECKS.map(({ name }) => (
          <li key={name}>
            {name}: <strong>{status[name] ?? "..."}</strong>
          </li>
        ))}
      </ul>
    </main>
  );
}
