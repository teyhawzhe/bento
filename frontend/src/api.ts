import axios from "axios";
import type { AxiosResponse, InternalAxiosRequestConfig } from "axios";
import type {
  CsvImportRow,
  CsvImportType,
  Department,
  EmployeeOrderReport,
  EmployeeOrderReportSort,
  EmployeeMenuOption,
  EmployeeSummary,
  ErrorEmail,
  LoginRequest,
  LoginResponse,
  Menu,
  MonthlyBillingLog,
  Order,
  ReportEmail,
  SessionUser,
  Supplier,
  WorkCalendarDay,
} from "./types";

interface ApiEnvelope<T> {
  status: string;
  data: T;
}

interface BackendLoginResponse {
  accessToken: string;
  refreshToken: string;
  role: "employee" | "admin";
  employeeId: number;
  username: string;
  name: string;
}

interface AuthConfig {
  getSession: () => SessionUser | null;
  onSessionUpdate: (session: SessionUser) => void;
  onUnauthorized: () => void;
}

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  _skipAuthRefresh?: boolean;
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
});

let authConfig: AuthConfig = {
  getSession: () => null,
  onSessionUpdate: () => {},
  onUnauthorized: () => {},
};
let refreshSessionPromise: Promise<SessionUser> | null = null;

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Object.prototype.toString.call(value) === "[object Object]";
}

function toSnakeCase(value: string) {
  return value.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toLowerCase();
}

function toCamelCase(value: string) {
  return value.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase());
}

function snakeCaseKeys<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map((item) => snakeCaseKeys(item)) as T;
  }

  if (value instanceof Date || value instanceof File || value instanceof Blob || value instanceof FormData) {
    return value;
  }

  if (!isPlainObject(value)) {
    return value;
  }

  return Object.fromEntries(
    Object.entries(value).map(([key, nestedValue]) => [toSnakeCase(key), snakeCaseKeys(nestedValue)]),
  ) as T;
}

function camelCaseKeys<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map((item) => camelCaseKeys(item)) as T;
  }

  if (value instanceof Date || value instanceof File || value instanceof Blob || value instanceof FormData) {
    return value;
  }

  if (!isPlainObject(value)) {
    return value;
  }

  return Object.fromEntries(
    Object.entries(value).map(([key, nestedValue]) => [toCamelCase(key), camelCaseKeys(nestedValue)]),
  ) as T;
}

async function unwrap<T>(request: Promise<AxiosResponse<ApiEnvelope<T>>>) {
  const response = await request;
  return {
    ...response,
    data: camelCaseKeys(response.data.data),
  } as AxiosResponse<T>;
}

function mapLoginResponse(response: AxiosResponse<BackendLoginResponse>) {
  return {
    ...response,
    data: {
      token: response.data.accessToken,
      refreshToken: response.data.refreshToken,
      role: response.data.role,
      employeeId: response.data.employeeId,
      username: response.data.username,
      name: response.data.name,
    },
  } as AxiosResponse<LoginResponse>;
}

function mapBackendLoginData(response: BackendLoginResponse): LoginResponse {
  return {
    token: response.accessToken,
    refreshToken: response.refreshToken,
    role: response.role,
    employeeId: response.employeeId,
    username: response.username,
    name: response.name,
  };
}

async function refreshSession(currentSession: SessionUser) {
  if (!refreshSessionPromise) {
    refreshSessionPromise = (async () => {
      const endpoint =
        currentSession.role === "admin" ? "/admin/auth/refresh" : "/auth/refresh";
      const response = await unwrap<BackendLoginResponse>(
        api.post(
          endpoint,
          { refreshToken: currentSession.refreshToken },
          {
            headers: {
              Authorization: `Bearer ${currentSession.token}`,
            },
            _skipAuthRefresh: true,
          } as RetriableRequestConfig,
        ),
      );
      const nextSession: SessionUser = {
        ...currentSession,
        ...mapBackendLoginData(response.data),
      };
      authConfig.onSessionUpdate(nextSession);
      return nextSession;
    })().finally(() => {
      refreshSessionPromise = null;
    });
  }

  return refreshSessionPromise;
}

export function configureAuth(config: AuthConfig) {
  authConfig = config;
}

