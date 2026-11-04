import { apiFetch } from "./client";
import type { PageResponse } from "../types";

export interface NotificationItem {
  id: number;
  userId: number;
  type: string;
  title: string;
  message: string;
  entityId: string | null;
  payload: string | null;
  readAt: string | null;
  createdAt: string;
  read: boolean;
}
export interface BatchReadResponse {
  updatedCount: number;
}
export interface NotificationListParams {
  unreadOnly?: boolean;
  page?: number;
  size?: number;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export function listNotifications(p: NotificationListParams = {}) {
  const q = new URLSearchParams();

  if (p.unreadOnly !== undefined) {
    q.set("unreadOnly", String(p.unreadOnly));
  }

  q.set("page", String(p.page ?? 0));
  q.set("size", String(p.size ?? 20));

  return apiFetch<PageResponse<NotificationItem>>(
    `/api/notifications?${q.toString()}`
  );
}

export function getUnreadCount() {
  return apiFetch<UnreadCountResponse>(
    "/api/notifications/unread-count"
  );
}

export function markNotificationRead(id: number) {
  return apiFetch<NotificationItem>(
    `/api/notifications/${id}/read`,
    { method: "PATCH" }
  );
}

export function markAllNotificationsRead() {
  return apiFetch<BatchReadResponse>(
    "/api/notifications/read-all",
    { method: "PATCH" }
  );
}
