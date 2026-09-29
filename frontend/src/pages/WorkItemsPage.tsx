import { FormEvent, useCallback, useEffect, useState } from "react";
import { assignWorkItem, changeStatus, createWorkItem, listWorkItems } from "../api/workItems";
import type { PageResponse, Priority, UserSummary, WorkItem, WorkStatus } from "../types";

const STATUSES: WorkStatus[] = ["NEW", "IN_PROGRESS", "BLOCKED", "COMPLETED"];
const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

// Mirrors the workflow enforced by core-service (the server is still the real gatekeeper).
const NEXT: Record<WorkStatus, WorkStatus[]> = {
  NEW: ["IN_PROGRESS"],
  IN_PROGRESS: ["BLOCKED", "COMPLETED"],
  BLOCKED: ["IN_PROGRESS"],
  COMPLETED: [],
};

const STATUS_COLOR: Record<WorkStatus, string> = {
  NEW: "#2563eb",
  IN_PROGRESS: "#d97706",
  BLOCKED: "#dc2626",
  COMPLETED: "#16a34a",
};

export default function WorkItemsPage({ user }: { user: UserSummary }) {
  const can = (perm: string) => user.permissions.includes(perm);

  const [data, setData] = useState<PageResponse<WorkItem> | null>(null);
  const [statusFilter, setStatusFilter] = useState<WorkStatus | "">("");
  const [priorityFilter, setPriorityFilter] = useState<Priority | "">("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await listWorkItems({ status: statusFilter, priority: priorityFilter, page, size: 8 }));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load work items");
    } finally {
      setLoading(false);
    }
  }, [statusFilter, priorityFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await createWorkItem({ title, description, priority });
      setTitle("");
      setDescription("");
      setPriority("MEDIUM");
      setPage(0);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create work item");
    } finally {
      setSaving(false);
    }
  }

  async function run(action: () => Promise<unknown>) {
    setError("");
    try {
      await action();
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Action failed");
    }
  }

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Work Items</h2>

      {can("WORK_CREATE") && (
        <form onSubmit={handleCreate} style={{ ...card, marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>New work item</h3>
          <input
            style={input}
            placeholder="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            maxLength={200}
          />
          <textarea
            style={{ ...input, height: 70, marginTop: 10 }}
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <div style={{ display: "flex", gap: 10, marginTop: 10 }}>
            <select style={input} value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
              {PRIORITIES.map((p) => (
                <option key={p}>{p}</option>
              ))}
            </select>
            <button style={primaryBtn} type="submit" disabled={saving}>
              {saving ? "Saving..." : "Create"}
            </button>
          </div>
        </form>
      )}

      <div style={{ display: "flex", gap: 10, marginBottom: 14 }}>
        <select
          style={{ ...input, width: 180 }}
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as WorkStatus | "");
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
        <select
          style={{ ...input, width: 180 }}
          value={priorityFilter}
          onChange={(e) => {
            setPriorityFilter(e.target.value as Priority | "");
            setPage(0);
          }}
        >
          <option value="">All priorities</option>
          {PRIORITIES.map((p) => (
            <option key={p}>{p}</option>
          ))}
        </select>
      </div>

      {error && <div style={errorBox}>{error}</div>}

      <div style={card}>
        {loading && <p>Loading...</p>}
        {!loading && data && data.content.length === 0 && <p>No work items match. Create one above.</p>}
        {!loading && data && data.content.length > 0 && (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "#64748b", fontSize: 13 }}>
                <th style={th}>#</th>
                <th style={th}>Title</th>
                <th style={th}>Status</th>
                <th style={th}>Priority</th>
                <th style={th}>Assignee</th>
                <th style={th}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((w) => (
                <tr key={w.id} style={{ borderTop: "1px solid #e2e8f0" }}>
                  <td style={td}>{w.id}</td>
                  <td style={td}>{w.title}</td>
                  <td style={td}>
                    <span style={{ ...badge, background: STATUS_COLOR[w.status] }}>{w.status}</span>
                  </td>
                  <td style={td}>{w.priority}</td>
                  <td style={td}>{w.assigneeId ?? "-"}</td>
                  <td style={td}>
                    {can("WORK_EDIT") &&
                      NEXT[w.status].map((s) => (
                        <button key={s} style={smallBtn} onClick={() => run(() => changeStatus(w.id, s))}>
                          {s}
                        </button>
                      ))}
                    {can("WORK_ASSIGN") && w.assigneeId !== user.id && (
                      <button style={smallBtn} onClick={() => run(() => assignWorkItem(w.id, user.id))}>
                        Assign to me
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {data && data.totalPages > 1 && (
          <div style={{ display: "flex", gap: 10, marginTop: 14, alignItems: "center" }}>
            <button style={smallBtn} disabled={page === 0} onClick={() => setPage(page - 1)}>
              Prev
            </button>
            <span>
              Page {data.page + 1} of {data.totalPages} ({data.totalElements} items)
            </span>
            <button style={smallBtn} disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

const card = { background: "white", padding: 22, borderRadius: 10 } as const;
const input = {
  width: "100%",
  boxSizing: "border-box",
  padding: "10px 12px",
  border: "1px solid #cbd5e1",
  borderRadius: 7,
  fontSize: 14,
  fontFamily: "inherit",
} as const;
const primaryBtn = {
  padding: "10px 22px",
  border: "none",
  borderRadius: 7,
  background: "#2563eb",
  color: "white",
  fontWeight: 600,
  cursor: "pointer",
} as const;
const smallBtn = {
  padding: "5px 10px",
  marginRight: 6,
  border: "1px solid #cbd5e1",
  borderRadius: 6,
  background: "white",
  cursor: "pointer",
  fontSize: 12,
} as const;
const badge = { color: "white", padding: "3px 9px", borderRadius: 12, fontSize: 12 } as const;
const errorBox = {
  marginBottom: 14,
  padding: 10,
  borderRadius: 6,
  background: "#fee2e2",
  color: "#b91c1c",
  fontSize: 14,
} as const;
const th = { padding: "8px 6px" } as const;
const td = { padding: "10px 6px", fontSize: 14 } as const;