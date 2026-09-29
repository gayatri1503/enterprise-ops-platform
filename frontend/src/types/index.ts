export type Role = "ADMIN" | "MANAGER" | "USER" | "AUDITOR";

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  departmentId: number;
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: UserSummary;
}

export type WorkStatus = "NEW" | "IN_PROGRESS" | "BLOCKED" | "COMPLETED";
export type Priority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface WorkItem {
  id: number;
  title: string;
  description: string | null;
  status: WorkStatus;
  priority: Priority;
  creatorId: number;
  assigneeId: number | null;
  departmentId: number;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}