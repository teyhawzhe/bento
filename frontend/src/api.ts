import axios from "axios";
import type {
  AdminOrder,
  ApiMessageResponse,
  EmployeeMenuCatalog,
  EmployeeCreatedResponse,
  EmployeeSummary,
  ErrorEmail,
  ImportEmployeesResponse,
  LoginRequest,
  LoginResponse,
  Menu,
  MonthlyBillingLog,
  MonthlyBillingTriggerResult,
  Order,
  ReportEmail,
  Supplier,
} from "./types";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api",
});

export async function login(payload: LoginRequest) {
  try {
    return await api.post<LoginResponse>("/auth/login", payload);
  } catch (unknownError) {
    if (
      axios.isAxiosError(unknownError) &&
      unknownError.response?.status === 403 &&
      unknownError.response?.data?.message === "請使用管理員登入入口"
    ) {
      return api.post<LoginResponse>("/admin/auth/login", payload);
    }
    throw unknownError;
  }
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
  return api.get<EmployeeMenuCatalog>("/orders/menu", {
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

export function getAdminOrders(
  token: string,
  params: Partial<{
    date_from: string;
    date_to: string;
    employee_id: number;
  }>,
) {
  return api.get<AdminOrder[]>("/admin/orders", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params,
  });
}

export function createAdminOrder(
  token: string,
  payload: { employeeId: number; menuId: number; orderDate: string },
) {
  return api.post<Order>("/admin/orders", payload, {
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

export function getSuppliers(
  token: string,
  params: Partial<{
    name: string;
    search_type: "exact" | "fuzzy";
  }>,
) {
  return api.get<Supplier[]>("/suppliers", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params,
  });
}

export function getSupplier(token: string, supplierId: number) {
  return api.get<Supplier>(`/suppliers/${supplierId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function updateSupplier(
  token: string,
  supplierId: number,
  payload: {
    name: string;
    email: string;
    phone: string;
    contactPerson: string;
    isActive: boolean;
  },
) {
  return api.patch<Supplier>(`/suppliers/${supplierId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getErrorEmails(token: string) {
  return api.get<ErrorEmail[]>("/settings/error-emails", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function createErrorEmail(token: string, payload: { email: string }) {
  return api.post<ErrorEmail>("/settings/error-emails", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function deleteErrorEmail(token: string, id: number) {
  return api.delete<ApiMessageResponse>(`/settings/error-emails/${id}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getReportEmails(token: string) {
  return api.get<ReportEmail[]>("/settings/report-emails", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function createReportEmail(token: string, payload: { email: string }) {
  return api.post<ReportEmail>("/settings/report-emails", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function deleteReportEmail(token: string, id: number) {
  return api.delete<ApiMessageResponse>(`/settings/report-emails/${id}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function triggerMonthlyBilling(token: string) {
  return api.post<MonthlyBillingTriggerResult>(
    "/admin/reports/monthly",
    {},
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );
}

export function getMonthlyBillingLogs(token: string) {
  return api.get<MonthlyBillingLog[]>("/admin/reports/monthly", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}
