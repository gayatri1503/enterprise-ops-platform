import { FormEvent, useState } from "react";
import { login } from "./api/client";
import type { UserSummary } from "./types";

type LoginProps = {
  onLogin: (user: UserSummary) => void;
};

export default function Login({ onLogin }: LoginProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      // login() keeps the access token in memory; the refresh token arrives as an HttpOnly cookie.
      onLogin(await login(email, password));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to login");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <h1 style={styles.title}>Enterprise Operations Portal</h1>

        <p style={styles.subtitle}>
          Sign in to access the operations dashboard
        </p>

        <form onSubmit={handleSubmit}>
          <label style={styles.label}>Email</label>

          <input
            style={styles.input}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email"
            required
          />

          <label style={styles.label}>Password</label>

          <input
            style={styles.input}
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter your password"
            required
          />

          {error && <div style={styles.error}>{error}</div>}

          <button
            style={styles.button}
            type="submit"
            disabled={loading}
          >
            {loading ? "Signing in..." : "Sign In"}
          </button>
        </form>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    background: "#f4f7fb",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontFamily: "Arial, sans-serif",
  },

  card: {
    width: "400px",
    background: "white",
    padding: "40px",
    borderRadius: "12px",
    boxShadow: "0 8px 30px rgba(0,0,0,0.08)",
  },

  title: {
    margin: "0 0 10px",
    color: "#172554",
    fontSize: "25px",
  },

  subtitle: {
    color: "#64748b",
    marginBottom: "30px",
  },

  label: {
    display: "block",
    marginBottom: "7px",
    marginTop: "18px",
    fontWeight: 600,
  },

  input: {
    width: "100%",
    boxSizing: "border-box" as const,
    padding: "12px",
    border: "1px solid #cbd5e1",
    borderRadius: "7px",
    fontSize: "15px",
  },

  button: {
    width: "100%",
    marginTop: "25px",
    padding: "12px",
    border: "none",
    borderRadius: "7px",
    background: "#2563eb",
    color: "white",
    fontSize: "15px",
    fontWeight: 600,
    cursor: "pointer",
  },

  error: {
    marginTop: "15px",
    padding: "10px",
    borderRadius: "6px",
    background: "#fee2e2",
    color: "#b91c1c",
    fontSize: "14px",
  },
};