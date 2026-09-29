import { FormEvent, useState } from "react";
import { ApiRequestError, register } from "./api/client";
import type { UserSummary } from "./types";

type RegisterProps = {
  onRegistered: (user: UserSummary) => void;
  onBack: () => void;
};

export default function Register({ onRegistered, onBack }: RegisterProps) {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      onRegistered(await register(fullName, email, password));
    } catch (err) {
      if (err instanceof ApiRequestError && err.details.length > 0) {
        setError(err.details.join(" | "));
      } else {
        setError(err instanceof Error ? err.message : "Could not create account");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={s.page}>
      <form onSubmit={handleSubmit} style={s.card}>
        <h1 style={{ margin: "0 0 6px" }}>Create account</h1>
        <p style={{ margin: "0 0 20px", color: "#64748b" }}>New accounts start with the standard USER role.</p>

        <label style={s.label}>Full name</label>
        <input style={s.input} value={fullName} onChange={(e) => setFullName(e.target.value)} required maxLength={100} />

        <label style={s.label}>Email</label>
        <input style={s.input} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />

        <label style={s.label}>Password</label>
        <input
          style={s.input}
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          minLength={8}
          maxLength={72}
        />
        <p style={{ fontSize: 12, color: "#64748b", margin: "6px 0 0" }}>At least 8 characters, with a letter and a number.</p>

        {error && <div style={s.error}>{error}</div>}

        <button style={s.primary} type="submit" disabled={loading}>
          {loading ? "Creating..." : "Create account"}
        </button>
        <button style={s.link} type="button" onClick={onBack}>
          Back to sign in
        </button>
      </form>
    </div>
  );
}

const s = {
  page: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background: "#f4f7fb",
    fontFamily: "Arial, sans-serif",
  },
  card: { background: "white", padding: 32, borderRadius: 12, width: 380, boxShadow: "0 4px 20px #0000001a" },
  label: { display: "block", margin: "14px 0 6px", fontSize: 14, fontWeight: 600 },
  input: {
    width: "100%",
    boxSizing: "border-box",
    padding: "10px 12px",
    border: "1px solid #cbd5e1",
    borderRadius: 7,
    fontSize: 14,
  },
  primary: {
    width: "100%",
    marginTop: 20,
    padding: 12,
    border: "none",
    borderRadius: 7,
    background: "#2563eb",
    color: "white",
    fontWeight: 600,
    cursor: "pointer",
  },
  link: {
    width: "100%",
    marginTop: 10,
    padding: 8,
    border: "none",
    background: "transparent",
    color: "#2563eb",
    cursor: "pointer",
  },
  error: { marginTop: 14, padding: 10, borderRadius: 6, background: "#fee2e2", color: "#b91c1c", fontSize: 14 },
} as const;