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
