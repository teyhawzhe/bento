export type UserRole = "employee" | "admin";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
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

export interface EmployeeCreatedResponse {
  message: string;
  employee: EmployeeSummary;
  generatedPassword: string;
}

export interface ImportEmployeesResponse {
  message: string;
  successCount: number;
  failureCount: number;
  errors: Array<{
    lineNumber: number;
    rawData: string;
    reason: string;
  }>;
}

export interface SessionUser {
  token: string;
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
  isActive: boolean;
  createdAt: string | null;
  updatedAt: string | null;
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

export interface MonthlyBillingTriggerResult {
  message: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
  supplierCount: number;
  recipientCount: number;
  failedCount: number;
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

export interface EmployeeMenuCatalog {
  orderableDates: string[];
  menus: EmployeeMenuOption[];
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
