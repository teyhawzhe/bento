import { useEffect, useState, type ReactNode } from "react";
import axios from "axios";
import { buildAdminOrders, employeeMenusForDate, employeeOrderableDatesFor } from "./app-utils";
import {
  cancelAdminOrder,
  cancelOrder,
  changeAdminPassword,
  changePassword,
  configureAuth,
  createDepartment,
  createAdminOrder,
  createEmployee,
  createErrorEmail,
  createReportEmail,
  createMenu,
  createOrder,
  createSupplier,
  deleteErrorEmail,
  deleteReportEmail,
  downloadEmployeeOrderReportPdf,
  downloadImportTemplate,
  forgotPassword,
  getEmployeeOrderReports,
  getAdminOrders,
  getDepartments,
  getEmployeeMenus,
  getEmployees,
  getErrorEmails,
  getReportEmails,
  getMenus,
  getMonthlyBillingLogs,
  getMyOrders,
  getSupplier,
  getSuppliers,
  importAdminCsv,
  login,
  logout as logoutRequest,
  updateEmployee,
  resetEmployeePassword,
  triggerMonthlyBilling,
  updateDepartment,
  updateSupplier,
  updateEmployeeStatus,
  updateMenu,
  updateOrder,
} from "./api";
import type {
  AdminOrder,
  CsvImportErrorData,
  CsvImportRow,
  CsvImportType,
  Department,
  EmployeeOrderReport,
  EmployeeOrderReportSort,
  EmployeeMenuOption,
  EmployeeSummary,
  ErrorEmail,
  Menu,
  MonthlyBillingLog,
  Order,
  ReportEmail,
  SessionUser,
  Supplier,
  UserRole,
} from "./types";

const SESSION_KEY = "bento-session";
const CATEGORY_OPTIONS = ["肉類", "海鮮", "素食"];
const EMPLOYEE_TABS = [
  { id: "employee-ordering", label: "訂便當" },
  { id: "employee-orders", label: "我的訂單" },
  { id: "employee-change-password", label: "修改密碼" },
] as const;
const ADMIN_TABS = [
  { id: "admin-orders", label: "訂單管理" },
  { id: "admin-suppliers", label: "供應商管理" },
  { id: "admin-reports", label: "報表設定" },
  { id: "admin-employees", label: "員工管理" },
  { id: "admin-departments", label: "部門管理" },
  { id: "admin-import", label: "CSV 匯入" },
  { id: "admin-settings", label: "系統設定" },
] as const;
const ADMIN_REPORT_TABS = [
  { id: "employee-order-report", label: "員工訂餐報表" },
  { id: "monthly-billing", label: "月結報表" },
] as const;
const ADMIN_SUPPLIER_TABS = [
  { id: "supplier-directory", label: "供應商管理" },
  { id: "supplier-menus", label: "建立菜單" },
] as const;
const CSV_IMPORT_CARDS: Array<{
  type: CsvImportType;
  title: string;
  header: string;
  description: string;
  columns: Array<{ key: string; label: string }>;
}> = [
  {
    type: "employees",
    title: "員工 CSV",
    header: "username,name,email,department_id",
    description: "驗證 username、email 與 department_id，匯入後員工固定為一般啟用帳號。",
    columns: [
      { key: "id", label: "ID" },
      { key: "username", label: "帳號" },
      { key: "name", label: "姓名" },
      { key: "email", label: "Email" },
      { key: "departmentId", label: "部門 ID" },
      { key: "isAdmin", label: "管理員" },
      { key: "isActive", label: "啟用" },
    ],
  },
  {
    type: "departments",
    title: "部門 CSV",
    header: "name",
    description: "驗證部門名稱必填且唯一，成功後回傳本次建立的部門清單。",
    columns: [
      { key: "id", label: "ID" },
      { key: "name", label: "部門名稱" },
    ],
  },
  {
    type: "suppliers",
    title: "供應商 CSV",
    header: "name,email,phone,contact_person,business_registration_no",
    description: "驗證統編唯一性，並回傳本次建立的供應商清單。",
    columns: [
      { key: "id", label: "ID" },
      { key: "name", label: "名稱" },
      { key: "email", label: "Email" },
      { key: "phone", label: "電話" },
      { key: "contactPerson", label: "聯絡人" },
      { key: "businessRegistrationNo", label: "統編" },
      { key: "isActive", label: "啟用" },
    ],
  },
  {
    type: "menus",
    title: "菜單 CSV",
    header: "supplier_id,name,category,description,price,valid_from,valid_to",
    description: "驗證供應商存在、名稱唯一性與日期區間，成功後回傳本次建立的菜單清單。",
    columns: [
      { key: "id", label: "ID" },
      { key: "supplierId", label: "供應商 ID" },
      { key: "name", label: "名稱" },
      { key: "category", label: "分類" },
      { key: "description", label: "描述" },
      { key: "price", label: "價格" },
      { key: "validFrom", label: "起日" },
      { key: "validTo", label: "迄日" },
    ],
  },
] as const;

type EmployeeTabId = (typeof EMPLOYEE_TABS)[number]["id"];
type AdminTabId = (typeof ADMIN_TABS)[number]["id"];
type AdminSupplierTabId = (typeof ADMIN_SUPPLIER_TABS)[number]["id"];
type AdminReportTabId = (typeof ADMIN_REPORT_TABS)[number]["id"];
type AppTabId = EmployeeTabId | AdminTabId;
type MessageBoxVariant = "success" | "error" | "warning" | "confirm";

interface TabDefinition<T extends string = string> {
  id: T;
  label: string;
}

interface MessageBoxState {
  isOpen: boolean;
  variant: MessageBoxVariant;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm?: (() => void | Promise<void>) | null;
  onCancel?: (() => void) | null;
}

interface EmployeeEditForm {
  username: string;
  name: string;
  email: string;
  departmentId: string;
  isAdmin: boolean;
}

function readSession(): SessionUser | null {
  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as SessionUser;
  } catch {
    return null;
  }
}

function persistSession(session: SessionUser | null) {
  if (!session) {
    window.localStorage.removeItem(SESSION_KEY);
    return;
  }
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value));
}

function formatDateWithWeekday(value: string) {
  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(new Date(value));
}

function formatCurrentTime(value: Date) {
  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(value);
}

function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function tomorrowDate() {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return toDateInputValue(tomorrow);
}

function todayDate() {
  return toDateInputValue(new Date());
}

function nextWeekdays() {
  const days: string[] = [];
  const now = new Date();
  const day = now.getDay();
  const nextMondayOffset = day === 0 ? 1 : 8 - day;
  const nextMonday = new Date(now);
  nextMonday.setDate(now.getDate() + nextMondayOffset);

  for (let index = 0; index < 5; index += 1) {
    const nextDate = new Date(nextMonday);
    nextDate.setDate(nextMonday.getDate() + index);
    days.push(nextDate.toISOString().slice(0, 10));
  }

  return days;
}

function employeeOrderCancellationDeadline(orderDate: string) {
  const cutoff = new Date(`${orderDate}T16:30:00`);
  cutoff.setDate(cutoff.getDate() - 1);
  return cutoff;
}

function adminOrderCancellationDeadline(orderDate: string) {
  const cutoff = new Date(`${orderDate}T16:30:00`);
  cutoff.setDate(cutoff.getDate() - 1);
  return cutoff;
}

function canCancelEmployeeOrder(orderDate: string, currentTime: Date) {
  return currentTime < employeeOrderCancellationDeadline(orderDate);
}

function canCancelAdminOrder(orderDate: string, currentTime: Date) {
  return currentTime < adminOrderCancellationDeadline(orderDate);
}

function defaultTabForRole(role: UserRole): AppTabId {
  return role === "admin" ? "admin-orders" : "employee-ordering";
}

function tabsForRole(role: UserRole): TabDefinition<AppTabId>[] {
  return role === "admin" ? [...ADMIN_TABS] : [...EMPLOYEE_TABS];
}

function isBlank(value: string) {
  return value.trim().length === 0;
}

function PanelCard({ children }: { children: ReactNode }) {
  return (
    <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
      {children}
    </article>
  );
}

