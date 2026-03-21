import axios from "axios";
import type {
  ApiMessageResponse,
  EmployeeMenuOption,
  EmployeeCreatedResponse,
  EmployeeSummary,
  ImportEmployeesResponse,
  LoginRequest,
  LoginResponse,
  Menu,
  Order,
  Supplier,
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

export function getEmployeeMenus(token: string) {
  return api.get<EmployeeMenuOption[]>("/orders/menu", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getMyOrders(token: string) {
  return api.get<Order[]>("/orders/me", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function createOrder(token: string, payload: { menuId: number; orderDate: string }) {
  return api.post<Order>("/orders", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function updateOrder(token: string, orderId: number, payload: { menuId: number }) {
  return api.patch<Order>(`/orders/${orderId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function cancelOrder(token: string, orderId: number) {
  return api.delete<ApiMessageResponse>(`/orders/${orderId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getMenus(token: string, includeHistory: boolean) {
  return api.get<Menu[]>("/menus", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params: {
      include_history: includeHistory,
    },
  });
}

export function createMenu(
  token: string,
  payload: {
    supplierId: number;
    name: string;
    category: string;
    description: string;
    price: number;
    validFrom: string;
    validTo: string;
  },
) {
  return api.post<Menu>("/menus", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function updateMenu(
  token: string,
  menuId: number,
  payload: Partial<{
    supplierId: number;
    name: string;
    category: string;
    description: string;
    price: number;
    validFrom: string;
    validTo: string;
  }>,
) {
  return api.patch<Menu>(`/menus/${menuId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function createSupplier(
  token: string,
  payload: {
    name: string;
    email: string;
    phone: string;
    contactPerson: string;
    businessRegistrationNo: string;
  },
) {
  return api.post<Supplier>("/suppliers", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}
