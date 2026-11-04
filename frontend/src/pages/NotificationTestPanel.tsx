import { useState } from "react";
import {
  getUnreadCount,
  listNotifications,
  type NotificationItem,
} from "../api/notifications";

export default function NotificationTestPanel() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  async function loadNotifications() {
    setLoading(true);
    setMessage("");

    try {
      const result = await listNotifications({ page: 0, size: 20 });
      setNotifications(result.content);
      setLoaded(true);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Failed to load notifications");
    } finally {
      setLoading(false);
    }
  }

  async function refreshCount() {
    setMessage("");

    try {
      const result = await getUnreadCount();
      setUnreadCount(result.unreadCount);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Failed to load unread count");
    }
  }

  return (
    <section style={{ background: "white", padding: 22, borderRadius: 10, marginBottom: 24 }}>
      <h2 style={{ marginTop: 0 }}>Notification Service Test</h2>

      <p>
        Unread count: <strong>{unreadCount === null ? "Not loaded" : unreadCount}</strong>
      </p>

      <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
        <button
          type="button"
          disabled={loading}
          onClick={loadNotifications}
          style={buttonStyle}
        >
          {loading ? "Loading..." : "Load notifications"}
        </button>

        <button
          type="button"
          onClick={refreshCount}
          style={buttonStyle}
        >
          Refresh unread count
        </button>
      </div>

      {message && (
        <p role="alert" style={{ color: "#b91c1c", marginTop: 14 }}>
          API error: {message}
        </p>
      )}

      {loaded && (
        <p style={{ color: "#475569", marginTop: 14 }}>
          Loaded {notifications.length} notification(s) from this page.
        </p>
      )}

      {notifications.map((n) => (
        <article
          key={n.id}
          style={{
            borderTop: "1px solid #e2e8f0",
            padding: "14px 0",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
            <strong>{n.title}</strong>
            <span style={{ fontSize: 12, color: n.read ? "#64748b" : "#2563eb" }}>
              {n.read ? "Read" : "Unread"}
            </span>
          </div>

          <p style={{ margin: "6px 0" }}>{n.message}</p>

          <p style={{ margin: 0, fontSize: 12, color: "#64748b" }}>
            Type: {n.type} · ID: {n.id}
          </p>

          <p style={{ margin: "4px 0", fontSize: 12, color: "#64748b" }}>
            Created: {n.createdAt}
          </p>
        </article>
      ))}

      {loaded && notifications.length === 0 && (
        <p>No notifications found on this page.</p>
      )}
    </section>
  );
}

const buttonStyle = {
  padding: "9px 14px",
  border: "1px solid #cbd5e1",
  borderRadius: 7,
  background: "white",
  cursor: "pointer",
} as const;
