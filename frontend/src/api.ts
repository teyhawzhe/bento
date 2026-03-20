import axios from "axios";
import type {
  ApiMessageResponse,
  EmployeeCreatedResponse,
  EmployeeSummary,
  ImportEmployeesResponse,
  LoginRequest,
  LoginResponse,
} from "./types";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api",
});

export function login(role: "employee" | "admin", payload: LoginRequest) {
  const path = role === "admin" ? "/admin/auth/login" : "/auth/login";
  return api.post<LoginResponse>(path, payload);
}

export function forgotPassword(email: string) {
  return api.post<ApiMessageResponse>("/auth/forgot-password", { email });
}

export function logout(token: string) {
  return api.post<ApiMessageResponse>(
    "/auth/logout",
    {},
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );
}

export function changePassword(token: string, oldPassword: string, newPassword: string) {
  return api.patch<ApiMessageResponse>(
    "/auth/change-password",
    { oldPassword, newPassword },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );
}

export function getEmployees(token: string) {
  return api.get<EmployeeSummary[]>("/admin/employees", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function createEmployee(
  token: string,
  payload: { username: string; name: string; email: string },
) {
  return api.post<EmployeeCreatedResponse>("/admin/employees", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function importEmployees(token: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return api.post<ImportEmployeesResponse>("/admin/employees/import", formData, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function updateEmployeeStatus(token: string, employeeId: number, isActive: boolean) {
  return api.patch<EmployeeSummary>(
    `/admin/employees/${employeeId}/status`,
    { is_active: isActive },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );
}

export function resetEmployeePassword(token: string, employeeId: number, newPassword: string) {
  return api.patch<EmployeeSummary>(
    `/admin/employees/${employeeId}/reset-password`,
    { newPassword },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );
}
