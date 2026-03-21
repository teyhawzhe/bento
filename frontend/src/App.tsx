import { useEffect, useState } from "react";
import axios from "axios";
import {
  cancelOrder,
  changePassword,
  createEmployee,
  createErrorEmail,
  createMenu,
  createOrder,
  createSupplier,
  deleteErrorEmail,
  forgotPassword,
  getEmployeeMenus,
  getEmployees,
  getErrorEmails,
  getMenus,
  getMyOrders,
  importEmployees,
  login,
  logout as logoutRequest,
  resetEmployeePassword,
  updateEmployeeStatus,
  updateMenu,
  updateOrder,
} from "./api";
import type {
  EmployeeCreatedResponse,
  EmployeeMenuOption,
  EmployeeSummary,
  ErrorEmail,
  ImportEmployeesResponse,
  Menu,
  Order,
  SessionUser,
  Supplier,
  UserRole,
} from "./types";

const SESSION_KEY = "bento-session";
const CATEGORY_OPTIONS = ["肉類", "海鮮", "素食"];

function readInitialRole(): UserRole {
  return window.location.pathname.startsWith("/admin") ? "admin" : "employee";
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

export default function App() {
  const [role, setRole] = useState<UserRole>(readInitialRole);
  const [session, setSession] = useState<SessionUser | null>(readSession);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [employeeMenus, setEmployeeMenus] = useState<EmployeeMenuOption[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [menus, setMenus] = useState<Menu[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [errorEmails, setErrorEmails] = useState<ErrorEmail[]>([]);
  const [createResult, setCreateResult] = useState<EmployeeCreatedResponse | null>(null);
  const [importResult, setImportResult] = useState<ImportEmployeesResponse | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [includeHistory, setIncludeHistory] = useState(false);
  const [deadlineMessage, setDeadlineMessage] = useState("");

  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [forgotEmail, setForgotEmail] = useState("");
  const [changeForm, setChangeForm] = useState({ oldPassword: "", newPassword: "" });
  const [createForm, setCreateForm] = useState({ username: "", name: "", email: "" });
  const [resetForms, setResetForms] = useState<Record<number, string>>({});
  const [orderForm, setOrderForm] = useState({
    orderDate: nextWeekdays()[0] ?? "",
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
  const [menuForm, setMenuForm] = useState({
    supplierId: "",
    name: "",
    category: CATEGORY_OPTIONS[0],
    description: "",
    price: "",
    validFrom: nextWeekdays()[0] ?? "",
    validTo: nextWeekdays()[4] ?? "",
  });
  const [editingMenus, setEditingMenus] = useState<Record<number, Partial<Menu>>>({});

  useEffect(() => {
    if (!session || session.role !== role) {
      return;
    }

    if (role === "employee") {
      void loadEmployeeData(session.token);
      return;
    }

    void loadAdminData(session.token, includeHistory);
  }, [includeHistory, role, session]);

  async function loadEmployeeData(token: string) {
    try {
      const [ordersResponse] = await Promise.all([getMyOrders(token)]);
      setOrders(ordersResponse.data);
    } catch (unknownError) {
      handleHttpError(unknownError, "訂餐記錄讀取失敗");
    }

    try {
      const response = await getEmployeeMenus(token);
      setEmployeeMenus(response.data);
      setDeadlineMessage("");
      if (!orderForm.menuId && response.data[0]) {
        setOrderForm((current) => ({
          ...current,
          menuId: String(response.data[0].id),
        }));
      }
    } catch (unknownError) {
      setEmployeeMenus([]);
      if (axios.isAxiosError(unknownError)) {
        setDeadlineMessage(unknownError.response?.data?.message ?? "本週訂餐已截止");
      } else {
        setDeadlineMessage("本週訂餐已截止");
      }
    }
  }

  async function loadAdminData(token: string, history: boolean) {
    try {
      const [employeesResponse, menusResponse, errorEmailsResponse] = await Promise.all([
        getEmployees(token),
        getMenus(token, history),
        getErrorEmails(token),
      ]);
      setEmployees(employeesResponse.data);
      setMenus(menusResponse.data);
      setErrorEmails(errorEmailsResponse.data);
    } catch (unknownError) {
      handleHttpError(unknownError, "管理資料讀取失敗");
    }
  }

  function handleHttpError(unknownError: unknown, fallbackMessage: string) {
    if (axios.isAxiosError(unknownError)) {
      setError(unknownError.response?.data?.message ?? fallbackMessage);
      return;
    }
    setError(fallbackMessage);
  }

  async function submitLogin() {
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await login(role, loginForm);
      const nextSession: SessionUser = response.data;
      setSession(nextSession);
      persistSession(nextSession);
      setMessage(role === "admin" ? "管理員登入成功，已進入菜單管理頁" : "登入成功，已進入訂餐頁");
      if (role === "employee") {
        await loadEmployeeData(nextSession.token);
      } else {
        await loadAdminData(nextSession.token, includeHistory);
      }
    } catch (unknownError) {
      handleHttpError(unknownError, "登入失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitForgotPassword() {
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await forgotPassword(forgotEmail);
      setMessage(response.data.message);
      setForgotEmail("");
    } catch (unknownError) {
      handleHttpError(unknownError, "寄送臨時密碼失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitChangePassword() {
    if (!session) {
      return;
    }
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await changePassword(
        session.token,
        changeForm.oldPassword,
        changeForm.newPassword,
      );
      setMessage(response.data.message);
      setChangeForm({ oldPassword: "", newPassword: "" });
      setSession(null);
      persistSession(null);
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
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await createEmployee(session.token, createForm);
      setCreateResult(response.data);
      setCreateForm({ username: "", name: "", email: "" });
      setMessage(response.data.message);
      await loadAdminData(session.token, includeHistory);
    } catch (unknownError) {
      handleHttpError(unknownError, "新增員工失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitImportEmployees() {
    if (!session || !selectedFile) {
      setError("請先選擇 CSV 檔案");
      return;
    }
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await importEmployees(session.token, selectedFile);
      setImportResult(response.data);
      setMessage(response.data.message);
      await loadAdminData(session.token, includeHistory);
    } catch (unknownError) {
      handleHttpError(unknownError, "CSV 匯入失敗");
    } finally {
      setLoading(false);
    }
  }

  async function toggleEmployeeStatus(employee: EmployeeSummary) {
    if (!session) {
      return;
    }
    setLoading(true);
    setError("");
    try {
      await updateEmployeeStatus(session.token, employee.id, !employee.isActive);
      setMessage(employee.isActive ? "員工已停用" : "員工已啟用");
      await loadAdminData(session.token, includeHistory);
    } catch (unknownError) {
      handleHttpError(unknownError, "更新狀態失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitResetPassword(employeeId: number) {
    if (!session) {
      return;
    }
    const nextPassword = resetForms[employeeId] ?? "";
    setLoading(true);
    setError("");
    try {
      await resetEmployeePassword(session.token, employeeId, nextPassword);
      setMessage("密碼已重設，系統已送出通知");
      setResetForms((current) => ({ ...current, [employeeId]: "" }));
      await loadAdminData(session.token, includeHistory);
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

    const selectedMenuId = Number(orderForm.menuId);
    const existingOrder = orders.find((entry) => entry.orderDate === orderForm.orderDate);

    setLoading(true);
    setError("");
    setMessage("");
    try {
      if (existingOrder) {
        await updateOrder(session.token, existingOrder.id, { menuId: selectedMenuId });
        setMessage("已更新當日訂餐");
      } else {
        await createOrder(session.token, {
          menuId: selectedMenuId,
          orderDate: orderForm.orderDate,
        });
        setMessage("訂餐成功");
      }
      await loadEmployeeData(session.token);
    } catch (unknownError) {
      handleHttpError(unknownError, "訂餐失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCancelOrder(orderId: number) {
    if (!session) {
      return;
    }
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await cancelOrder(session.token, orderId);
      setMessage(response.data.message);
      await loadEmployeeData(session.token);
    } catch (unknownError) {
      handleHttpError(unknownError, "取消訂餐失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateSupplier() {
    if (!session) {
      return;
    }
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await createSupplier(session.token, supplierForm);
      setSuppliers((current) => [response.data, ...current]);
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
      setMessage(`供應商已建立，ID: ${response.data.id}`);
    } catch (unknownError) {
      handleHttpError(unknownError, "新增供應商失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateErrorEmail() {
    if (!session) {
      return;
    }
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await createErrorEmail(session.token, errorEmailForm);
      setErrorEmails((current) => [response.data, ...current]);
      setErrorEmailForm({ email: "" });
      setMessage("錯誤通知信箱已新增");
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
    setError("");
    setMessage("");
    try {
      const response = await deleteErrorEmail(session.token, id);
      setErrorEmails((current) => current.filter((item) => item.id !== id));
      setMessage(response.data.message);
    } catch (unknownError) {
      handleHttpError(unknownError, "刪除錯誤通知信箱失敗");
    } finally {
      setLoading(false);
    }
  }

  async function submitCreateMenu() {
    if (!session) {
      return;
    }
    setLoading(true);
    setError("");
    setMessage("");
    try {
      await createMenu(session.token, {
        supplierId: Number(menuForm.supplierId),
        name: menuForm.name,
        category: menuForm.category,
        description: menuForm.description,
        price: Number(menuForm.price),
        validFrom: menuForm.validFrom,
        validTo: menuForm.validTo,
      });
      setMessage("菜單建立成功");
      setMenuForm({
        supplierId: menuForm.supplierId,
        name: "",
        category: CATEGORY_OPTIONS[0],
        description: "",
        price: "",
        validFrom: nextWeekdays()[0] ?? "",
        validTo: nextWeekdays()[4] ?? "",
      });
      await loadAdminData(session.token, includeHistory);
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
    setLoading(true);
    setError("");
    setMessage("");
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
      setMessage("菜單已更新");
      setEditingMenus((current) => {
        const next = { ...current };
        delete next[menuId];
        return next;
      });
      await loadAdminData(session.token, includeHistory);
    } catch (unknownError) {
      handleHttpError(unknownError, "更新菜單失敗");
    } finally {
      setLoading(false);
    }
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

    setSession(null);
    persistSession(null);
    setEmployees([]);
    setEmployeeMenus([]);
    setOrders([]);
    setMenus([]);
    setSuppliers([]);
    setErrorEmails([]);
    setDeadlineMessage("");
    setMessage("已安全登出");
    setError("");
  }

  const introText =
    role === "admin"
      ? "管理員可登入後維護供應商、菜單與員工帳號，並查看歷史菜單。"
      : "員工可登入後瀏覽下週工作日便當、修改訂單與查看個人訂餐記錄。";

  const selectedOrder = orders.find((entry) => entry.orderDate === orderForm.orderDate);

  return (
    <main className="min-h-screen px-4 py-6 text-ink sm:px-6 lg:px-10">
      <div className="mx-auto flex max-w-7xl flex-col gap-6">
        <header className="overflow-hidden rounded-[2rem] border border-white/60 bg-white/70 shadow-float backdrop-blur">
          <div className="grid gap-6 px-6 py-8 lg:grid-cols-[1.15fr_0.85fr] lg:px-10">
            <div className="space-y-5">
              <p className="text-sm uppercase tracking-[0.35em] text-pine/70">A002 Bento Ordering</p>
              <div className="space-y-3">
                <h1 className="max-w-2xl text-3xl font-semibold leading-tight sm:text-5xl">
                  公司員工訂便當系統
                  <span className="block text-clay">
                    {role === "admin" ? "菜單與供應商管理" : "下週便當訂餐入口"}
                  </span>
                </h1>
                <p className="max-w-xl text-sm leading-7 text-ink/70 sm:text-base">{introText}</p>
              </div>
              <div className="flex flex-wrap gap-3">
                <button
                  className={`rounded-full px-5 py-2 text-sm transition ${
                    role === "employee"
                      ? "bg-ink text-white"
                      : "border border-ink/10 bg-white text-ink/70"
                  }`}
                  onClick={() => setRole("employee")}
                  type="button"
                >
                  員工入口
                </button>
                <button
                  className={`rounded-full px-5 py-2 text-sm transition ${
                    role === "admin"
                      ? "bg-pine text-white"
                      : "border border-ink/10 bg-white text-ink/70"
                  }`}
                  onClick={() => setRole("admin")}
                  type="button"
                >
                  管理員入口
                </button>
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

        {error ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}
        {message ? (
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            {message}
          </div>
        ) : null}

        {!session || session.role !== role ? (
          <section className="grid gap-6 lg:grid-cols-[1fr_0.9fr]">
            <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="space-y-6">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">
                    {role === "admin" ? "Admin Login" : "Employee Login"}
                  </p>
                  <h2 className="mt-3 text-2xl font-semibold">
                    {role === "admin" ? "管理員登入" : "員工登入"}
                  </h2>
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
                  {loading ? "處理中..." : role === "admin" ? "登入管理後台" : "登入訂餐頁"}
                </button>
              </div>
            </article>

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

        {session && role === "employee" ? (
          <section className="grid gap-6 lg:grid-cols-[1fr_1fr]">
            <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Employee Ordering</p>
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

              {deadlineMessage ? (
                <div className="mt-6 rounded-[1.5rem] border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
                  {deadlineMessage}
                </div>
              ) : (
                <div className="mt-6 space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <label className="grid gap-2 text-sm text-ink/70">
                      訂餐日期
                      <select
                        className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                        value={orderForm.orderDate}
                        onChange={(event) =>
                          setOrderForm((current) => ({ ...current, orderDate: event.target.value }))
                        }
                      >
                        {nextWeekdays().map((day) => (
                          <option key={day} value={day}>
                            {formatDate(day)}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="grid gap-2 text-sm text-ink/70">
                      便當選項
                      <select
                        className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                        value={orderForm.menuId}
                        onChange={(event) =>
                          setOrderForm((current) => ({ ...current, menuId: event.target.value }))
                        }
                      >
                        {employeeMenus.map((menu) => (
                          <option key={menu.id} value={menu.id}>
                            {menu.name} / {menu.category}
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>
                  <div className="rounded-[1.5rem] bg-[#171717] p-5 text-sm leading-7 text-white/80">
                    員工端不顯示價格資訊。若同一天已經下單，送出後會以最新選擇覆蓋舊訂單。
                  </div>
                  {selectedOrder ? (
                    <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                      {formatDate(selectedOrder.orderDate)} 已有訂單，目前為 {selectedOrder.menuName}
                    </div>
                  ) : null}
                  <button
                    className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                    onClick={() => void submitOrder()}
                    type="button"
                    disabled={loading || !orderForm.menuId}
                  >
                    {selectedOrder ? "更新當日訂單" : "送出訂餐"}
                  </button>
                </div>
              )}
            </article>

            <article className="rounded-[2rem] border border-white/60 bg-[#f1e8db]/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="space-y-6">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-clay/80">History</p>
                  <h2 className="mt-3 text-2xl font-semibold">個人訂餐記錄</h2>
                </div>
                <div className="grid gap-3">
                  {orders.length ? (
                    orders.map((order) => (
                      <div key={order.id} className="rounded-2xl border border-ink/10 bg-white p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <p className="font-medium text-ink">{order.menuName}</p>
                            <p className="mt-1 text-sm text-ink/65">訂餐日期 {formatDate(order.orderDate)}</p>
                            <p className="text-xs text-ink/45">建立時間 {formatDateTime(order.createdAt)}</p>
                          </div>
                          <button
                            className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                            onClick={() => void submitCancelOrder(order.id)}
                            type="button"
                            disabled={loading}
                          >
                            取消
                          </button>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="rounded-2xl border border-ink/10 bg-white p-4 text-sm text-ink/65">
                      目前尚無個人訂餐記錄
                    </div>
                  )}
                </div>

                <div className="space-y-4 rounded-2xl border border-ink/10 bg-white p-5">
                  <div>
                    <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Security</p>
                    <h3 className="mt-2 text-xl font-semibold">修改密碼</h3>
                  </div>
                  <label className="grid gap-2 text-sm text-ink/70">
                    舊密碼
                    <input
                      className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                      type="password"
                      value={changeForm.oldPassword}
                      onChange={(event) =>
                        setChangeForm((current) => ({ ...current, oldPassword: event.target.value }))
                      }
                    />
                  </label>
                  <label className="grid gap-2 text-sm text-ink/70">
                    新密碼
                    <input
                      className="rounded-2xl border border-ink/10 bg-[#fcfbf7] px-4 py-3 outline-none transition focus:border-clay"
                      type="password"
                      value={changeForm.newPassword}
                      onChange={(event) =>
                        setChangeForm((current) => ({ ...current, newPassword: event.target.value }))
                      }
                    />
                  </label>
                  <button
                    className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                    onClick={() => void submitChangePassword()}
                    type="button"
                    disabled={loading}
                  >
                    更新密碼
                  </button>
                </div>
              </div>
            </article>
          </section>
        ) : null}

        {session && role === "admin" ? (
          <section className="grid gap-6">
            <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Admin Console</p>
                  <h2 className="mt-3 text-2xl font-semibold">{session.name}</h2>
                  <p className="mt-2 text-sm text-ink/65">
                    這裡可以管理供應商、建立菜單、錯誤通知信箱，並維護員工帳號。
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
            </article>

            <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
              <article className="rounded-[2rem] border border-white/60 bg-[#f1e8db]/80 p-6 shadow-float backdrop-blur sm:p-8">
                <div className="space-y-8">
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
                        onChange={(event) =>
                          setErrorEmailForm({ email: event.target.value })
                        }
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
                              onClick={() => void submitDeleteErrorEmail(entry.id)}
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

                  <section className="space-y-4">
                    <div>
                      <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Suppliers</p>
                      <h3 className="mt-3 text-xl font-semibold">新增供應商</h3>
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
                    {suppliers.length ? (
                      <div className="rounded-2xl border border-ink/10 bg-white px-4 py-4 text-sm text-ink/75">
                        最新建立供應商 ID: {suppliers[0].id} / {suppliers[0].name}
                      </div>
                    ) : null}
                  </section>

                  <section className="space-y-4">
                    <div>
                      <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Menus</p>
                      <h3 className="mt-3 text-xl font-semibold">建立菜單</h3>
                    </div>
                    <div className="grid gap-3">
                      <input
                        className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                        placeholder="供應商 ID"
                        value={menuForm.supplierId}
                        onChange={(event) =>
                          setMenuForm((current) => ({ ...current, supplierId: event.target.value }))
                        }
                      />
                      <input
                        className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                        placeholder="便當名稱"
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
                        placeholder="說明"
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
                    <button
                      className="rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink disabled:cursor-not-allowed disabled:opacity-60"
                      onClick={() => void submitCreateMenu()}
                      type="button"
                      disabled={loading}
                    >
                      建立菜單
                    </button>
                  </section>
                </div>
              </article>

              <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Menu List</p>
                    <h3 className="mt-3 text-xl font-semibold">菜單清單</h3>
                  </div>
                  <label className="flex items-center gap-2 text-sm text-ink/70">
                    <input
                      checked={includeHistory}
                      onChange={(event) => setIncludeHistory(event.target.checked)}
                      type="checkbox"
                    />
                    顯示歷史菜單
                  </label>
                </div>

                <div className="mt-6 grid gap-4">
                  {menus.map((menu) => {
                    const editing = editingMenus[menu.id] ?? menu;
                    return (
                      <div
                        key={menu.id}
                        className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5"
                      >
                        <div className="grid gap-3">
                          <div className="grid gap-3 sm:grid-cols-2">
                            <input
                              className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                              value={editing.name ?? ""}
                              onChange={(event) =>
                                setEditingMenus((current) => ({
                                  ...current,
                                  [menu.id]: {
                                    ...editing,
                                    name: event.target.value,
                                  },
                                }))
                              }
                            />
                            <input
                              className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                              value={String(editing.supplierId ?? "")}
                              onChange={(event) =>
                                setEditingMenus((current) => ({
                                  ...current,
                                  [menu.id]: {
                                    ...editing,
                                    supplierId: Number(event.target.value),
                                  },
                                }))
                              }
                            />
                          </div>
                          <div className="grid gap-3 sm:grid-cols-3">
                            <select
                              className="rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-pine"
                              value={editing.category ?? ""}
                              onChange={(event) =>
                                setEditingMenus((current) => ({
                                  ...current,
                                  [menu.id]: {
                                    ...editing,
                                    category: event.target.value,
                                  },
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
                                  [menu.id]: {
                                    ...editing,
                                    price: Number(event.target.value),
                                  },
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
                                [menu.id]: {
                                  ...editing,
                                  description: event.target.value,
                                },
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
                                  [menu.id]: {
                                    ...editing,
                                    validFrom: event.target.value,
                                  },
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
                                  [menu.id]: {
                                    ...editing,
                                    validTo: event.target.value,
                                  },
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
                  })}
                  {!menus.length ? (
                    <div className="rounded-2xl border border-ink/10 bg-[#fcfbf7] p-5 text-sm text-ink/65">
                      目前尚無菜單資料
                    </div>
                  ) : null}
                </div>
              </article>
            </div>

            <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Employees</p>
                  <h3 className="mt-3 text-xl font-semibold">員工清單</h3>
                </div>
                <span className="rounded-full bg-[#f3efe7] px-4 py-2 text-sm text-ink/65">
                  {employees.length} 人
                </span>
              </div>

              <div className="mt-6 grid gap-6 lg:grid-cols-[0.85fr_1.15fr]">
                <section className="space-y-6 rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5">
                  <div>
                    <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Create</p>
                    <h4 className="mt-2 text-lg font-semibold">新增員工帳號</h4>
                  </div>
                  <div className="grid gap-3">
                    <input
                      className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                      placeholder="username"
                      value={createForm.username}
                      onChange={(event) =>
                        setCreateForm((current) => ({ ...current, username: event.target.value }))
                      }
                    />
                    <input
                      className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                      placeholder="姓名"
                      value={createForm.name}
                      onChange={(event) =>
                        setCreateForm((current) => ({ ...current, name: event.target.value }))
                      }
                    />
                    <input
                      className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                      placeholder="Email"
                      value={createForm.email}
                      onChange={(event) =>
                        setCreateForm((current) => ({ ...current, email: event.target.value }))
                      }
                    />
                  </div>
                  <button
                    className="rounded-full bg-ink px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                    onClick={() => void submitCreateEmployee()}
                    type="button"
                    disabled={loading}
                  >
                    建立員工
                  </button>
                  {createResult ? (
                    <div className="rounded-2xl border border-emerald-200 bg-white px-4 py-3 text-sm text-emerald-700">
                      初始密碼: {createResult.generatedPassword}
                    </div>
                  ) : null}

                  <div className="border-t border-ink/10 pt-6">
                    <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Import</p>
                    <h4 className="mt-2 text-lg font-semibold">CSV 批次匯入</h4>
                    <input
                      className="mt-4 block w-full text-sm text-ink/70"
                      type="file"
                      accept=".csv,text/csv"
                      onChange={(event) => setSelectedFile(event.target.files?.item(0) ?? null)}
                    />
                    <button
                      className="mt-4 rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink disabled:cursor-not-allowed disabled:opacity-60"
                      onClick={() => void submitImportEmployees()}
                      type="button"
                      disabled={loading}
                    >
                      上傳 CSV
                    </button>
                    {importResult ? (
                      <div className="mt-4 rounded-2xl border border-ink/10 bg-white px-4 py-4 text-sm text-ink/80">
                        成功 {importResult.successCount} 筆，失敗 {importResult.failureCount} 筆
                      </div>
                    ) : null}
                  </div>
                </section>

                <section className="grid gap-4">
                  {employees.map((employee) => (
                    <div
                      key={employee.id}
                      className="rounded-[1.5rem] border border-ink/10 bg-[#fcfbf7] p-5"
                    >
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
                          <p className="mt-2 text-xs text-ink/45">
                            更新時間 {formatDateTime(employee.updatedAt)}
                          </p>
                        </div>
                        <button
                          className="rounded-full border border-ink/10 px-4 py-2 text-sm"
                          onClick={() => void toggleEmployeeStatus(employee)}
                          type="button"
                        >
                          {employee.isActive ? "停用" : "啟用"}
                        </button>
                      </div>
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
                  ))}
                </section>
              </div>

              {importResult?.errors.length ? (
                <div className="mt-6 rounded-[1.5rem] border border-red-100 bg-red-50 p-4 text-sm text-red-700">
                  {importResult.errors.map((entry) => (
                    <p key={`${entry.lineNumber}-${entry.rawData}`}>
                      第 {entry.lineNumber} 行: {entry.reason}
                    </p>
                  ))}
                </div>
              ) : null}
            </article>
          </section>
        ) : null}
      </div>
    </main>
  );
}
