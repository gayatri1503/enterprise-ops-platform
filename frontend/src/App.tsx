import { useEffect, useState } from "react";
import Login from "./Login";
import WorkItemsPage from "./pages/WorkItemsPage";
import { logout as apiLogout, refreshSession, setSessionExpiredHandler } from "./api/client";
import type { UserSummary } from "./types";
import Register from "./Register";

export default function App() {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [booting, setBooting] = useState(true);
  const [mode, setMode] = useState<"login" | "register">("login");

  useEffect(() => {
    setSessionExpiredHandler(() => setUser(null));
    // On page load, try to restore the session from the refresh cookie.
    refreshSession()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setBooting(false));
  }, []);

  async function logout() {
    await apiLogout();
    setUser(null);
  }

  if (booting) return <p style={{ padding: 40, fontFamily: "Arial, sans-serif" }}>Loading...</p>;
    if (!user) {
    if (mode === "register") {
      return <Register onRegistered={setUser} onBack={() => setMode("login")} />;
    }
    return (
      <>
        <Login onLogin={setUser} />
        <button
          onClick={() => setMode("register")}
          style={{
            position: "fixed",
            bottom: 24,
            left: "50%",
            transform: "translateX(-50%)",
            padding: "10px 18px",
            border: "none",
            borderRadius: 8,
            background: "transparent",
            color: "#2563eb",
            fontSize: 15,
            cursor: "pointer",
          }}
        >
          New here? Create an account
        </button>
      </>
    );
  }
  return (
    <div style={{ minHeight: "100vh", background: "#f4f7fb", fontFamily: "Arial, sans-serif" }}>
      <header
        style={{
          background: "#172554",
          color: "white",
          padding: "18px 40px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: 22 }}>Enterprise Operations Portal</h1>
          <p style={{ margin: "4px 0 0", color: "#c7d2fe", fontSize: 13 }}>
            {user.fullName} · {user.role}
          </p>
        </div>
        <button
          onClick={logout}
          style={{
            padding: "9px 16px",
            borderRadius: 6,
            border: "1px solid #ffffff55",
            background: "transparent",
            color: "white",
            cursor: "pointer",
          }}
        >
          Logout
        </button>
      </header>

      <main style={{ padding: 40 }}>
        <WorkItemsPage user={user} />
      </main>
    </div>
  );
}