function TabBar<T extends string>({
  tabs,
  activeTab,
  onChange,
}: {
  tabs: TabDefinition<T>[];
  activeTab: T;
  onChange: (tabId: T) => void;
}) {
  return (
    <div className="flex flex-wrap gap-3">
      {tabs.map((tab) => {
        const isActive = tab.id === activeTab;
        return (
          <button
            key={tab.id}
            className={`rounded-full px-5 py-2 text-sm font-medium transition ${
              isActive
                ? "bg-ink text-white shadow-sm"
                : "border border-ink/10 bg-white text-ink/70 hover:border-pine hover:text-pine"
            }`}
            onClick={() => onChange(tab.id)}
            type="button"
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}

function MessageBox({
  state,
  busy,
  onClose,
  onConfirm,
}: {
  state: MessageBoxState;
  busy: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}) {
  if (!state.isOpen) {
    return null;
  }

  const toneClasses: Record<MessageBoxVariant, string> = {
    success: "border-emerald-200 bg-emerald-50 text-emerald-900",
    error: "border-red-200 bg-red-50 text-red-900",
    warning: "border-amber-200 bg-amber-50 text-amber-900",
    confirm: "border-ink/10 bg-white text-ink",
  };
  const eyebrow: Record<MessageBoxVariant, string> = {
    success: "Success",
    error: "Error",
    warning: "Warning",
    confirm: "Confirm",
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/35 px-4 py-6 backdrop-blur-sm">
      <div className={`w-full max-w-md rounded-[1.75rem] border p-6 shadow-2xl ${toneClasses[state.variant]}`}>
        <p className="text-xs uppercase tracking-[0.35em] opacity-70">{eyebrow[state.variant]}</p>
        <h2 className="mt-3 text-2xl font-semibold">{state.title}</h2>
        <p className="mt-3 whitespace-pre-line text-sm leading-7 opacity-90">{state.message}</p>
        <div className="mt-6 flex flex-wrap justify-end gap-3">
          {state.variant === "confirm" ? (
            <button
              className="rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink disabled:cursor-not-allowed disabled:opacity-60"
              onClick={onClose}
              type="button"
              disabled={busy}
            >
              {state.cancelLabel ?? "取消"}
            </button>
          ) : null}
          <button
            className={`rounded-full px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60 ${
              state.variant === "error"
                ? "bg-red-700"
                : state.variant === "warning"
                  ? "bg-amber-700"
                  : state.variant === "confirm"
                    ? "bg-ink"
                    : "bg-emerald-700"
            }`}
            onClick={() => void onConfirm()}
            type="button"
            disabled={busy}
          >
            {busy ? "處理中..." : state.confirmLabel ?? "確認"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [session, setSession] = useState<SessionUser | null>(readSession);
  const [activeTab, setActiveTab] = useState<AppTabId>(() => defaultTabForRole("employee"));
  const [adminSupplierTab, setAdminSupplierTab] = useState<AdminSupplierTabId>("supplier-directory");
  const [adminReportTab, setAdminReportTab] = useState<AdminReportTabId>("employee-order-report");
  const [currentTime, setCurrentTime] = useState(() => new Date());
  const [messageBox, setMessageBox] = useState<MessageBoxState>({
    isOpen: false,
    variant: "success",
    title: "",
    message: "",
  });
  const [messageBoxBusy, setMessageBoxBusy] = useState(false);
  const [loading, setLoading] = useState(false);

  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [employeeMenus, setEmployeeMenus] = useState<EmployeeMenuOption[]>([]);
  const [employeeOrderDates, setEmployeeOrderDates] = useState<string[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [menus, setMenus] = useState<Menu[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [employeeDepartmentFilter, setEmployeeDepartmentFilter] = useState("");
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [supplierOptions, setSupplierOptions] = useState<Supplier[]>([]);
  const [supplierMenus, setSupplierMenus] = useState<Menu[]>([]);
  const [errorEmails, setErrorEmails] = useState<ErrorEmail[]>([]);
  const [reportEmails, setReportEmails] = useState<ReportEmail[]>([]);
  const [adminOrders, setAdminOrders] = useState<AdminOrder[]>([]);
  const [monthlyBillingLogs, setMonthlyBillingLogs] = useState<MonthlyBillingLog[]>([]);
  const [employeeOrderReports, setEmployeeOrderReports] = useState<EmployeeOrderReport[]>([]);
  const [hasLoadedEmployeeOrderReports, setHasLoadedEmployeeOrderReports] = useState(false);
  const [csvImportFiles, setCsvImportFiles] = useState<Partial<Record<CsvImportType, File | null>>>({});
  const [csvImportResults, setCsvImportResults] = useState<Partial<Record<CsvImportType, CsvImportRow[]>>>({});
  const [csvImportErrors, setCsvImportErrors] = useState<Partial<Record<CsvImportType, CsvImportErrorData | null>>>({});
  const [deadlineMessage, setDeadlineMessage] = useState("");

  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [forgotEmail, setForgotEmail] = useState("");
  const [employeeChangeForm, setEmployeeChangeForm] = useState({ oldPassword: "", newPassword: "" });
  const [adminChangeForm, setAdminChangeForm] = useState({ oldPassword: "", newPassword: "" });
  const [createForm, setCreateForm] = useState({
    username: "",
    name: "",
    email: "",
    departmentId: "",
  });
  const [departmentForm, setDepartmentForm] = useState({ name: "" });
  const [editingDepartments, setEditingDepartments] = useState<Record<number, { name: string }>>({});
  const [editingEmployeeId, setEditingEmployeeId] = useState<number | null>(null);
  const [editingEmployees, setEditingEmployees] = useState<Record<number, EmployeeEditForm>>({});
  const [resetForms, setResetForms] = useState<Record<number, string>>({});
  const [orderForm, setOrderForm] = useState({
    orderDate: "",
    menuId: "",
  });
  const [supplierForm, setSupplierForm] = useState({
    name: "",
    email: "",
    phone: "",
    contactPerson: "",
    businessRegistrationNo: "",
  });
  const [errorEmailForm, setErrorEmailForm] = useState({ email: "" });
  const [reportEmailForm, setReportEmailForm] = useState({ email: "" });
  const [employeeOrderReportFilters, setEmployeeOrderReportFilters] = useState<{
    dateFrom: string;
    dateTo: string;
    sortBy: EmployeeOrderReportSort;
  }>({
    dateFrom: todayDate(),
    dateTo: todayDate(),
    sortBy: "date",
  });
  const [menuForm, setMenuForm] = useState({
    name: "",
    category: CATEGORY_OPTIONS[0],
    description: "",
    price: "",
    validFrom: nextWeekdays()[0] ?? "",
    validTo: nextWeekdays()[4] ?? "",
  });
  const [selectedMenuSupplierId, setSelectedMenuSupplierId] = useState("");
  const [editingMenus, setEditingMenus] = useState<Record<number, Partial<Menu>>>({});
  const [adminOrderFilters, setAdminOrderFilters] = useState({
    dateFrom: todayDate(),
    dateTo: todayDate(),
    employeeId: "",
  });
  const [adminOrderForm, setAdminOrderForm] = useState({
    employeeId: "",
    orderDate: tomorrowDate(),
    menuId: "",
  });
  const [supplierFilters, setSupplierFilters] = useState({
    name: "",
    searchType: "exact" as "exact" | "fuzzy",
  });
  const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null);
  const [supplierDetailForm, setSupplierDetailForm] = useState({
    id: "",
    name: "",
    email: "",
    phone: "",
    contactPerson: "",
    businessRegistrationNo: "",
    isActive: true,
  });

  function resetAppState() {
    setEmployees([]);
    setEmployeeMenus([]);
    setOrders([]);
    setEmployeeOrderDates([]);
    setMenus([]);
    setDepartments([]);
    setEmployeeDepartmentFilter("");
    setSuppliers([]);
    setSupplierOptions([]);
    setSupplierMenus([]);
    setErrorEmails([]);
    setReportEmails([]);
    setAdminOrders([]);
    setMonthlyBillingLogs([]);
    setEmployeeOrderReports([]);
    setHasLoadedEmployeeOrderReports(false);
    setSelectedSupplier(null);
    setDeadlineMessage("");
    setCsvImportFiles({});
    setCsvImportResults({});
    setCsvImportErrors({});
    setActiveTab(defaultTabForRole("employee"));
  }

  function applySession(nextSession: SessionUser | null) {
    setSession(nextSession);
    persistSession(nextSession);
  }

  useEffect(() => {
    configureAuth({
      getSession: readSession,
      onSessionUpdate: (nextSession) => {
        applySession(nextSession);
      },
      onUnauthorized: () => {
        applySession(null);
        resetAppState();
        openErrorBox("登入已失效，請重新登入。", "登入逾時");
      },
    });
  }, []);

  useEffect(() => {
    if (!session) {
      return;
    }

    if (session.role === "employee") {
      void loadEmployeeData(session.token);
      return;
    }

    void loadAdminData(session.token, adminOrderFilters, supplierFilters, employeeDepartmentFilter);
  }, [adminOrderFilters, employeeDepartmentFilter, session, supplierFilters]);

  useEffect(() => {
    if (session?.role !== "admin") {
      setSupplierMenus([]);
      return;
    }

    void loadSupplierMenus(session.token, selectedMenuSupplierId);
  }, [selectedMenuSupplierId, session]);

  useEffect(() => {
    if (!selectedMenuSupplierId) {
      return;
    }

    const supplierStillExists = supplierOptions.some(
      (supplier) => String(supplier.id) === selectedMenuSupplierId,
    );
    if (!supplierStillExists) {
      setSelectedMenuSupplierId("");
    }
  }, [selectedMenuSupplierId, supplierOptions]);

  useEffect(() => {
    if (session?.role !== "admin" || !departments.length) {
      return;
    }

    if (!departments.length) {
      if (createForm.departmentId) {
        setCreateForm((current) => ({ ...current, departmentId: "" }));
      }
      return;
    }

    const selectedStillValid = departments.some(
      (department) => String(department.id) === createForm.departmentId,
    );
    if (!selectedStillValid) {
      setCreateForm((current) => ({
        ...current,
        departmentId: String(departments[0].id),
      }));
    }
  }, [createForm.departmentId, departments, session]);

  useEffect(() => {
    if (session?.role !== "admin") {
      if (employeeDepartmentFilter) {
        setEmployeeDepartmentFilter("");
      }
      return;
    }

    if (!employeeDepartmentFilter) {
      return;
    }

    const selectedStillValid = departments.some(
      (department) => String(department.id) === employeeDepartmentFilter,
    );
    if (!selectedStillValid) {
      setEmployeeDepartmentFilter("");
    }
  }, [departments, employeeDepartmentFilter, session]);

  useEffect(() => {
    setEditingDepartments((current) => {
      const next: Record<number, { name: string }> = {};
      for (const department of departments) {
        next[department.id] = current[department.id] ?? {
          name: department.name,
        };
      }
      return next;
    });
  }, [departments]);

  useEffect(() => {
    setEditingEmployees((current) => {
      const next: Record<number, EmployeeEditForm> = {};
      for (const employee of employees) {
        next[employee.id] = current[employee.id] ?? {
          username: employee.username,
          name: employee.name,
          email: employee.email,
          departmentId: String(employee.department.id),
          isAdmin: employee.isAdmin,
        };
      }
      return next;
    });
  }, [employees]);

  useEffect(() => {
    if (session?.role !== "admin") {
      return;
    }

    const availableMenus = menus.filter(
      (menu) =>
        menu.validFrom <= adminOrderForm.orderDate && adminOrderForm.orderDate <= menu.validTo,
    );
    const selectedMenuExists = availableMenus.some(
      (menu) => String(menu.id) === adminOrderForm.menuId,
    );

    if (!selectedMenuExists) {
      setAdminOrderForm((current) => ({
        ...current,
        menuId: availableMenus[0] ? String(availableMenus[0].id) : "",
      }));
    }
  }, [adminOrderForm.menuId, adminOrderForm.orderDate, menus, session]);

  useEffect(() => {
    if (session?.role !== "admin" || adminOrderForm.employeeId) {
      return;
    }

    const defaultEmployee = employees.find((employee) => !employee.isAdmin) ?? employees[0];
    if (defaultEmployee) {
      setAdminOrderForm((current) => ({
        ...current,
        employeeId: String(defaultEmployee.id),
      }));
    }
  }, [adminOrderForm.employeeId, employees, session]);

  useEffect(() => {
    if (session?.role !== "employee") {
      return;
    }

    setOrderForm((current) => {
      const nextOrderDate = employeeOrderDates.includes(current.orderDate)
        ? current.orderDate
        : (employeeOrderDates[0] ?? "");
      const availableMenus = employeeMenusForDate(employeeMenus, nextOrderDate);
      const existingOrder = orders.find((entry) => entry.orderDate === nextOrderDate);
      const preferredMenuId = existingOrder
        ? String(existingOrder.menuId)
        : availableMenus[0]
          ? String(availableMenus[0].id)
          : "";
      const nextMenuId = availableMenus.some((menu) => String(menu.id) === current.menuId)
        ? current.menuId
        : preferredMenuId;

      if (current.orderDate === nextOrderDate && current.menuId === nextMenuId) {
        return current;
      }

      return {
        orderDate: nextOrderDate,
        menuId: nextMenuId,
      };
    });
  }, [employeeMenus, employeeOrderDates, orders, session]);

  useEffect(() => {
    setActiveTab(defaultTabForRole(session?.role ?? "employee"));
    setAdminSupplierTab("supplier-directory");
  }, [session]);

  useEffect(() => {
    if (!session) {
      return;
    }

    const timer = window.setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);

    return () => {
      window.clearInterval(timer);
    };
  }, [session]);

  function closeMessageBox() {
    setMessageBoxBusy(false);
    setMessageBox({
      isOpen: false,
      variant: "success",
      title: "",
      message: "",
    });
  }

  function openMessageBox(nextState: Omit<MessageBoxState, "isOpen">) {
    setMessageBoxBusy(false);
    setMessageBox({
      isOpen: true,
      ...nextState,
    });
  }

  function openSuccessBox(message: string, title = "操作成功") {
    openMessageBox({
      variant: "success",
      title,
      message,
    });
  }

  function openErrorBox(message: string, title = "操作失敗") {
    openMessageBox({
      variant: "error",
      title,
      message,
    });
  }

  function openWarningBox(message: string, title = "請先確認資料") {
    openMessageBox({
      variant: "warning",
      title,
      message,
    });
  }

  function openConfirmBox(
    message: string,
    onConfirm: () => void | Promise<void>,
    title = "請確認",
    confirmLabel = "確認",
    cancelLabel = "取消",
  ) {
    openMessageBox({
      variant: "confirm",
      title,
      message,
      confirmLabel,
      cancelLabel,
      onConfirm,
      onCancel: closeMessageBox,
    });
  }

  async function handleMessageBoxConfirm() {
    if (messageBox.variant === "confirm" && messageBox.onConfirm) {
      try {
        setMessageBoxBusy(true);
        await messageBox.onConfirm();
      } finally {
        setMessageBoxBusy(false);
      }
      return;
    }

    closeMessageBox();
  }

  function validateWarning(message: string, checks: boolean[]) {
    if (checks.every(Boolean)) {
      return true;
    }
    openWarningBox(message);
    return false;
  }

  async function loadEmployeeData(token: string) {
    let nextOrders: Order[] = [];
    try {
      const [ordersResponse] = await Promise.all([getMyOrders(token)]);
      nextOrders = ordersResponse.data;
      setOrders(nextOrders);
    } catch (unknownError) {
      handleHttpError(unknownError, "訂餐記錄讀取失敗");
    }

    try {
      const response = await getEmployeeMenus(token);
      const nextMenus = response.data;
      const nextOrderableDates = employeeOrderableDatesFor(nextMenus, new Date());
      setEmployeeMenus(nextMenus);
      setEmployeeOrderDates(nextOrderableDates);
      setDeadlineMessage(
        nextOrderableDates.length
          ? ""
          : "目前沒有可訂日期。員工可訂本次截止日後一天起到下一個星期五、且仍在菜單有效期間內的便當。",
      );
      setOrderForm((current) => {
        const nextOrderDate = nextOrderableDates.includes(current.orderDate)
          ? current.orderDate
          : (nextOrderableDates[0] ?? "");
        const availableMenus = employeeMenusForDate(nextMenus, nextOrderDate);
        const existingOrder = nextOrders.find((entry) => entry.orderDate === nextOrderDate);
        const preferredMenuId = existingOrder
          ? String(existingOrder.menuId)
          : availableMenus[0]
            ? String(availableMenus[0].id)
            : "";
        const nextMenuId = availableMenus.some((menu) => String(menu.id) === current.menuId)
          ? current.menuId
          : preferredMenuId;
        return {
          orderDate: nextOrderDate,
          menuId: nextMenuId,
        };
      });
    } catch (unknownError) {
      setEmployeeMenus([]);
      setEmployeeOrderDates([]);
      if (axios.isAxiosError(unknownError)) {
        setDeadlineMessage(unknownError.response?.data?.message ?? "可訂日期讀取失敗");
      } else {
        setDeadlineMessage("可訂日期讀取失敗");
      }
    }
  }

  async function loadAdminData(
    token: string,
    filters: { dateFrom: string; dateTo: string; employeeId: string },
    supplierQuery: { name: string; searchType: "exact" | "fuzzy" },
    employeeDepartmentId = "",
  ) {
    try {
      const [
        employeesResponse,
        departmentsResponse,
        menusResponse,
        errorEmailsResponse,
        reportEmailsResponse,
        monthlyBillingLogsResponse,
        adminOrdersResponse,
        suppliersResponse,
        supplierOptionsResponse,
      ] = await Promise.all([
        getEmployees(token, employeeDepartmentId ? Number(employeeDepartmentId) : undefined),
        getDepartments(token),
        getMenus(token),
        getErrorEmails(token),
        getReportEmails(token),
        getMonthlyBillingLogs(token),
        getAdminOrders(token, {
          date_from: filters.dateFrom || undefined,
          date_to: filters.dateTo || undefined,
          employee_id: filters.employeeId ? Number(filters.employeeId) : undefined,
        }),
        getSuppliers(token, {
          name: supplierQuery.name || undefined,
          search_type: supplierQuery.searchType,
        }),
        getSuppliers(token, {}),
      ]);
      setEmployees(employeesResponse.data);
      setDepartments(departmentsResponse.data);
      setMenus(menusResponse.data);
      setErrorEmails(errorEmailsResponse.data);
      setReportEmails(reportEmailsResponse.data);
      setMonthlyBillingLogs(monthlyBillingLogsResponse.data);
      setSuppliers(suppliersResponse.data);
      setSupplierOptions(supplierOptionsResponse.data);
      setAdminOrders(
        buildAdminOrders(
          adminOrdersResponse.data,
          employeesResponse.data,
          menusResponse.data,
          supplierOptionsResponse.data,
        ),
      );
    } catch (unknownError) {
      handleHttpError(unknownError, "管理資料讀取失敗");
    }
  }

  function handleHttpError(unknownError: unknown, fallbackMessage: string) {
    if (axios.isAxiosError(unknownError)) {
      openErrorBox(unknownError.response?.data?.data?.message ?? fallbackMessage);
      return;
    }
    openErrorBox(fallbackMessage);
  }

  async function loadSupplierMenus(token: string, supplierId: string) {
    if (!supplierId) {
      setSupplierMenus([]);
      return;
    }

    try {
      const response = await getMenus(token, Number(supplierId));
      setSupplierMenus(response.data);
    } catch (unknownError) {
      setSupplierMenus([]);
      handleHttpError(unknownError, "菜單清單讀取失敗");
    }
  }

  async function submitLogin() {
    if (!validateWarning("請輸入帳號與密碼後再登入。", [!isBlank(loginForm.username), !isBlank(loginForm.password)])) {
      return;
    }
    setLoading(true);
    try {
      const response = await login(loginForm);
      const nextSession: SessionUser = response.data;
      applySession(nextSession);
      setActiveTab(defaultTabForRole(nextSession.role));
      if (nextSession.role === "employee") {
        await loadEmployeeData(nextSession.token);
      } else {
        await loadAdminData(nextSession.token, adminOrderFilters, supplierFilters);
      }
      openSuccessBox(
        nextSession.role === "admin"
          ? "管理員登入成功，已進入訂單管理頁。"
          : "登入成功，已進入訂便當頁。",
        "歡迎回來",
      );
    } catch (unknownError) {
      handleHttpError(unknownError, "登入失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitForgotPassword() {
    if (!validateWarning("請輸入 Email 後再寄送臨時密碼。", [!isBlank(forgotEmail)])) {
      return;
    }
    setLoading(true);
    try {
      await forgotPassword(forgotEmail);
      setForgotEmail("");
      openSuccessBox("臨時密碼已寄送至信箱。", "寄送成功");
    } catch (unknownError) {
      handleHttpError(unknownError, "寄送臨時密碼失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitEmployeeChangePassword() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請完整輸入舊密碼與新密碼。", [
        !isBlank(employeeChangeForm.oldPassword),
        !isBlank(employeeChangeForm.newPassword),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      await changePassword(
        session.token,
        employeeChangeForm.oldPassword,
        employeeChangeForm.newPassword,
      );
      setEmployeeChangeForm({ oldPassword: "", newPassword: "" });
      applySession(null);
      resetAppState();
      openSuccessBox("密碼修改成功，請使用新密碼重新登入。", "密碼已更新");
    } catch (unknownError) {
      handleHttpError(unknownError, "修改密碼失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitAdminChangePassword() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請完整輸入舊密碼與新密碼。", [
        !isBlank(adminChangeForm.oldPassword),
        !isBlank(adminChangeForm.newPassword),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      await changeAdminPassword(
        session.token,
        adminChangeForm.oldPassword,
        adminChangeForm.newPassword,
      );
      setAdminChangeForm({ oldPassword: "", newPassword: "" });
      applySession(null);
      resetAppState();
      openSuccessBox("密碼修改成功，請使用新密碼重新登入。", "密碼已更新");
    } catch (unknownError) {
      handleHttpError(unknownError, "修改密碼失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateEmployee() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請完整填寫員工帳號、姓名、Email 與部門。", [
        !isBlank(createForm.username),
        !isBlank(createForm.name),
        !isBlank(createForm.email),
        !isBlank(createForm.departmentId),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      await createEmployee(session.token, {
        username: createForm.username,
        name: createForm.name,
        email: createForm.email,
        departmentId: Number(createForm.departmentId),
      });
      setCreateForm((current) => ({
        username: "",
        name: "",
        email: "",
        departmentId: current.departmentId,
      }));
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("員工帳號已建立，初始密碼已寄送至員工信箱。", "員工已建立");
    } catch (unknownError) {
      handleHttpError(unknownError, "新增員工失敗");
    } finally {
      setLoading(false);
    }
  }

  function readImportErrorData(unknownError: unknown, fallbackMessage: string): CsvImportErrorData {
    if (!axios.isAxiosError(unknownError)) {
      return { message: fallbackMessage };
    }
    const rawData = unknownError.response?.data as
      | { data?: { message?: string; failed_at_line?: number; failedAtLine?: number; reason?: string } }
      | undefined;
    return {
      message: rawData?.data?.message ?? fallbackMessage,
      failedAtLine: rawData?.data?.failedAtLine ?? rawData?.data?.failed_at_line,
      reason: rawData?.data?.reason,
    };
  }

  async function handleDownloadCsvTemplate(type: CsvImportType) {
    if (!session) {
      return;
    }
    const card = CSV_IMPORT_CARDS.find((entry) => entry.type === type);
    setLoading(true);
    try {
      const content = await downloadImportTemplate(session.token, type);
      const blob = new Blob([content], { type: "text/csv;charset=utf-8" });
      const objectUrl = window.URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = `${type}-template.csv`;
      anchor.click();
      window.URL.revokeObjectURL(objectUrl);
      openSuccessBox(`${card?.title ?? "CSV"} 範本已下載。`, "下載完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "CSV 範本下載失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCsvImport(type: CsvImportType) {
    if (!session) {
      return;
    }
    const file = csvImportFiles[type];
    if (!file) {
      openWarningBox("請先選擇 CSV 檔案。");
      return;
    }
    setLoading(true);
    try {
      const response = await importAdminCsv(session.token, type, file);
      setCsvImportResults((current) => ({ ...current, [type]: response.data }));
      setCsvImportErrors((current) => ({ ...current, [type]: null }));
      await loadAdminData(session.token, adminOrderFilters, supplierFilters, employeeDepartmentFilter);
      openSuccessBox(`CSV 匯入成功，共 ${response.data.length} 筆。`, "匯入完成");
    } catch (unknownError) {
      const errorData = readImportErrorData(unknownError, "CSV 匯入失敗");
      setCsvImportErrors((current) => ({ ...current, [type]: errorData }));
      openErrorBox(errorData.message, "匯入失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitUpdateEmployee(employeeId: number) {
    if (!session) {
      return;
    }

    const draft = editingEmployees[employeeId];
    if (
      !draft ||
      !validateWarning("請完整填寫員工資料後再儲存。", [
        !isBlank(draft.username),
        !isBlank(draft.name),
        !isBlank(draft.email),
        !isBlank(draft.departmentId),
      ])
    ) {
      return;
    }

    setLoading(true);
    try {
      await updateEmployee(session.token, employeeId, {
        username: draft.username,
        name: draft.name,
        email: draft.email,
        departmentId: Number(draft.departmentId),
        isAdmin: draft.isAdmin,
      });
      setEditingEmployeeId(null);
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("員工資料已更新。", "員工資料已更新");
    } catch (unknownError) {
      handleHttpError(unknownError, "更新員工資料失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateDepartment() {
    if (!session) {
      return;
    }
    if (!validateWarning("請輸入部門名稱。", [!isBlank(departmentForm.name)])) {
      return;
    }

    setLoading(true);
    try {
      const response = await createDepartment(session.token, { name: departmentForm.name });
      setDepartmentForm({ name: "" });
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox(`部門 ${response.data.name} 已建立。`, "部門建立成功");
    } catch (unknownError) {
      handleHttpError(unknownError, "建立部門失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitUpdateDepartment(departmentId: number) {
    if (!session) {
      return;
    }

    const draft = editingDepartments[departmentId];
    if (!draft || isBlank(draft.name)) {
      openWarningBox("請輸入部門名稱。");
      return;
    }

    setLoading(true);
    try {
      await updateDepartment(session.token, departmentId, draft);
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("部門資料已更新。", "部門更新成功");
    } catch (unknownError) {
      handleHttpError(unknownError, "更新部門失敗");
    } finally {
      setLoading(false);
    }
  }

  async function toggleEmployeeStatus(employee: EmployeeSummary) {
    if (!session) {
      return;
    }
    setLoading(true);
    try {
      await updateEmployeeStatus(session.token, employee.id, !employee.isActive);
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox(employee.isActive ? "員工已停用。" : "員工已啟用。", "狀態已更新");
    } catch (unknownError) {
      handleHttpError(unknownError, "更新狀態失敗");
    } finally {
      setLoading(false);
    }
  }

  function startEditEmployee(employee: EmployeeSummary) {
    setEditingEmployees((current) => ({
      ...current,
      [employee.id]: {
        username: employee.username,
        name: employee.name,
        email: employee.email,
        departmentId: String(employee.department.id),
        isAdmin: employee.isAdmin,
      },
    }));
    setEditingEmployeeId(employee.id);
  }

  function cancelEditEmployee(employee: EmployeeSummary) {
    setEditingEmployees((current) => ({
      ...current,
      [employee.id]: {
        username: employee.username,
        name: employee.name,
        email: employee.email,
        departmentId: String(employee.department.id),
        isAdmin: employee.isAdmin,
      },
    }));
    setEditingEmployeeId(null);
  }

  async function submitResetPassword(employeeId: number) {
    if (!session) {
      return;
    }
    const nextPassword = resetForms[employeeId] ?? "";
    if (!validateWarning("請先輸入新的密碼。", [!isBlank(nextPassword)])) {
      return;
    }
    setLoading(true);
    try {
      await resetEmployeePassword(session.token, employeeId, nextPassword);
      setResetForms((current) => ({ ...current, [employeeId]: "" }));
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("密碼已重設，系統已送出通知。", "重設完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "重設密碼失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitOrder() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請先選擇訂餐日期與便當選項。", [
        !isBlank(orderForm.orderDate),
        !isBlank(orderForm.menuId),
      ])
    ) {
      return;
    }

    const selectedMenuId = Number(orderForm.menuId);
    const existingOrder = orders.find((entry) => entry.orderDate === orderForm.orderDate);

    setLoading(true);
    try {
      if (existingOrder) {
        await updateOrder(session.token, existingOrder.id, { menuId: selectedMenuId });
        openSuccessBox("已更新當日訂餐。", "訂單已更新");
      } else {
        await createOrder(session.token, {
          menuId: selectedMenuId,
          orderDate: orderForm.orderDate,
        });
        openSuccessBox("訂餐成功。", "送出完成");
      }
      await loadEmployeeData(session.token);
    } catch (unknownError) {
      handleHttpError(unknownError, "訂餐失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCancelOrder(orderId: number, orderDate: string) {
    if (!session) {
      return;
    }
    if (!canCancelEmployeeOrder(orderDate, currentTime)) {
      openErrorBox("已超過取消訂餐截止時間");
      return;
    }
    setLoading(true);
    try {
      await cancelOrder(session.token, orderId);
      await loadEmployeeData(session.token);
      openSuccessBox("訂餐已取消。", "訂單已取消");
    } catch (unknownError) {
      handleHttpError(unknownError, "取消訂餐失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCancelAdminOrder(orderId: number, orderDate: string) {
    if (!session) {
      return;
    }
    if (!canCancelAdminOrder(orderDate, currentTime)) {
      openErrorBox("已超過管理員取消訂餐截止時間");
      return;
    }
    setLoading(true);
    try {
      await cancelAdminOrder(session.token, orderId);
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("已取消指定員工訂餐。", "訂單已取消");
    } catch (unknownError) {
      handleHttpError(unknownError, "管理員取消訂餐失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateSupplier() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請完整填寫供應商名稱、Email、電話、負責人與營業登記編號。", [
        !isBlank(supplierForm.name),
        !isBlank(supplierForm.email),
        !isBlank(supplierForm.phone),
        !isBlank(supplierForm.contactPerson),
        !isBlank(supplierForm.businessRegistrationNo),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      const response = await createSupplier(session.token, supplierForm);
      setSupplierForm({
        name: "",
        email: "",
        phone: "",
        contactPerson: "",
        businessRegistrationNo: "",
      });
      setMenuForm((current) => ({
        ...current,
        supplierId: String(response.data.id),
      }));
      setSelectedSupplier(response.data);
      setSupplierDetailForm({
        id: String(response.data.id),
        name: response.data.name,
        email: response.data.email,
        phone: response.data.phone,
        contactPerson: response.data.contactPerson,
        businessRegistrationNo: response.data.businessRegistrationNo,
        isActive: response.data.isActive,
      });
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox(`供應商已建立，ID: ${response.data.id}`, "建立完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "新增供應商失敗");
    } finally {
      setLoading(false);
    }
  }

  async function loadSupplierDetail(supplierId: number) {
    if (!session) {
      return;
    }
    setLoading(true);
    try {
      const response = await getSupplier(session.token, supplierId);
      setSelectedSupplier(response.data);
      setSupplierDetailForm({
        id: String(response.data.id),
        name: response.data.name,
        email: response.data.email,
        phone: response.data.phone,
        contactPerson: response.data.contactPerson,
        businessRegistrationNo: response.data.businessRegistrationNo,
        isActive: response.data.isActive,
      });
    } catch (unknownError) {
      handleHttpError(unknownError, "供應商詳細資料讀取失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitUpdateSupplier() {
    if (!session || !selectedSupplier) {
      return;
    }
    if (
      !validateWarning("請完整填寫供應商名稱、Email、電話與負責人。", [
        !isBlank(supplierDetailForm.name),
        !isBlank(supplierDetailForm.email),
        !isBlank(supplierDetailForm.phone),
        !isBlank(supplierDetailForm.contactPerson),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      const response = await updateSupplier(session.token, selectedSupplier.id, {
        name: supplierDetailForm.name,
        email: supplierDetailForm.email,
        phone: supplierDetailForm.phone,
        contactPerson: supplierDetailForm.contactPerson,
        isActive: supplierDetailForm.isActive,
      });
      setSelectedSupplier(response.data);
      setSupplierDetailForm({
        id: String(response.data.id),
        name: response.data.name,
        email: response.data.email,
        phone: response.data.phone,
        contactPerson: response.data.contactPerson,
        businessRegistrationNo: response.data.businessRegistrationNo,
        isActive: response.data.isActive,
      });
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("供應商資料已更新。", "更新完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "更新供應商失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateErrorEmail() {
    if (!session) {
      return;
    }
    if (!validateWarning("請輸入錯誤通知信箱 Email。", [!isBlank(errorEmailForm.email)])) {
      return;
    }
    setLoading(true);
    try {
      const response = await createErrorEmail(session.token, errorEmailForm);
      setErrorEmails((current) => [response.data, ...current]);
      setErrorEmailForm({ email: "" });
      openSuccessBox("錯誤通知信箱已新增。", "建立完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "新增錯誤通知信箱失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitDeleteErrorEmail(id: number) {
    if (!session) {
      return;
    }
    setLoading(true);
    try {
      await deleteErrorEmail(session.token, id);
      setErrorEmails((current) => current.filter((item) => item.id !== id));
      openSuccessBox("錯誤通知信箱已刪除。", "刪除完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "刪除錯誤通知信箱失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateReportEmail() {
    if (!session) {
      return;
    }
    if (!validateWarning("請輸入月結報表收件 Email。", [!isBlank(reportEmailForm.email)])) {
      return;
    }
    setLoading(true);
    try {
      const response = await createReportEmail(session.token, reportEmailForm);
      setReportEmails((current) => [response.data, ...current]);
      setReportEmailForm({ email: "" });
      openSuccessBox("報表收件信箱已新增。", "建立完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "新增報表收件信箱失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitDeleteReportEmail(id: number) {
    if (!session) {
      return;
    }
    setLoading(true);
    try {
      await deleteReportEmail(session.token, id);
      setReportEmails((current) => current.filter((item) => item.id !== id));
      openSuccessBox("報表收件信箱已刪除。", "刪除完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "刪除報表收件信箱失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitMonthlyBillingTrigger() {
    if (!session) {
      return;
    }
    setLoading(true);
    try {
      await triggerMonthlyBilling(session.token);
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("月結報表已開始執行，請至下方發送記錄查看最新結果。", "月結報表已觸發");
    } catch (unknownError) {
      handleHttpError(unknownError, "手動觸發月結報表失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitEmployeeOrderReportQuery() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請完整選擇查詢日期起訖。", [
        !isBlank(employeeOrderReportFilters.dateFrom),
        !isBlank(employeeOrderReportFilters.dateTo),
      ])
    ) {
      return;
    }
    if (employeeOrderReportFilters.dateFrom > employeeOrderReportFilters.dateTo) {
      openWarningBox("查詢起日不可晚於迄日。");
      return;
    }
    setLoading(true);
    try {
      const response = await getEmployeeOrderReports(session.token, {
        date_from: employeeOrderReportFilters.dateFrom,
        date_to: employeeOrderReportFilters.dateTo,
        sort_by: employeeOrderReportFilters.sortBy,
      });
      setEmployeeOrderReports(response.data);
      setHasLoadedEmployeeOrderReports(true);
      openSuccessBox("員工訂餐報表已更新。", "查詢完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "員工訂餐報表查詢失敗");
    } finally {
      setLoading(false);
    }
  }

  async function handleEmployeeOrderReportPdfDownload() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請完整選擇查詢日期起訖。", [
        !isBlank(employeeOrderReportFilters.dateFrom),
        !isBlank(employeeOrderReportFilters.dateTo),
      ])
    ) {
      return;
    }
    if (employeeOrderReportFilters.dateFrom > employeeOrderReportFilters.dateTo) {
      openWarningBox("查詢起日不可晚於迄日。");
      return;
    }
    setLoading(true);
    try {
      const blob = await downloadEmployeeOrderReportPdf(session.token, {
        date_from: employeeOrderReportFilters.dateFrom,
        date_to: employeeOrderReportFilters.dateTo,
        sort_by: employeeOrderReportFilters.sortBy,
      });
      const objectUrl = window.URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = "employee-order-report.pdf";
      anchor.click();
      window.URL.revokeObjectURL(objectUrl);
      openSuccessBox("員工訂餐報表 PDF 已下載。", "下載完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "員工訂餐報表 PDF 下載失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateMenu() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請完整填寫菜單名稱、類別、價格與有效日期。", [
        !isBlank(selectedMenuSupplierId),
        !isBlank(menuForm.name),
        !isBlank(menuForm.category),
        !isBlank(menuForm.price),
        !isBlank(menuForm.validFrom),
        !isBlank(menuForm.validTo),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      await createMenu(session.token, {
        supplierId: Number(selectedMenuSupplierId),
        name: menuForm.name,
        category: menuForm.category,
        description: menuForm.description,
        price: Number(menuForm.price),
        validFrom: menuForm.validFrom,
        validTo: menuForm.validTo,
      });
      setMenuForm({
        name: "",
        category: CATEGORY_OPTIONS[0],
        description: "",
        price: "",
        validFrom: nextWeekdays()[0] ?? "",
        validTo: nextWeekdays()[4] ?? "",
      });
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      await loadSupplierMenus(session.token, selectedMenuSupplierId);
      openSuccessBox("菜單建立成功。", "建立完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "建立菜單失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitUpdateMenu(menuId: number) {
    if (!session) {
      return;
    }
    const editing = editingMenus[menuId];
    if (!editing) {
      return;
    }
    if (
      !validateWarning("請完整填寫菜單名稱、供應商、分類、價格與有效期間。", [
        typeof editing.supplierId === "number" && !Number.isNaN(editing.supplierId),
        !isBlank(editing.name ?? ""),
        !isBlank(editing.category ?? ""),
        typeof editing.price === "number" && !Number.isNaN(editing.price),
        !isBlank(editing.validFrom ?? ""),
        !isBlank(editing.validTo ?? ""),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      await updateMenu(session.token, menuId, {
        supplierId: editing.supplierId,
        name: editing.name,
        category: editing.category,
        description: editing.description,
        price: editing.price,
        validFrom: editing.validFrom,
        validTo: editing.validTo,
      });
      setEditingMenus((current) => {
        const next = { ...current };
        delete next[menuId];
        return next;
      });
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      await loadSupplierMenus(session.token, selectedMenuSupplierId);
      openSuccessBox("菜單已更新。", "更新完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "更新菜單失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitAdminOrder() {
    if (!session) {
      return;
    }
    if (
      !validateWarning("請先選擇員工、日期與菜單。", [
        !isBlank(adminOrderForm.employeeId),
        !isBlank(adminOrderForm.orderDate),
        !isBlank(adminOrderForm.menuId),
      ])
    ) {
      return;
    }
    setLoading(true);
    try {
      await createAdminOrder(session.token, {
        employeeId: Number(adminOrderForm.employeeId),
        menuId: Number(adminOrderForm.menuId),
        orderDate: adminOrderForm.orderDate,
      });
      await loadAdminData(session.token, adminOrderFilters, supplierFilters);
      openSuccessBox("已為員工建立隔日訂餐。", "建立完成");
    } catch (unknownError) {
      handleHttpError(unknownError, "管理員代訂失敗");
    } finally {
      setLoading(false);
    }
  }

  function confirmCancelOrder(orderId: number, orderDate: string) {
    if (!canCancelEmployeeOrder(orderDate, currentTime)) {
      openErrorBox("已超過取消訂餐截止時間");
      return;
    }
    openConfirmBox(
      "確定要取消這筆訂單嗎？",
      async () => submitCancelOrder(orderId, orderDate),
      "取消訂單",
    );
  }

  function confirmCancelAdminOrder(orderId: number, orderDate: string) {
    if (!canCancelAdminOrder(orderDate, currentTime)) {
      openErrorBox("已超過管理員取消訂餐截止時間");
      return;
    }
    openConfirmBox(
      "確定要取消這筆員工訂單嗎？",
      async () => submitCancelAdminOrder(orderId, orderDate),
      "取消員工訂單",
    );
  }

  function confirmDeleteErrorEmail(id: number) {
    openConfirmBox(
      "確定要刪除這筆錯誤通知信箱嗎？",
      async () => submitDeleteErrorEmail(id),
      "刪除錯誤通知信箱",
    );
  }

  function confirmDeleteReportEmail(id: number) {
    openConfirmBox(
      "確定要刪除這筆月結報表收件信箱嗎？",
      async () => submitDeleteReportEmail(id),
      "刪除報表收件信箱",
    );
  }

  function confirmToggleEmployeeStatus(employee: EmployeeSummary) {
    openConfirmBox(
      employee.isActive ? "確定要停用這位員工嗎？" : "確定要啟用這位員工嗎？",
      async () => toggleEmployeeStatus(employee),
      employee.isActive ? "停用員工" : "啟用員工",
    );
  }

  async function logout() {
    const token = session?.token;
    if (token) {
      try {
        await logoutRequest(token);
      } catch {
        // Ignore logout network failures and still clear client state.
      }
    }

    applySession(null);
    resetAppState();
    openSuccessBox("已安全登出。", "登出完成");
  }

  const introText = session
    ? session.role === "admin"
      ? "管理員登入後會直接進入訂單管理 TAB，並可切換供應商管理、報表設定、CSV 匯入與系統設定。"
      : "員工登入後會直接進入訂便當 TAB，並可切換到「我的訂單」查看訂單或切到「修改密碼」。"
    : "登入成功後，系統會依帳號角色直接導向對應的 TAB 主頁，不再顯示員工入口或管理員入口選擇頁。";

  const currentTabs = session ? tabsForRole(session.role) : [];
  const selectedOrder = orders.find((entry) => entry.orderDate === orderForm.orderDate);
  const availableEmployeeMenus = employeeMenusForDate(employeeMenus, orderForm.orderDate);
  const availableAdminMenus = menus.filter(
    (menu) => menu.validFrom <= adminOrderForm.orderDate && adminOrderForm.orderDate <= menu.validTo,
  );

  return (
    <main className="min-h-screen px-4 py-6 text-ink sm:px-6 lg:px-10">
      <div className="mx-auto flex max-w-7xl flex-col gap-6">
        {!session ? (
          <header className="overflow-hidden rounded-[2rem] border border-white/60 bg-white/70 shadow-float backdrop-blur">
            <div className="grid gap-6 px-6 py-8 lg:grid-cols-[1.15fr_0.85fr] lg:px-10">
              <div className="space-y-5">
                <p className="text-sm uppercase tracking-[0.35em] text-pine/70">A002 Bento Ordering</p>
                <div className="space-y-3">
                  <h1 className="max-w-2xl text-3xl font-semibold leading-tight sm:text-5xl">
                    公司員工訂便當系統
                    <span className="block text-clay">角色自動導向登入入口</span>
                  </h1>
                  <p className="max-w-xl text-sm leading-7 text-ink/70 sm:text-base">{introText}</p>
                </div>
                <div className="rounded-[1.5rem] border border-ink/10 bg-white/70 px-4 py-4 text-sm leading-7 text-ink/70">
                  系統會在登入成功後依 JWT 角色自動進入對應主頁：
                  員工進入「訂便當」，管理員進入「訂單管理」。
                </div>
              </div>
              <div className="rounded-[1.75rem] border border-ink/10 bg-[#171717] px-6 py-6 text-white">
                <p className="text-sm uppercase tracking-[0.3em] text-white/55">Demo Accounts</p>
                <div className="mt-5 space-y-4 text-sm text-white/80">
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                    <p className="text-white">員工</p>
                    <p>帳號: alice</p>
                    <p>密碼: WelcomeA1</p>
                  </div>
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                    <p className="text-white">管理員</p>
                    <p>帳號: admin</p>
                    <p>密碼: AdminPassA1</p>
                  </div>
                </div>
              </div>
            </div>
          </header>
        ) : (
          <header className="rounded-[1.75rem] border border-white/60 bg-white/80 px-5 py-4 shadow-float backdrop-blur sm:px-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.35em] text-pine/70">Workspace</p>
                <h1 className="mt-2 text-xl font-semibold text-ink">公司員工訂便當系統</h1>
              </div>
              <div className="text-right">
                <p className="text-sm font-medium text-clay">{formatCurrentTime(currentTime)}</p>
                <p className="text-sm font-medium text-ink">{session.name}</p>
                <p className="text-sm text-ink/65">
                  {session.role === "admin" ? "管理員" : "員工"} / 預設頁籤{" "}
                  {session.role === "admin" ? "訂單管理" : "訂便當"}
                </p>
              </div>
            </div>
          </header>
        )}

        {!session ? (
          <section className="grid gap-6 lg:grid-cols-[1fr_0.9fr]">
            <PanelCard>
              <div className="space-y-6">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Unified Login</p>
                  <h2 className="mt-3 text-2xl font-semibold">登入系統</h2>
                  <p className="mt-2 text-sm leading-7 text-ink/70">
                    不需先選擇員工或管理員入口，系統會在登入成功後自動導向對應 TAB 主頁。
                  </p>
                </div>
                <div className="grid gap-4">
                  <label className="grid gap-2 text-sm text-ink/70">
                    帳號
                    <input
                      className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 text-base outline-none transition focus:border-clay"
                      value={loginForm.username}
                      onChange={(event) =>
                        setLoginForm((current) => ({ ...current, username: event.target.value }))
                      }
                    />
                  </label>
                  <label className="grid gap-2 text-sm text-ink/70">
                    密碼
                    <input
                      className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 text-base outline-none transition focus:border-clay"
                      type="password"
                      value={loginForm.password}
                      onChange={(event) =>
                        setLoginForm((current) => ({ ...current, password: event.target.value }))
                      }
                    />
                  </label>
                </div>
                <button
                  className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white transition hover:bg-clay disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={() => void submitLogin()}
                  type="button"
                  disabled={loading}
                >
                  {loading ? "處理中..." : "登入並自動導向"}
                </button>
              </div>
            </PanelCard>

            <article className="rounded-[2rem] border border-white/60 bg-[#f1e8db]/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="space-y-6">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Support</p>
                  <h2 className="mt-3 text-2xl font-semibold">忘記密碼</h2>
                  <p className="mt-2 text-sm leading-7 text-ink/70">
                    輸入帳號對應信箱後，系統會產生臨時密碼並寄送到員工 Email。
                  </p>
                </div>
                <label className="grid gap-2 text-sm text-ink/70">
                  Email
                  <input
                    className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-base outline-none transition focus:border-pine"
                    value={forgotEmail}
                    onChange={(event) => setForgotEmail(event.target.value)}
                  />
                </label>
                <button
                  className="rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink transition hover:border-pine hover:text-pine disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={() => void submitForgotPassword()}
                  type="button"
                  disabled={loading}
                >
                  寄送臨時密碼
                </button>
              </div>
            </article>
          </section>
        ) : null}

        {session?.role === "employee" ? (
          <section className="grid gap-6">
            <PanelCard>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Employee Workspace</p>
                  <h2 className="mt-3 text-2xl font-semibold">{session.name}</h2>
                  <p className="mt-2 text-sm text-ink/65">登入身分: 員工 / 帳號 {session.username}</p>
                </div>
                <button
                  className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                  onClick={logout}
                  type="button"
                >
                  登出
                </button>
              </div>
              <div className="mt-6 space-y-6">
                <TabBar tabs={currentTabs} activeTab={activeTab} onChange={setActiveTab} />

                {activeTab === "employee-ordering" ? (
                  <div className="grid gap-6">
                    <div className="space-y-4 rounded-[1.75rem] border border-ink/10 bg-[#fcfbf7] p-6">
                      <div>
                        <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Ordering</p>
                        <h3 className="mt-3 text-xl font-semibold">訂便當</h3>
                      </div>
                      {deadlineMessage ? (
                        <div className="rounded-[1.5rem] border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
                          {deadlineMessage}
                        </div>
                      ) : (
                        <>
                          <div className="grid gap-4 sm:grid-cols-2">
                            <label className="grid gap-2 text-sm text-ink/70">
                              訂餐日期
                              <select
                                className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                value={orderForm.orderDate}
                                onChange={(event) =>
                                  setOrderForm((current) => ({ ...current, orderDate: event.target.value }))
                                }
                              >
                                {employeeOrderDates.map((day) => (
                                  <option key={day} value={day}>
                                    {formatDateWithWeekday(day)}
                                  </option>
                                ))}
                              </select>
                            </label>
                            <label className="grid gap-2 text-sm text-ink/70">
                              便當選項
                              <select
                                className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                value={orderForm.menuId}
                                onChange={(event) =>
                                  setOrderForm((current) => ({ ...current, menuId: event.target.value }))
                                }
                              >
                                {availableEmployeeMenus.map((menu) => (
                                  <option key={menu.id} value={menu.id}>
                                    {menu.name} / {menu.category}
                                  </option>
                                ))}
                              </select>
                            </label>
                          </div>
                          <div className="rounded-[1.5rem] bg-[#171717] p-5 text-sm leading-7 text-white/80">
                            員工端不顯示價格資訊。可訂日期為本次星期五 12:00 截止後一天起到下一個星期五，週末若有設定菜單也可訂；若同一天已經下單，送出後會以最新選擇覆蓋舊訂單。
                          </div>
                          {!availableEmployeeMenus.length ? (
                            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
                              目前選定日期尚無可訂便當選項，請改選其他日期。
                            </div>
                          ) : null}
                          {selectedOrder ? (
                            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                              {formatDateWithWeekday(selectedOrder.orderDate)} 已有訂單，目前為 {selectedOrder.menuName}
                            </div>
                          ) : null}
                          <button
                            className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void submitOrder()}
                            type="button"
                            disabled={loading || !orderForm.menuId || !orderForm.orderDate}
                          >
                            {selectedOrder ? "更新當日訂單" : "送出訂餐"}
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ) : null}

                {activeTab === "employee-orders" ? (
                  <div className="grid gap-6">
                    <div className="space-y-4 rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                      <div>
                        <p className="text-sm uppercase tracking-[0.35em] text-clay/80">My Orders</p>
                        <h3 className="mt-3 text-xl font-semibold">我的訂單</h3>
                        <p className="mt-2 text-sm leading-7 text-ink/65">
                          送出訂單後，右側會同步顯示目前個人訂單；各筆訂單超過前一日 16:30 後會隱藏取消按鈕。
                        </p>
                      </div>
                      <div className="grid gap-3">
                        {orders.length ? (
                          orders.map((order) => (
                            <div key={order.id} className="rounded-2xl border border-ink/10 bg-white p-4">
                              <div className="flex items-start justify-between gap-3">
                                <div>
                                  <p className="font-medium text-ink">{order.menuName}</p>
                                  <p className="mt-1 text-sm text-ink/65">
                                    訂餐日期 {formatDateWithWeekday(order.orderDate)}
                                  </p>
                                  <p className="text-xs text-ink/45">
                                    取消截止 {formatDateTime(employeeOrderCancellationDeadline(order.orderDate).toISOString())}
                                  </p>
                                  <p className="text-xs text-ink/45">
                                    建立時間 {formatDateTime(order.createdAt)}
                                  </p>
                                </div>
                                {canCancelEmployeeOrder(order.orderDate, currentTime) ? (
                                  <button
                                    className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                                    onClick={() => confirmCancelOrder(order.id, order.orderDate)}
                                    type="button"
                                    disabled={loading}
                                  >
                                    取消
                                  </button>
                                ) : null}
                              </div>
                            </div>
                          ))
                        ) : (
                          <div className="rounded-2xl border border-ink/10 bg-white p-4 text-sm text-ink/65">
                            目前尚無個人訂餐記錄
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ) : null}

                {activeTab === "employee-change-password" ? (
                  <div className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
                    <div className="space-y-4 rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                      <div>
                        <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Security</p>
                        <h3 className="mt-3 text-xl font-semibold">修改密碼</h3>
                      </div>
                      <label className="grid gap-2 text-sm text-ink/70">
                        舊密碼
                        <input
                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                          type="password"
                          value={employeeChangeForm.oldPassword}
                          onChange={(event) =>
                            setEmployeeChangeForm((current) => ({ ...current, oldPassword: event.target.value }))
                          }
                        />
                      </label>
                      <label className="grid gap-2 text-sm text-ink/70">
                        新密碼
                        <input
                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                          type="password"
                          value={employeeChangeForm.newPassword}
                          onChange={(event) =>
                            setEmployeeChangeForm((current) => ({ ...current, newPassword: event.target.value }))
                          }
                        />
                      </label>
                      <button
                        className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                        onClick={() => void submitEmployeeChangePassword()}
                        type="button"
                        disabled={loading}
                      >
                        更新密碼
                      </button>
                    </div>
                    <div className="rounded-[1.75rem] border border-ink/10 bg-white p-6 text-sm leading-7 text-ink/65">
                      修改密碼完成後，系統會要求重新登入；若要新增或更換訂單，請切回「訂便當」TAB 操作。
                    </div>
                  </div>
                ) : null}
              </div>
            </PanelCard>
          </section>
        ) : null}

        {session?.role === "admin" ? (
          <section className="grid gap-6">
            <PanelCard>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Admin Console</p>
                  <h2 className="mt-3 text-2xl font-semibold">{session.name}</h2>
                  <p className="mt-2 text-sm text-ink/65">
                    這裡可以依 TAB 切換訂單管理、供應商管理、報表設定、CSV 匯入與系統設定，不重新整理整頁。
                  </p>
                </div>
                <button
                  className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                  onClick={logout}
                  type="button"
                >
                  登出
                </button>
              </div>
              <div className="mt-6 space-y-6">
                <TabBar tabs={currentTabs} activeTab={activeTab} onChange={setActiveTab} />

                {activeTab === "admin-orders" ? (
                  <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
                    <article className="rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                      <div className="space-y-4">
                        <div>
                          <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Admin Orders</p>
                          <h3 className="mt-3 text-xl font-semibold">代替員工新增隔日訂餐</h3>
                        </div>
                        <div className="grid gap-3">
                          <select
                            className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                            value={adminOrderForm.employeeId}
                            onChange={(event) =>
                              setAdminOrderForm((current) => ({ ...current, employeeId: event.target.value }))
                            }
                          >
                            {employees.map((employee) => (
                              <option key={employee.id} value={employee.id}>
                                #{employee.id} {employee.name} {employee.isAdmin ? "(Admin)" : ""}
                              </option>
                            ))}
                          </select>
                          <input
                            className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                            type="date"
                            value={adminOrderForm.orderDate}
                            onChange={(event) =>
                              setAdminOrderForm((current) => ({ ...current, orderDate: event.target.value }))
                            }
                          />
                          <select
                            className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                            value={adminOrderForm.menuId}
                            onChange={(event) =>
                              setAdminOrderForm((current) => ({ ...current, menuId: event.target.value }))
                            }
                          >
                            {availableAdminMenus.map((menu) => (
                              <option key={menu.id} value={menu.id}>
                                {menu.name} / {menu.category} / NT${menu.price}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-800">
                          截止提醒：僅可在訂餐日前一日 16:30 前代訂隔日便當，系統會保留 A003 17:00 供應商通知前的 30 分鐘緩衝。
                        </div>
                        <button
                          className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                          onClick={() => void submitAdminOrder()}
                          type="button"
                          disabled={
                            loading ||
                            !adminOrderForm.employeeId ||
                            !adminOrderForm.menuId ||
                            availableAdminMenus.length === 0
                          }
                        >
                          為員工建立隔日訂單
                        </button>
                        {!availableAdminMenus.length ? (
                          <div className="rounded-2xl border border-ink/10 bg-white px-4 py-4 text-sm text-ink/65">
                            指定日期目前沒有可代訂的菜單，請先建立有效菜單。
                          </div>
                        ) : null}
                      </div>
                    </article>

                    <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                      <div className="flex flex-wrap items-center justify-between gap-4">
                        <div>
                          <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Overview</p>
                          <h3 className="mt-3 text-xl font-semibold">員工訂單總覽</h3>
                        </div>
                        <span className="rounded-full bg-[#f3efe7] px-4 py-2 text-sm text-ink/65">
                          {adminOrders.length} 筆
                        </span>
                      </div>
                      <div className="mt-6 grid gap-4 lg:grid-cols-3">
                        <input
                          className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 text-sm outline-none transition focus:border-pine"
                          type="date"
                          value={adminOrderFilters.dateFrom}
                          onChange={(event) =>
                            setAdminOrderFilters((current) => ({ ...current, dateFrom: event.target.value }))
                          }
                        />
                        <input
                          className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 text-sm outline-none transition focus:border-pine"
                          type="date"
                          value={adminOrderFilters.dateTo}
                          onChange={(event) =>
                            setAdminOrderFilters((current) => ({ ...current, dateTo: event.target.value }))
                          }
                        />
                        <select
                          className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 text-sm outline-none transition focus:border-pine"
                          value={adminOrderFilters.employeeId}
                          onChange={(event) =>
                            setAdminOrderFilters((current) => ({
                              ...current,
                              employeeId: event.target.value,
                            }))
                          }
                        >
                          <option value="">全部員工</option>
                          {employees.map((employee) => (
                            <option key={employee.id} value={employee.id}>
                              #{employee.id} {employee.name}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="mt-6 grid gap-4">
                        {adminOrders.length ? (
                          adminOrders.map((order) => (
                            <div key={order.id} className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5">
                              <div className="flex flex-wrap items-start justify-between gap-3">
                                <div>
                                  <p className="font-medium text-ink">
                                    {order.employeeName} / {order.menuName}
                                  </p>
                                  <p className="mt-1 text-sm text-ink/65">
                                    訂餐日期 {formatDate(order.orderDate)} / 供應商 {order.supplierName}
                                  </p>
                                  <p className="text-sm text-ink/55">價格 NT${order.menuPrice}</p>
                                </div>
                                <div className="flex items-start gap-2">
                                  <span className="rounded-full bg-white px-3 py-1 text-xs text-ink/65">
                                    #{order.employeeId}
                                  </span>
                                  {canCancelAdminOrder(order.orderDate, currentTime) ? (
                                    <button
                                      className="rounded-full border border-ink/10 bg-white px-4 py-2 text-sm"
                                      onClick={() => confirmCancelAdminOrder(order.id, order.orderDate)}
                                      type="button"
                                      disabled={loading}
                                    >
                                      取消
                                    </button>
                                  ) : null}
                                </div>
                              </div>
                              <div className="mt-3 text-sm text-ink/60">
                                <p>
                                  取消截止 {formatDateTime(adminOrderCancellationDeadline(order.orderDate).toISOString())}
                                </p>
                                <p>建立者 {order.createdByName ?? "員工自助"}</p>
                                <p>建立時間 {formatDateTime(order.createdAt)}</p>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                            目前查無符合條件的員工訂單
                          </div>
                        )}
                      </div>
                    </article>
                  </div>
                ) : null}

                {activeTab === "admin-suppliers" ? (
                  <div className="space-y-6">
                    <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                      <div className="flex flex-wrap items-center justify-between gap-4">
                        <div>
                          <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Supplier Workspace</p>
                          <h3 className="mt-3 text-xl font-semibold">供應商與菜單管理</h3>
                        </div>
                        <TabBar
                          tabs={[...ADMIN_SUPPLIER_TABS]}
                          activeTab={adminSupplierTab}
                          onChange={setAdminSupplierTab}
                        />
                      </div>
                    </article>

                    {adminSupplierTab === "supplier-directory" ? (
                      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
                        <article className="rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                          <div className="space-y-8">
                            <section className="space-y-4">
                              <div>
                                <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Suppliers</p>
                                <h3 className="mt-3 text-xl font-semibold">供應商管理</h3>
                              </div>
                              <div className="grid gap-3 sm:grid-cols-[1fr_140px_auto]">
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="輸入供應商名稱"
                                  value={supplierFilters.name}
                                  onChange={(event) =>
                                    setSupplierFilters((current) => ({ ...current, name: event.target.value }))
                                  }
                                />
                                <select
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  value={supplierFilters.searchType}
                                  onChange={(event) =>
                                    setSupplierFilters((current) => ({
                                      ...current,
                                      searchType: event.target.value as "exact" | "fuzzy",
                                    }))
                                  }
                                >
                                  <option value="exact">精確查詢</option>
                                  <option value="fuzzy">模糊查詢</option>
                                </select>
                                <div className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm text-ink/65">
                                  共 {suppliers.length} 筆
                                </div>
                              </div>
                              <div className="grid gap-3">
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="供應商名稱"
                                  value={supplierForm.name}
                                  onChange={(event) =>
                                    setSupplierForm((current) => ({ ...current, name: event.target.value }))
                                  }
                                />
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="通知 Email"
                                  value={supplierForm.email}
                                  onChange={(event) =>
                                    setSupplierForm((current) => ({ ...current, email: event.target.value }))
                                  }
                                />
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="聯繫電話"
                                  value={supplierForm.phone}
                                  onChange={(event) =>
                                    setSupplierForm((current) => ({ ...current, phone: event.target.value }))
                                  }
                                />
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="負責人"
                                  value={supplierForm.contactPerson}
                                  onChange={(event) =>
                                    setSupplierForm((current) => ({
                                      ...current,
                                      contactPerson: event.target.value,
                                    }))
                                  }
                                />
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="營業登記編號"
                                  value={supplierForm.businessRegistrationNo}
                                  onChange={(event) =>
                                    setSupplierForm((current) => ({
                                      ...current,
                                      businessRegistrationNo: event.target.value,
                                    }))
                                  }
                                />
                              </div>
                              <button
                                className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                                onClick={() => void submitCreateSupplier()}
                                type="button"
                                disabled={loading}
                              >
                                建立供應商
                              </button>
                              <div className="grid gap-3">
                                {suppliers.length ? (
                                  suppliers.map((supplier) => (
                                    <button
                                      key={supplier.id}
                                      className="rounded-2xl border border-ink/10 bg-white px-4 py-4 text-left transition hover:border-pine"
                                      onClick={() => void loadSupplierDetail(supplier.id)}
                                      type="button"
                                    >
                                      <div className="flex items-start justify-between gap-3">
                                        <div>
                                          <p className="font-medium text-ink">{supplier.name}</p>
                                          <p className="mt-1 text-sm text-ink/65">{supplier.email}</p>
                                          <p className="text-xs text-ink/45">
                                            #{supplier.id} / {supplier.contactPerson}
                                          </p>
                                        </div>
                                        <span
                                          className={`rounded-full px-3 py-1 text-xs ${
                                            supplier.isActive
                                              ? "bg-emerald-100 text-emerald-700"
                                              : "bg-zinc-200 text-zinc-700"
                                          }`}
                                        >
                                          {supplier.isActive ? "啟用中" : "已停用"}
                                        </span>
                                      </div>
                                    </button>
                                  ))
                                ) : (
                                  <div className="rounded-2xl border border-ink/10 bg-white px-4 py-4 text-sm text-ink/65">
                                    目前查無符合條件的供應商
                                  </div>
                                )}
                              </div>
                            </section>
                          </div>
                        </article>

                        <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                        <div className="flex flex-wrap items-center justify-between gap-4">
                          <div>
                            <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Supplier Detail</p>
                            <h3 className="mt-3 text-xl font-semibold">供應商詳細資料與編輯</h3>
                          </div>
                          <span className="rounded-full bg-[#f3efe7] px-4 py-2 text-sm text-ink/65">
                            {selectedSupplier ? `#${selectedSupplier.id}` : "未選取"}
                          </span>
                        </div>
                        {selectedSupplier ? (
                          <div className="mt-6 grid gap-4">
                            <div className="grid gap-3 sm:grid-cols-2">
                              <label className="grid gap-2 text-sm text-ink/70">
                                供應商 ID
                                <input
                                  className="rounded-2xl border border-ink/10 bg-[#f3efe7] px-4 py-3 text-sm text-ink/60"
                                  value={supplierDetailForm.id}
                                  disabled
                                />
                              </label>
                              <label className="grid gap-2 text-sm text-ink/70">
                                營業登記編號
                                <input
                                  className="rounded-2xl border border-ink/10 bg-[#f3efe7] px-4 py-3 text-sm text-ink/60"
                                  value={supplierDetailForm.businessRegistrationNo}
                                  disabled
                                />
                              </label>
                            </div>
                            <div className="grid gap-3 sm:grid-cols-2">
                              <input
                                className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                value={supplierDetailForm.name}
                                onChange={(event) =>
                                  setSupplierDetailForm((current) => ({ ...current, name: event.target.value }))
                                }
                              />
                              <input
                                className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                value={supplierDetailForm.email}
                                onChange={(event) =>
                                  setSupplierDetailForm((current) => ({ ...current, email: event.target.value }))
                                }
                              />
                            </div>
                            <div className="grid gap-3 sm:grid-cols-2">
                              <input
                                className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                value={supplierDetailForm.phone}
                                onChange={(event) =>
                                  setSupplierDetailForm((current) => ({ ...current, phone: event.target.value }))
                                }
                              />
                              <input
                                className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                value={supplierDetailForm.contactPerson}
                                onChange={(event) =>
                                  setSupplierDetailForm((current) => ({
                                    ...current,
                                    contactPerson: event.target.value,
                                  }))
                                }
                              />
                            </div>
                            <label className="flex items-center gap-3 text-sm text-ink/70">
                              <input
                                checked={supplierDetailForm.isActive}
                                onChange={(event) =>
                                  setSupplierDetailForm((current) => ({
                                    ...current,
                                    isActive: event.target.checked,
                                  }))
                                }
                                type="checkbox"
                              />
                              供應商啟用中
                            </label>
                            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-800">
                              唯讀欄位：`id` 與 `business_registration_no` 僅供顯示，不可修改。
                            </div>
                            <button
                              className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                              onClick={() => void submitUpdateSupplier()}
                              type="button"
                              disabled={loading}
                            >
                              更新供應商資料
                            </button>
                          </div>
                        ) : (
                          <div className="mt-6 rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                            請先從左側供應商清單選取一筆資料以載入詳細內容。
                          </div>
                        )}
                      </article>
                    </div>
                    ) : null}

                    {adminSupplierTab === "supplier-menus" ? (
                      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
                        <article className="rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                          <div className="space-y-6">
                            <div>
                              <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Menu Setup</p>
                              <h3 className="mt-3 text-xl font-semibold">建立菜單</h3>
                              <p className="mt-2 text-sm leading-7 text-ink/65">
                                先選擇供應商，系統會自動帶入供應商 ID 建立新菜單，右側清單也會同步顯示該供應商的菜單。
                              </p>
                            </div>

                            <section className="space-y-4">
                              <label className="grid gap-2 text-sm text-ink/70">
                                供應商
                                <select
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  value={selectedMenuSupplierId}
                                  onChange={(event) => setSelectedMenuSupplierId(event.target.value)}
                                >
                                  <option value="">請選擇供應商</option>
                                  {supplierOptions.map((supplier) => (
                                    <option key={supplier.id} value={supplier.id}>
                                      {supplier.name} #{supplier.id}
                                    </option>
                                  ))}
                                </select>
                              </label>
                              <div className="rounded-2xl border border-dashed border-ink/15 bg-white px-4 py-4 text-sm text-ink/70">
                                目前供應商 ID: {selectedMenuSupplierId || "未選擇"}
                              </div>
                            </section>

                            <section className="space-y-4">
                              <div>
                                <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Create Menu</p>
                                <h4 className="mt-2 text-lg font-semibold">新增供應商菜單</h4>
                              </div>
                              <div className="grid gap-3">
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="菜單名稱"
                                  value={menuForm.name}
                                  onChange={(event) =>
                                    setMenuForm((current) => ({ ...current, name: event.target.value }))
                                  }
                                />
                                <select
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  value={menuForm.category}
                                  onChange={(event) =>
                                    setMenuForm((current) => ({ ...current, category: event.target.value }))
                                  }
                                >
                                  {CATEGORY_OPTIONS.map((category) => (
                                    <option key={category} value={category}>
                                      {category}
                                    </option>
                                  ))}
                                </select>
                                <textarea
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="菜單說明"
                                  value={menuForm.description}
                                  onChange={(event) =>
                                    setMenuForm((current) => ({ ...current, description: event.target.value }))
                                  }
                                />
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="價格"
                                  value={menuForm.price}
                                  onChange={(event) =>
                                    setMenuForm((current) => ({ ...current, price: event.target.value }))
                                  }
                                />
                                <div className="grid gap-3 sm:grid-cols-2">
                                  <input
                                    className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                    type="date"
                                    value={menuForm.validFrom}
                                    onChange={(event) =>
                                      setMenuForm((current) => ({ ...current, validFrom: event.target.value }))
                                    }
                                  />
                                  <input
                                    className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                    type="date"
                                    value={menuForm.validTo}
                                    onChange={(event) =>
                                      setMenuForm((current) => ({ ...current, validTo: event.target.value }))
                                    }
                                  />
                                </div>
                              </div>
                              {!selectedMenuSupplierId ? (
                                <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-800">
                                  請先選擇供應商，再建立新的菜單資料。
                                </div>
                              ) : null}
                              <button
                                className="rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink disabled:cursor-not-allowed disabled:opacity-60"
                                onClick={() => void submitCreateMenu()}
                                type="button"
                                disabled={loading || !selectedMenuSupplierId}
                              >
                                建立菜單
                              </button>
                            </section>
                          </div>
                        </article>

                        <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                          <div className="flex flex-wrap items-center justify-between gap-4">
                            <div>
                              <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Menu List</p>
                              <h3 className="mt-3 text-xl font-semibold">供應商菜單清單</h3>
                            </div>
                          </div>
                          <div className="mt-6 grid gap-4">
                            {!selectedMenuSupplierId ? (
                              <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                                請先選擇供應商，系統才會顯示對應的菜單清單。
                              </div>
                            ) : supplierMenus.length ? (
                              supplierMenus.map((menu) => {
                                const editing = editingMenus[menu.id] ?? menu;
                                return (
                                  <div key={menu.id} className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5">
                                    <div className="grid gap-3">
                                      <div className="grid gap-3 sm:grid-cols-2">
                                        <input
                                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                          value={editing.name ?? ""}
                                          onChange={(event) =>
                                            setEditingMenus((current) => ({
                                              ...current,
                                              [menu.id]: { ...editing, name: event.target.value },
                                            }))
                                          }
                                        />
                                        <select
                                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                          value={String(editing.supplierId ?? "")}
                                          onChange={(event) =>
                                            setEditingMenus((current) => ({
                                              ...current,
                                              [menu.id]: { ...editing, supplierId: Number(event.target.value) },
                                            }))
                                          }
                                        >
                                          {supplierOptions.map((supplier) => (
                                            <option key={supplier.id} value={supplier.id}>
                                              {supplier.name} #{supplier.id}
                                            </option>
                                          ))}
                                        </select>
                                      </div>
                                      <div className="grid gap-3 sm:grid-cols-3">
                                        <select
                                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                          value={editing.category ?? ""}
                                          onChange={(event) =>
                                            setEditingMenus((current) => ({
                                              ...current,
                                              [menu.id]: { ...editing, category: event.target.value },
                                            }))
                                          }
                                        >
                                          {CATEGORY_OPTIONS.map((category) => (
                                            <option key={category} value={category}>
                                              {category}
                                            </option>
                                          ))}
                                        </select>
                                        <input
                                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                          value={String(editing.price ?? "")}
                                          onChange={(event) =>
                                            setEditingMenus((current) => ({
                                              ...current,
                                              [menu.id]: { ...editing, price: Number(event.target.value) },
                                            }))
                                          }
                                        />
                                        <p className="self-center text-xs text-ink/50">
                                          更新時間 {formatDateTime(menu.updatedAt)}
                                        </p>
                                      </div>
                                      <textarea
                                        className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                        value={editing.description ?? ""}
                                        onChange={(event) =>
                                          setEditingMenus((current) => ({
                                            ...current,
                                            [menu.id]: { ...editing, description: event.target.value },
                                          }))
                                        }
                                      />
                                      <div className="grid gap-3 sm:grid-cols-2">
                                        <input
                                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                          type="date"
                                          value={editing.validFrom ?? ""}
                                          onChange={(event) =>
                                            setEditingMenus((current) => ({
                                              ...current,
                                              [menu.id]: { ...editing, validFrom: event.target.value },
                                            }))
                                          }
                                        />
                                        <input
                                          className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                          type="date"
                                          value={editing.validTo ?? ""}
                                          onChange={(event) =>
                                            setEditingMenus((current) => ({
                                              ...current,
                                              [menu.id]: { ...editing, validTo: event.target.value },
                                            }))
                                          }
                                        />
                                      </div>
                                      <button
                                        className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white"
                                        onClick={() => void submitUpdateMenu(menu.id)}
                                        type="button"
                                        disabled={loading}
                                      >
                                        更新菜單
                                      </button>
                                    </div>
                                  </div>
                                );
                              })
                            ) : (
                              <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                                目前此供應商尚無菜單資料
                              </div>
                            )}
                          </div>
                        </article>
                      </div>
                    ) : null}
                  </div>
                  ) : null}

                {activeTab === "admin-reports" ? (
                  <div className="grid gap-6">
                    <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                      <div>
                        <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Reports Workspace</p>
                        <h3 className="mt-3 text-xl font-semibold">報表設定</h3>
                        <p className="mt-2 text-sm leading-7 text-ink/65">
                          在這裡切換員工訂餐報表與月結報表相關設定，不需離開目前頁面。
                        </p>
                      </div>
                      <div className="mt-6">
                        <TabBar tabs={[...ADMIN_REPORT_TABS]} activeTab={adminReportTab} onChange={setAdminReportTab} />
                      </div>
                    </article>

                    {adminReportTab === "employee-order-report" ? (
                      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
                        <article className="rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                          <div className="space-y-5">
                            <div>
                              <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Employee Order Report</p>
                              <h3 className="mt-3 text-xl font-semibold">員工訂餐報表</h3>
                              <p className="mt-2 text-sm leading-7 text-ink/65">
                                依日期區間與排序條件查詢員工訂餐明細，並可直接下載同條件 PDF。
                              </p>
                            </div>
                            <div className="grid gap-4 sm:grid-cols-2">
                              <label className="grid gap-2 text-sm text-ink/70">
                                起日
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  type="date"
                                  value={employeeOrderReportFilters.dateFrom}
                                  onChange={(event) =>
                                    setEmployeeOrderReportFilters((current) => ({
                                      ...current,
                                      dateFrom: event.target.value,
                                    }))
                                  }
                                />
                              </label>
                              <label className="grid gap-2 text-sm text-ink/70">
                                迄日
                                <input
                                  className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  type="date"
                                  value={employeeOrderReportFilters.dateTo}
                                  onChange={(event) =>
                                    setEmployeeOrderReportFilters((current) => ({
                                      ...current,
                                      dateTo: event.target.value,
                                    }))
                                  }
                                />
                              </label>
                            </div>
                            <label className="grid gap-2 text-sm text-ink/70">
                              排序方式
                              <select
                                className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                value={employeeOrderReportFilters.sortBy}
                                onChange={(event) =>
                                  setEmployeeOrderReportFilters((current) => ({
                                    ...current,
                                    sortBy: event.target.value as EmployeeOrderReportSort,
                                  }))
                                }
                              >
                                <option value="date">日期</option>
                                <option value="department">部門</option>
                                <option value="employee">員工姓名</option>
                                <option value="supplier">廠商</option>
                              </select>
                            </label>
                            <div className="flex flex-wrap gap-3">
                              <button
                                className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                                onClick={() => void submitEmployeeOrderReportQuery()}
                                type="button"
                                disabled={loading}
                              >
                                查詢報表
                              </button>
                              <button
                                className="rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink disabled:cursor-not-allowed disabled:opacity-60"
                                onClick={() => void handleEmployeeOrderReportPdfDownload()}
                                type="button"
                                disabled={loading}
                              >
                                下載 PDF
                              </button>
                            </div>
                          </div>
                        </article>

                        <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                          <div className="flex flex-wrap items-center justify-between gap-4">
                            <div>
                              <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Preview</p>
                              <h3 className="mt-3 text-xl font-semibold">畫面預覽</h3>
                            </div>
                            <span className="rounded-full bg-[#f3efe7] px-4 py-2 text-sm text-ink/65">
                              {employeeOrderReports.length} 筆
                            </span>
                          </div>
                          <div className="mt-6 grid gap-4">
                            {employeeOrderReports.length ? (
                              employeeOrderReports.map((report, index) => (
                                <div key={`${report.orderDate}-${report.employeeName}-${index}`} className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5">
                                  <div className="grid gap-2 sm:grid-cols-2">
                                    <p className="text-sm text-ink/65">日期 {formatDate(report.orderDate)}</p>
                                    <p className="text-sm text-ink/65">部門 {report.departmentName}</p>
                                    <p className="text-sm text-ink/65">員工 {report.employeeName}</p>
                                    <p className="text-sm text-ink/65">廠商 {report.supplierName}</p>
                                  </div>
                                  <p className="mt-3 font-medium text-ink">{report.menuName}</p>
                                </div>
                              ))
                            ) : hasLoadedEmployeeOrderReports ? (
                              <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                                目前查無符合條件的員工訂餐資料
                              </div>
                            ) : (
                              <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                                請先選擇日期區間與排序方式後查詢
                              </div>
                            )}
                          </div>
                        </article>
                      </div>
                    ) : null}

                    {adminReportTab === "monthly-billing" ? (
                      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
                        <article className="rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                          <div className="space-y-8">
                            <section className="space-y-4">
                              <div>
                                <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Monthly Billing</p>
                                <h3 className="mt-3 text-xl font-semibold">月結報表</h3>
                                <p className="mt-2 text-sm leading-7 text-ink/65">
                                  手動重跑當期月結報表，會寄給供應商信箱與 A008 報表收件清單。
                                </p>
                              </div>
                              <button
                                className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                                onClick={() => void submitMonthlyBillingTrigger()}
                                type="button"
                                disabled={loading}
                              >
                                手動觸發月結報表
                              </button>
                            </section>

                            <section className="space-y-4">
                              <div>
                                <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Report Recipients</p>
                                <h3 className="mt-3 text-xl font-semibold">月結報表收件信箱</h3>
                              </div>
                              <div className="flex gap-3">
                                <input
                                  className="flex-1 rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                  placeholder="finance-reports@company.local"
                                  value={reportEmailForm.email}
                                  onChange={(event) => setReportEmailForm({ email: event.target.value })}
                                />
                                <button
                                  className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                                  onClick={() => void submitCreateReportEmail()}
                                  type="button"
                                  disabled={loading}
                                >
                                  新增
                                </button>
                              </div>
                              <div className="grid gap-3">
                                {reportEmails.length ? (
                                  reportEmails.map((entry) => (
                                    <div
                                      key={entry.id}
                                      className="flex items-center justify-between gap-3 rounded-2xl border border-ink/10 bg-white px-4 py-4"
                                    >
                                      <div>
                                        <p className="font-medium text-ink">{entry.email}</p>
                                        <p className="mt-1 text-xs text-ink/45">
                                          建立者 #{entry.createdBy} / {formatDateTime(entry.createdAt)}
                                        </p>
                                      </div>
                                      <button
                                        className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                                        onClick={() => confirmDeleteReportEmail(entry.id)}
                                        type="button"
                                        disabled={loading}
                                      >
                                        刪除
                                      </button>
                                    </div>
                                  ))
                                ) : (
                                  <div className="rounded-2xl border border-ink/10 bg-white px-4 py-4 text-sm text-ink/65">
                                    目前尚未設定報表收件信箱
                                  </div>
                                )}
                              </div>
                            </section>
                          </div>
                        </article>

                        <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                          <div className="flex flex-wrap items-center justify-between gap-4">
                            <div>
                              <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Billing Logs</p>
                              <h3 className="mt-3 text-xl font-semibold">月結發送記錄</h3>
                            </div>
                            <span className="rounded-full bg-[#f3efe7] px-4 py-2 text-sm text-ink/65">
                              {monthlyBillingLogs.length} 筆
                            </span>
                          </div>
                          <div className="mt-6 grid gap-4">
                            {monthlyBillingLogs.length ? (
                              monthlyBillingLogs.map((log) => (
                                <div key={log.id} className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5">
                                  <div className="flex flex-wrap items-start justify-between gap-3">
                                    <div>
                                      <p className="font-medium text-ink">{log.supplierName}</p>
                                      <p className="mt-1 text-sm text-ink/65">
                                        期間 {formatDate(log.billingPeriodStart)} - {formatDate(log.billingPeriodEnd)}
                                      </p>
                                      <p className="text-sm text-ink/55">收件者 {log.emailTo}</p>
                                    </div>
                                    <span
                                      className={`rounded-full px-3 py-1 text-xs font-medium ${
                                        log.status === "sent"
                                          ? "bg-emerald-100 text-emerald-700"
                                          : "bg-red-100 text-red-700"
                                      }`}
                                    >
                                      {log.status}
                                    </span>
                                  </div>
                                  <div className="mt-3 text-sm text-ink/60">
                                    <p>觸發者 {log.triggeredBy ? `#${log.triggeredBy}` : "系統排程"}</p>
                                    <p>建立時間 {formatDateTime(log.createdAt)}</p>
                                    {log.sentAt ? <p>寄送時間 {formatDateTime(log.sentAt)}</p> : null}
                                    {log.errorMessage ? <p className="text-red-700">錯誤：{log.errorMessage}</p> : null}
                                  </div>
                                </div>
                              ))
                            ) : (
                              <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                                目前尚無月結發送記錄
                              </div>
                            )}
                          </div>
                        </article>
                      </div>
                    ) : null}
                  </div>
                ) : null}

                {activeTab === "admin-import" ? (
                  <div className="grid gap-6">
                    <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                      <div>
                        <p className="text-sm uppercase tracking-[0.35em] text-pine/70">CSV Import</p>
                        <h3 className="mt-3 text-xl font-semibold">CSV 批次匯入管理</h3>
                        <p className="mt-2 text-sm text-ink/65">
                          下載對應範本後填寫資料，再上傳 UTF-8 編碼 CSV。系統會依資料類型進行表頭、
                          必填欄位、重複資料與批次 transaction 驗證。
                        </p>
                      </div>
                      <div className="mt-6 grid gap-4">
                        {CSV_IMPORT_CARDS.map((card) => {
                          const resultRows = csvImportResults[card.type] ?? [];
                          const errorData = csvImportErrors[card.type];
                          return (
                            <div key={card.type} className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5">
                              <div className="flex flex-wrap items-start justify-between gap-4">
                                <div>
                                  <p className="text-sm uppercase tracking-[0.35em] text-clay/80">{card.type}</p>
                                  <h4 className="mt-2 text-lg font-semibold">{card.title}</h4>
                                  <p className="mt-2 text-sm text-ink/65">{card.description}</p>
                                </div>
                                <button
                                  className="rounded-full border border-ink/10 bg-white px-4 py-2 text-sm font-medium text-ink disabled:cursor-not-allowed disabled:opacity-60"
                                  onClick={() => void handleDownloadCsvTemplate(card.type)}
                                  type="button"
                                  disabled={loading}
                                >
                                  下載範本
                                </button>
                              </div>
                              <div className="mt-4 rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm text-ink/75">
                                表頭：`{card.header}`
                              </div>
                              <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
                                <input
                                  className="block w-full text-sm text-ink/70"
                                  type="file"
                                  accept=".csv,text/csv"
                                  onChange={(event) =>
                                    setCsvImportFiles((current) => ({
                                      ...current,
                                      [card.type]: event.target.files?.item(0) ?? null,
                                    }))
                                  }
                                />
                                <button
                                  className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                                  onClick={() => void submitCsvImport(card.type)}
                                  type="button"
                                  disabled={loading}
                                >
                                  上傳 CSV
                                </button>
                              </div>
                              {errorData ? (
                                <div className="mt-4 rounded-2xl border border-red-100 bg-red-50 px-4 py-4 text-sm text-red-700">
                                  <p>{errorData.message}</p>
                                  {errorData.failedAtLine ? (
                                    <p className="mt-2">失敗行號：第 {errorData.failedAtLine} 行</p>
                                  ) : null}
                                  {errorData.reason ? (
                                    <p className="mt-2">原因：{errorData.reason}</p>
                                  ) : null}
                                </div>
                              ) : null}
                              {resultRows.length ? (
                                <div className="mt-4 rounded-2xl border border-ink/10 bg-white p-4">
                                  <div className="flex items-center justify-between gap-3">
                                    <h5 className="text-sm font-semibold text-ink">最近一次成功匯入結果</h5>
                                    <span className="text-xs text-ink/55">共 {resultRows.length} 筆</span>
                                  </div>
                                  <div className="mt-3 overflow-x-auto">
                                    <table className="min-w-full text-left text-sm text-ink/75">
                                      <thead>
                                        <tr className="border-b border-ink/10 text-ink/55">
                                          {card.columns.map((column) => (
                                            <th key={column.key} className="px-3 py-2 font-medium">
                                              {column.label}
                                            </th>
                                          ))}
                                        </tr>
                                      </thead>
                                      <tbody>
                                        {resultRows.map((row, index) => (
                                          <tr key={`${card.type}-${index}`} className="border-b border-ink/5">
                                            {card.columns.map((column) => (
                                              <td key={column.key} className="px-3 py-2 align-top">
                                                {String(row[column.key] ?? "")}
                                              </td>
                                            ))}
                                          </tr>
                                        ))}
                                      </tbody>
                                    </table>
                                  </div>
                                </div>
                              ) : null}
                            </div>
                          );
                        })}
                      </div>
                    </article>
                  </div>
                ) : null}

                {activeTab === "admin-employees" ? (
                  <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
                    <article className="rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                      <div className="space-y-8">
                        <section className="space-y-4 rounded-[1.5rem] border border-ink/10 bg-white p-5">
                          <div>
                            <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Create</p>
                            <h3 className="mt-3 text-xl font-semibold">新增員工帳號</h3>
                            <p className="mt-2 text-sm leading-7 text-ink/65">
                              建立新員工帳號並指定部門，成功後系統會寄送初始密碼通知。
                            </p>
                          </div>
                          <div className="grid gap-3">
                            <input
                              className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                              placeholder="username"
                              value={createForm.username}
                              onChange={(event) =>
                                setCreateForm((current) => ({ ...current, username: event.target.value }))
                              }
                            />
                            <input
                              className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                              placeholder="姓名"
                              value={createForm.name}
                              onChange={(event) =>
                                setCreateForm((current) => ({ ...current, name: event.target.value }))
                              }
                            />
                            <input
                              className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                              placeholder="Email"
                              value={createForm.email}
                              onChange={(event) =>
                                setCreateForm((current) => ({ ...current, email: event.target.value }))
                              }
                            />
                            <select
                              className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                              value={createForm.departmentId}
                              onChange={(event) =>
                                setCreateForm((current) => ({
                                  ...current,
                                  departmentId: event.target.value,
                                }))
                              }
                            >
                              <option value="">選擇部門</option>
                              {departments.map((department) => (
                                <option key={department.id} value={department.id}>
                                  {department.name}
                                </option>
                              ))}
                            </select>
                          </div>
                          <button
                            className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void submitCreateEmployee()}
                            type="button"
                            disabled={loading}
                          >
                            建立員工
                          </button>
                          {!departments.length ? (
                            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                              目前沒有可用部門，請先建立部門。
                            </div>
                          ) : null}
                        </section>
                      </div>
                    </article>

                    <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                      <div className="flex items-center justify-between gap-4">
                        <div>
                          <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Employees</p>
                          <h3 className="mt-3 text-xl font-semibold">員工清單</h3>
                        </div>
                        <span className="rounded-full bg-[#f3efe7] px-4 py-2 text-sm text-ink/65">
                          {employees.length} 人
                        </span>
                      </div>
                      <div className="mt-6 grid gap-3 sm:grid-cols-[minmax(0,16rem)_1fr] sm:items-center">
                        <label className="grid gap-2 text-sm text-ink/70">
                          依部門篩選
                          <select
                            className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                            value={employeeDepartmentFilter}
                            onChange={(event) => setEmployeeDepartmentFilter(event.target.value)}
                          >
                            <option value="">全部</option>
                            {departments.map((department) => (
                              <option key={department.id} value={department.id}>
                                {department.name}
                              </option>
                            ))}
                          </select>
                        </label>
                        <p className="text-sm text-ink/55">
                          選擇特定部門時，系統會重新查詢該部門的員工；切回「全部」時不帶
                          `department_id`。
                        </p>
                      </div>
                      <div className="mt-6 grid gap-4">
                        {employees.map((employee) => {
                          const isEditing = editingEmployeeId === employee.id;
                          const draft = editingEmployees[employee.id] ?? {
                            username: employee.username,
                            name: employee.name,
                            email: employee.email,
                            departmentId: String(employee.department.id),
                            isAdmin: employee.isAdmin,
                          };

                          return (
                            <div key={employee.id} className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5">
                              <div className="flex flex-wrap items-start justify-between gap-4">
                                <div>
                                  <div className="flex items-center gap-3">
                                    <h4 className="text-lg font-medium">{employee.name}</h4>
                                    <span
                                      className={`rounded-full px-3 py-1 text-xs ${
                                        employee.isActive
                                          ? "bg-emerald-100 text-emerald-700"
                                          : "bg-zinc-200 text-zinc-700"
                                      }`}
                                    >
                                      {employee.isActive ? "啟用中" : "已停用"}
                                    </span>
                                    {employee.isAdmin ? (
                                      <span className="rounded-full bg-pine/10 px-3 py-1 text-xs text-pine">
                                        Admin
                                      </span>
                                    ) : null}
                                  </div>
                                  <p className="mt-2 text-sm text-ink/65">{employee.username}</p>
                                  <p className="text-sm text-ink/65">{employee.email}</p>
                                  <p className="text-sm text-ink/65">部門: {employee.department.name}</p>
                                  <p className="text-sm text-ink/65">ID: {employee.id}</p>
                                  <p className="mt-2 text-xs text-ink/45">
                                    更新時間 {formatDateTime(employee.updatedAt)}
                                  </p>
                                </div>
                                <div className="flex flex-wrap gap-2">
                                  <button
                                    className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                                    onClick={() =>
                                      isEditing ? cancelEditEmployee(employee) : startEditEmployee(employee)
                                    }
                                    type="button"
                                  >
                                    {isEditing ? "取消編輯" : "編輯資料"}
                                  </button>
                                  <button
                                    className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                                    onClick={() => confirmToggleEmployeeStatus(employee)}
                                    type="button"
                                  >
                                    {employee.isActive ? "停用" : "啟用"}
                                  </button>
                                </div>
                              </div>
                              {isEditing ? (
                                <div className="mt-4 grid gap-3 rounded-2xl border border-ink/10 bg-white p-4">
                                  <label className="grid gap-2 text-sm text-ink/70">
                                    員工 ID
                                    <input
                                      className="rounded-2xl border border-ink/10 bg-[#f3efe7] px-4 py-3 text-sm text-ink/60"
                                      value={employee.id}
                                      disabled
                                    />
                                  </label>
                                  <div className="grid gap-3 sm:grid-cols-2">
                                    <input
                                      className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                      placeholder="username"
                                      value={draft.username}
                                      onChange={(event) =>
                                        setEditingEmployees((current) => ({
                                          ...current,
                                          [employee.id]: {
                                            ...draft,
                                            username: event.target.value,
                                          },
                                        }))
                                      }
                                    />
                                    <input
                                      className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                      placeholder="姓名"
                                      value={draft.name}
                                      onChange={(event) =>
                                        setEditingEmployees((current) => ({
                                          ...current,
                                          [employee.id]: {
                                            ...draft,
                                            name: event.target.value,
                                          },
                                        }))
                                      }
                                    />
                                  </div>
                                  <input
                                    className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                    placeholder="Email"
                                    value={draft.email}
                                    onChange={(event) =>
                                      setEditingEmployees((current) => ({
                                        ...current,
                                        [employee.id]: {
                                          ...draft,
                                          email: event.target.value,
                                        },
                                      }))
                                    }
                                  />
                                  <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                                    <select
                                      className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-pine"
                                      value={draft.departmentId}
                                      onChange={(event) =>
                                        setEditingEmployees((current) => ({
                                          ...current,
                                          [employee.id]: {
                                            ...draft,
                                            departmentId: event.target.value,
                                          },
                                        }))
                                      }
                                    >
                                      <option value="">選擇部門</option>
                                      {departments.map((department) => (
                                          <option key={department.id} value={department.id}>
                                            {department.name}
                                          </option>
                                        ))}
                                    </select>
                                    <label className="flex items-center gap-2 text-sm text-ink/70">
                                      <input
                                        checked={draft.isAdmin}
                                        type="checkbox"
                                        onChange={(event) =>
                                          setEditingEmployees((current) => ({
                                            ...current,
                                            [employee.id]: {
                                              ...draft,
                                              isAdmin: event.target.checked,
                                            },
                                          }))
                                        }
                                      />
                                      管理員
                                    </label>
                                  </div>
                                  <div className="flex flex-wrap gap-2">
                                    <button
                                      className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                                      onClick={() => void submitUpdateEmployee(employee.id)}
                                      type="button"
                                      disabled={loading}
                                    >
                                      儲存更新
                                    </button>
                                    <button
                                      className="rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink"
                                      onClick={() => cancelEditEmployee(employee)}
                                      type="button"
                                      disabled={loading}
                                    >
                                      取消
                                    </button>
                                  </div>
                                </div>
                              ) : null}
                              {!employee.isAdmin ? (
                                <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                                  <input
                                    className="min-w-0 flex-1 rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                                    placeholder="輸入新密碼"
                                    type="password"
                                    value={resetForms[employee.id] ?? ""}
                                    onChange={(event) =>
                                      setResetForms((current) => ({
                                        ...current,
                                        [employee.id]: event.target.value,
                                      }))
                                    }
                                  />
                                  <button
                                    className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white"
                                    onClick={() => void submitResetPassword(employee.id)}
                                    type="button"
                                  >
                                    重設密碼
                                  </button>
                                </div>
                              ) : null}
                            </div>
                          );
                        })}
                      </div>
                    </article>
                  </div>
                ) : null}

                {activeTab === "admin-settings" ? (
                  <div className="grid gap-6">
                    <article className="rounded-[1.75rem] border border-ink/10 bg-[#fcfbf7] p-6">
                      <section className="space-y-4">
                        <div>
                          <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Security</p>
                          <h3 className="mt-3 text-xl font-semibold">修改自己的密碼</h3>
                          <p className="mt-2 text-sm leading-7 text-ink/65">
                            管理員可在系統設定中直接更新個人密碼；成功後會立即要求重新登入。
                          </p>
                        </div>
                        <div className="grid gap-4 sm:grid-cols-2">
                          <label className="grid gap-2 text-sm text-ink/70">
                            舊密碼
                            <input
                              className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                              type="password"
                              value={adminChangeForm.oldPassword}
                              onChange={(event) =>
                                setAdminChangeForm((current) => ({ ...current, oldPassword: event.target.value }))
                              }
                            />
                          </label>
                          <label className="grid gap-2 text-sm text-ink/70">
                            新密碼
                            <input
                              className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                              type="password"
                              value={adminChangeForm.newPassword}
                              onChange={(event) =>
                                setAdminChangeForm((current) => ({ ...current, newPassword: event.target.value }))
                              }
                            />
                          </label>
                        </div>
                        <button
                          className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                          onClick={() => void submitAdminChangePassword()}
                          type="button"
                          disabled={loading}
                        >
                          更新密碼
                        </button>
                      </section>
                    </article>

                    <article className="rounded-[1.75rem] border border-ink/10 bg-[#f1e8db]/80 p-6">
                      <section className="space-y-4">
                        <div>
                          <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Settings</p>
                          <h3 className="mt-3 text-xl font-semibold">錯誤通知信箱</h3>
                          <p className="mt-2 text-sm leading-7 text-ink/65">
                            A003 供應商通知排程失敗時，會直接讀取這份收件清單。
                          </p>
                        </div>
                        <div className="flex gap-3">
                          <input
                            className="flex-1 rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                            placeholder="ops-alerts@company.local"
                            value={errorEmailForm.email}
                            onChange={(event) => setErrorEmailForm({ email: event.target.value })}
                          />
                          <button
                            className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void submitCreateErrorEmail()}
                            type="button"
                            disabled={loading}
                          >
                            新增
                          </button>
                        </div>
                        <div className="grid gap-3">
                          {errorEmails.length ? (
                            errorEmails.map((entry) => (
                              <div
                                key={entry.id}
                                className="flex items-center justify-between gap-3 rounded-2xl border border-ink/10 bg-white px-4 py-4"
                              >
                                <div>
                                  <p className="font-medium text-ink">{entry.email}</p>
                                  <p className="mt-1 text-xs text-ink/45">
                                    建立者 #{entry.createdBy} / {formatDateTime(entry.createdAt)}
                                  </p>
                                </div>
                                <button
                                  className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                                  onClick={() => confirmDeleteErrorEmail(entry.id)}
                                  type="button"
                                  disabled={loading}
                                >
                                  刪除
                                </button>
                              </div>
                            ))
                          ) : (
                            <div className="rounded-2xl border border-ink/10 bg-white px-4 py-4 text-sm text-ink/65">
                              目前尚未設定錯誤通知信箱
                            </div>
                          )}
                        </div>
                      </section>
                    </article>
                  </div>
                ) : null}

                {activeTab === "admin-departments" ? (
                  <div className="grid gap-6">
                    <article className="rounded-[1.75rem] border border-ink/10 bg-white p-6">
                      <section className="space-y-5">
                        <div>
                          <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Departments</p>
                          <h3 className="mt-3 text-xl font-semibold">部門管理</h3>
                          <p className="mt-2 text-sm leading-7 text-ink/65">
                            維護可供員工建立、查詢與篩選流程使用的部門主檔。
                          </p>
                        </div>
                        <div className="flex flex-col gap-3 sm:flex-row">
                          <input
                            className="min-w-0 flex-1 rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                            placeholder="新增部門名稱"
                            value={departmentForm.name}
                            onChange={(event) => setDepartmentForm({ name: event.target.value })}
                          />
                          <button
                            className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void submitCreateDepartment()}
                            type="button"
                            disabled={loading}
                          >
                            建立部門
                          </button>
                        </div>
                        <div className="grid gap-3">
                          {departments.length ? (
                            departments.map((department) => {
                              const draft = editingDepartments[department.id] ?? {
                                name: department.name,
                              };
                              return (
                                <div
                                  key={department.id}
                                  className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-4"
                                >
                                  <div className="flex flex-wrap items-center gap-3">
                                    <input
                                      className="min-w-0 flex-1 rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                                      value={draft.name}
                                      onChange={(event) =>
                                        setEditingDepartments((current) => ({
                                          ...current,
                                          [department.id]: {
                                            name: event.target.value,
                                          },
                                        }))
                                      }
                                    />
                                  </div>
                                  <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
                                    <p className="text-xs text-ink/45">#{department.id}</p>
                                    <div className="flex flex-wrap gap-2">
                                      <button
                                        className="rounded-full border border-ink/10 bg-white px-4 py-2 text-sm"
                                        onClick={() => void submitUpdateDepartment(department.id)}
                                        type="button"
                                        disabled={loading}
                                      >
                                        儲存
                                      </button>
                                    </div>
                                  </div>
                                </div>
                              );
                            })
                          ) : (
                            <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-4 text-sm text-ink/65">
                              目前沒有部門資料。
                            </div>
                          )}
                        </div>
                      </section>
                    </article>
                  </div>
                ) : null}
              </div>
            </PanelCard>
          </section>
        ) : null}
      </div>
      <MessageBox
        state={messageBox}
        busy={messageBoxBusy}
        onClose={closeMessageBox}
        onConfirm={handleMessageBoxConfirm}
      />
    </main>
  );
}