api.interceptors.request.use((config) => {
  if (config.data && !(config.data instanceof FormData)) {
    config.data = snakeCaseKeys(config.data);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (unknownError) => {
    if (!axios.isAxiosError(unknownError)) {
      throw unknownError;
    }

    const requestConfig = unknownError.config as RetriableRequestConfig | undefined;
    if (!requestConfig || requestConfig._skipAuthRefresh || requestConfig._retry) {
      throw unknownError;
    }

    if (unknownError.response?.status !== 401) {
      throw unknownError;
    }

    const currentSession = authConfig.getSession();
    if (!currentSession?.refreshToken) {
      authConfig.onUnauthorized();
      throw unknownError;
    }

    try {
      const nextSession = await refreshSession(currentSession);
      requestConfig._retry = true;
      requestConfig.headers = requestConfig.headers ?? {};
      requestConfig.headers.Authorization = `Bearer ${nextSession.token}`;
      return api(requestConfig);
    } catch (refreshError) {
      authConfig.onUnauthorized();
      throw refreshError;
    }
  },
);

export async function login(payload: LoginRequest) {
  try {
    return mapLoginResponse(await unwrap<BackendLoginResponse>(api.post("/auth/login", payload)));
  } catch (unknownError) {
    if (
      axios.isAxiosError(unknownError) &&
      unknownError.response?.status === 403 &&
      unknownError.response?.data?.data?.message === "請使用管理員登入入口"
    ) {
      return mapLoginResponse(
        await unwrap<BackendLoginResponse>(api.post("/admin/auth/login", payload)),
      );
    }
    throw unknownError;
  }
}

export function forgotPassword(email: string) {
  return unwrap<void>(api.post("/auth/forgot-password", { email }));
}

export function logout(token: string) {
  return unwrap<void>(api.post(
    "/auth/logout",
    {},
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  ));
}

export function changePassword(token: string, oldPassword: string, newPassword: string) {
  return unwrap<void>(api.patch(
    "/auth/change-password",
    { oldPassword, newPassword },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  ));
}

export function changeAdminPassword(token: string, oldPassword: string, newPassword: string) {
  return unwrap<void>(api.patch(
    "/admin/auth/change-password",
    { oldPassword, newPassword },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  ));
}

export function getEmployees(token: string, departmentId?: number) {
  return unwrap<EmployeeSummary[]>(api.get("/admin/employees", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params: {
      department_id: departmentId,
    },
  }));
}

export function createEmployee(
  token: string,
  payload: { username: string; name: string; email: string; departmentId: number },
) {
  return unwrap<void>(api.post("/admin/employees", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function importEmployees(token: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return unwrap<void>(api.post("/admin/employees/import", formData, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export async function downloadImportTemplate(token: string, type: CsvImportType) {
  const response = await api.get<string>(`/admin/import/template/${type}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    responseType: "text",
  });
  return response.data;
}

export function importAdminCsv(token: string, type: CsvImportType, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return unwrap<CsvImportRow[]>(api.post(`/admin/import/${type}`, formData, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function updateEmployeeStatus(token: string, employeeId: number, isActive: boolean) {
  return unwrap<void>(api.patch(
    `/admin/employees/${employeeId}/status`,
    { is_active: isActive },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  ));
}

export function resetEmployeePassword(token: string, employeeId: number, newPassword: string) {
  return unwrap<void>(api.patch(
    `/admin/employees/${employeeId}/reset-password`,
    { newPassword },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  ));
}

export function getEmployeeMenus(token: string) {
  return unwrap<EmployeeMenuOption[]>(api.get("/orders/menu", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function getMyOrders(token: string) {
  return unwrap<Order[]>(api.get("/orders/me", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function createOrder(token: string, payload: { menuId: number; orderDate: string }) {
  return unwrap<Order>(api.post("/orders", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function updateOrder(token: string, orderId: number, payload: { menuId: number }) {
  return unwrap<Order>(api.patch(`/orders/${orderId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function cancelOrder(token: string, orderId: number) {
  return unwrap<void>(api.delete(`/orders/${orderId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function getAdminOrders(
  token: string,
  params: Partial<{
    date_from: string;
    date_to: string;
    employee_id: number;
  }>,
) {
  return unwrap<Order[]>(api.get("/admin/orders", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params,
  }));
}

export function createAdminOrder(
  token: string,
  payload: { employeeId: number; menuId: number; orderDate: string },
) {
  return unwrap<Order>(api.post("/admin/orders", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function cancelAdminOrder(token: string, orderId: number) {
  return unwrap<void>(api.delete(`/admin/orders/${orderId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function getMenus(token: string, supplierId?: number) {
  return unwrap<Menu[]>(api.get("/menus", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params: {
      supplier_id: supplierId,
    },
  }));
}

export function updateEmployee(
  token: string,
  employeeId: number,
  payload: {
    username: string;
    name: string;
    email: string;
    departmentId: number;
    isAdmin: boolean;
  },
) {
  return unwrap<EmployeeSummary>(api.patch(
    `/admin/employees/${employeeId}`,
    payload,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  ));
}

export function getDepartments(token: string) {
  return unwrap<Department[]>(api.get("/admin/departments", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function createDepartment(token: string, payload: { name: string }) {
  return unwrap<Department>(api.post("/admin/departments", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function updateDepartment(
  token: string,
  departmentId: number,
  payload: { name: string },
) {
  return unwrap<Department>(api.patch(`/admin/departments/${departmentId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
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
  return unwrap<Menu>(api.post("/menus", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
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
  return unwrap<Menu>(api.patch(`/menus/${menuId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
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
  return unwrap<Supplier>(api.post("/suppliers", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function getSuppliers(
  token: string,
  params: Partial<{
    name: string;
    search_type: "exact" | "fuzzy";
  }>,
) {
  return unwrap<Supplier[]>(api.get("/suppliers", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params,
  }));
}

export function getSupplier(token: string, supplierId: number) {
  return unwrap<Supplier>(api.get(`/suppliers/${supplierId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
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
  return unwrap<Supplier>(api.patch(`/suppliers/${supplierId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function getErrorEmails(token: string) {
  return unwrap<ErrorEmail[]>(api.get("/settings/error-emails", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function createErrorEmail(token: string, payload: { email: string }) {
  return unwrap<ErrorEmail>(api.post("/settings/error-emails", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function deleteErrorEmail(token: string, id: number) {
  return unwrap<void>(api.delete(`/settings/error-emails/${id}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function getReportEmails(token: string) {
  return unwrap<ReportEmail[]>(api.get("/settings/report-emails", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function createReportEmail(token: string, payload: { email: string }) {
  return unwrap<ReportEmail>(api.post("/settings/report-emails", payload, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function deleteReportEmail(token: string, id: number) {
  return unwrap<void>(api.delete(`/settings/report-emails/${id}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function triggerMonthlyBilling(token: string) {
  return unwrap<void>(api.post(
    "/admin/reports/monthly",
    {},
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  ));
}

export function getMonthlyBillingLogs(token: string) {
  return unwrap<MonthlyBillingLog[]>(api.get("/admin/reports/monthly", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function getEmployeeOrderReports(
  token: string,
  params: { date_from: string; date_to: string; sort_by?: EmployeeOrderReportSort },
) {
  return unwrap<EmployeeOrderReport[]>(api.get("/admin/reports/orders", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params,
  }));
}

export async function downloadEmployeeOrderReportPdf(
  token: string,
  params: { date_from: string; date_to: string; sort_by?: EmployeeOrderReportSort },
) {
  const response = await api.get<Blob>("/admin/reports/orders/pdf", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params,
    responseType: "blob",
  });
  return response.data;
}

export function getWorkCalendar(token: string, year: number, month: number) {
  return unwrap<WorkCalendarDay[]>(api.get("/admin/calendar", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params: {
      year,
      month,
    },
  }));
}

export function updateWorkCalendar(token: string, days: WorkCalendarDay[]) {
  return unwrap<void>(api.put("/admin/calendar", days, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function generateWorkCalendar(token: string, year: number) {
  return unwrap<void>(api.post("/admin/calendar/generate", { year }, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}

export function importWorkCalendar(token: string, file: File, confirm: boolean) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("confirm", String(confirm));
  return unwrap<WorkCalendarDay[]>(api.post("/admin/calendar/import", formData, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }));
}
