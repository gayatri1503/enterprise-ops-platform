import { apiFetch } from "./client";
import type { PageResponse, Priority, WorkItem, WorkStatus } from "../types";

export interface ListParams {
  status: WorkStatus | "";
  priority: Priority | "";
  page: number;
  size: number;
}

export function listWorkItems(p: ListParams) {
  const q = new URLSearchParams();
  if (p.status) q.set("status", p.status);
  if (p.priority) q.set("priority", p.priority);
  q.set("page", String(p.page));
  q.set("size", String(p.size));
  q.set("sort", "createdAt,desc");
  return apiFetch<PageResponse<WorkItem>>(`/api/work-items?${q.toString()}`);
}

export function createWorkItem(body: { title: string; description: string; priority: Priority }) {
  return apiFetch<WorkItem>("/api/work-items", { method: "POST", body: JSON.stringify(body) });
}

export function changeStatus(id: number, status: WorkStatus) {
  return apiFetch<WorkItem>(`/api/work-items/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export function assignWorkItem(id: number, assigneeId: number) {
  return apiFetch<WorkItem>(`/api/work-items/${id}/assignee`, {
    method: "PATCH",
    body: JSON.stringify({ assigneeId }),
  });
}