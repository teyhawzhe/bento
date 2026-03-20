import { useEffect, useState } from "react";
import axios from "axios";
import {
  changePassword,
  createEmployee,
  forgotPassword,
  getEmployees,
  importEmployees,
  login,
  logout as logoutRequest,
  resetEmployeePassword,
  updateEmployeeStatus,
} from "./api";
import type {
  EmployeeCreatedResponse,
  EmployeeSummary,
  ImportEmployeesResponse,
  SessionUser,
  UserRole,
} from "./types";

const SESSION_KEY = "bento-session";

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

export default function App() {
  const [role, setRole] = useState<UserRole>(readInitialRole);
  const [session, setSession] = useState<SessionUser | null>(readSession);
  const [message, setMessage] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [createResult, setCreateResult] = useState<EmployeeCreatedResponse | null>(null);
  const [importResult, setImportResult] = useState<ImportEmployeesResponse | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [forgotEmail, setForgotEmail] = useState("");
  const [changeForm, setChangeForm] = useState({ oldPassword: "", newPassword: "" });
  const [createForm, setCreateForm] = useState({ username: "", name: "", email: "" });
  const [resetForms, setResetForms] = useState<Record<number, string>>({});

  useEffect(() => {
    if (role === "admin" && session?.role === "admin") {
      void loadEmployees(session.token);
    }
  }, [role, session]);

  async function loadEmployees(token: string) {
    try {
      const response = await getEmployees(token);
      setEmployees(response.data);
    } catch (unknownError) {
      handleHttpError(unknownError, "員工清單讀取失敗");
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
      setMessage(role === "admin" ? "管理員登入成功" : "登入成功");
      if (role === "admin") {
        await loadEmployees(nextSession.token);
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
      await loadEmployees(session.token);
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
      await loadEmployees(session.token);
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
      await loadEmployees(session.token);
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
      await loadEmployees(session.token);
    } catch (unknownError) {
      handleHttpError(unknownError, "重設密碼失敗");
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
        // Best-effort logout: client-side session clear still takes priority.
      }
    }
    setEmployees([]);
    setSession(null);
    persistSession(null);
    setMessage("已安全登出");
    setError("");
  }

  const introText =
    role === "admin"
      ? "管理員可由此登入並管理員工帳號、批次匯入與狀態切換。"
      : "員工可於此登入、索取臨時密碼並在首次登入後修改密碼。";

  return (
    <main className="min-h-screen px-4 py-6 text-ink sm:px-6 lg:px-10">
      <div className="mx-auto flex max-w-7xl flex-col gap-6">
        <header className="overflow-hidden rounded-[2rem] border border-white/60 bg-white/70 shadow-float backdrop-blur">
          <div className="grid gap-6 px-6 py-8 lg:grid-cols-[1.15fr_0.85fr] lg:px-10">
            <div className="space-y-5">
              <p className="text-sm uppercase tracking-[0.35em] text-pine/70">A001 Authentication</p>
              <div className="space-y-3">
                <h1 className="max-w-2xl text-3xl font-semibold leading-tight sm:text-5xl">
                  公司員工訂便當系統
                  <span className="block text-clay">內部登入驗證模組</span>
                </h1>
                <p className="max-w-xl text-sm leading-7 text-ink/70 sm:text-base">
                  {introText}
                </p>
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
                  {loading ? "處理中..." : role === "admin" ? "登入管理後台" : "登入系統"}
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

        {session && session.role === "employee" && role === "employee" ? (
          <section className="grid gap-6 lg:grid-cols-[1fr_1fr]">
            <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Session</p>
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
              <div className="mt-8 rounded-[1.5rem] bg-[#171717] p-5 text-sm leading-7 text-white/80">
                修改密碼後會自動登出，符合 A001 的安全流程設定。
              </div>
            </article>

            <article className="rounded-[2rem] border border-white/60 bg-[#f1e8db]/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="space-y-5">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Security</p>
                  <h2 className="mt-3 text-2xl font-semibold">修改密碼</h2>
                </div>
                <label className="grid gap-2 text-sm text-ink/70">
                  舊密碼
                  <input
                    className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
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
                    className="rounded-2xl border border-ink/10 bg-white px-4 py-3 outline-none transition focus:border-clay"
                    type="password"
                    value={changeForm.newPassword}
                    onChange={(event) =>
                      setChangeForm((current) => ({ ...current, newPassword: event.target.value }))
                    }
                  />
                </label>
                <p className="text-xs leading-6 text-ink/55">
                  密碼需為 8 到 16 碼，並同時包含大小寫英文字母。
                </p>
                <button
                  className="rounded-full bg-pine px-5 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={() => void submitChangePassword()}
                  type="button"
                  disabled={loading}
                >
                  更新密碼
                </button>
              </div>
            </article>
          </section>
        ) : null}

        {session && session.role === "admin" && role === "admin" ? (
          <section className="grid gap-6">
            <article className="rounded-[2rem] border border-white/60 bg-white/80 p-6 shadow-float backdrop-blur sm:p-8">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.35em] text-pine/70">Admin Console</p>
                  <h2 className="mt-3 text-2xl font-semibold">{session.name}</h2>
                  <p className="mt-2 text-sm text-ink/65">你可以建立員工、匯入 CSV、停用帳號與重設密碼。</p>
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

            <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <article className="rounded-[2rem] border border-white/60 bg-[#f1e8db]/80 p-6 shadow-float backdrop-blur sm:p-8">
                <div className="space-y-8">
                  <section className="space-y-4">
                    <div>
                      <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Create</p>
                      <h3 className="mt-3 text-xl font-semibold">新增員工帳號</h3>
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
                  </section>

                  <section className="space-y-4">
                    <div>
                      <p className="text-sm uppercase tracking-[0.35em] text-clay/80">Import</p>
                      <h3 className="mt-3 text-xl font-semibold">CSV 批次匯入</h3>
                    </div>
                    <input
                      className="block w-full text-sm text-ink/70"
                      type="file"
                      accept=".csv,text/csv"
                      onChange={(event) =>
                        setSelectedFile(event.target.files?.item(0) ?? null)
                      }
                    />
                    <button
                      className="rounded-full border border-ink/10 bg-white px-5 py-3 text-sm font-medium text-ink disabled:cursor-not-allowed disabled:opacity-60"
                      onClick={() => void submitImportEmployees()}
                      type="button"
                      disabled={loading}
                    >
                      上傳 CSV
                    </button>
                    {importResult ? (
                      <div className="rounded-2xl border border-ink/10 bg-white px-4 py-4 text-sm text-ink/80">
                        成功 {importResult.successCount} 筆，失敗 {importResult.failureCount} 筆
                      </div>
                    ) : null}
                  </section>
                </div>
              </article>

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
                <div className="mt-6 grid gap-4">
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
            </div>
          </section>
        ) : null}
      </div>
    </main>
  );
}
