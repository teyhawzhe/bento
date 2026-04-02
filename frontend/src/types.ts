export type UserRole = "employee" | "admin";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  role: UserRole;
  employeeId: number;
  username: string;
  name: string;
}

export interface ApiMessageResponse {
  message: string;
}

export interface EmployeeSummary {
  id: number;
  username: string;
  name: string;
  email: string;
  department: Department;
  isAdmin: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export type CsvImportType = "employees" | "departments" | "suppliers" | "menus";

export type CsvImportRowValue = string | number | boolean | null;

export type CsvImportRow = Record<string, CsvImportRowValue>;

export interface CsvImportErrorData {
  message: string;
  failedAtLine?: number;
  reason?: string;
}

export interface SessionUser {
  token: string;
  refreshToken: string;
  role: UserRole;
  employeeId: number;
  username: string;
  name: string;
}

export interface Supplier {
  id: number;
  name: string;
  email: string;
  phone: string;
  contactPerson: string;
  businessRegistrationNo: string;
  isActive: boolean;
  createdAt: string;
}

export interface Department {
  id: number;
  name: string;
}

export interface ErrorEmail {
  id: number;
  email: string;
  createdBy: number;
  createdAt: string;
}

export interface ReportEmail {
  id: number;
  email: string;
  createdBy: number;
  createdAt: string;
}

export interface MonthlyBillingLog {
  id: number;
  billingPeriodStart: string;
  billingPeriodEnd: string;
  supplierId: number;
  supplierName: string;
  emailTo: string;
  status: string;
  errorMessage: string | null;
  triggeredBy: number | null;
  sentAt: string | null;
  createdAt: string;
}

export type EmployeeOrderReportSort = "date" | "department" | "employee" | "supplier";

export interface EmployeeOrderReport {
  orderDate: string;
  departmentName: string;
  employeeName: string;
  menuName: string;
  supplierName: string;
}

export interface WorkCalendarDay {
  date: string;
  isWorkday: boolean;
}

export interface MenuCheckNotification {
  missingDates: string[];
}

export interface Menu {
  id: number;
  supplierId: number;
  name: string;
  category: string;
  description: string;
  price: number;
  validFrom: string;
  validTo: string;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeMenuOption {
  id: number;
  name: string;
  category: string;
  description: string;
  validFrom: string;
  validTo: string;
}

export interface Order {
  id: number;
  employeeId: number;
  employeeName: string;
  menuId: number;
  menuName: string;
  orderDate: string;
  createdBy: number | null;
  createdAt: string;
}

export interface AdminOrder {
  id: number;
  employeeId: number;
  employeeName: string;
  menuId: number;
  menuName: string;
  supplierId: number;
  supplierName: string;
  menuPrice: number;
  orderDate: string;
  createdBy: number | null;
  createdByName: string | null;
  createdAt: string;
}
