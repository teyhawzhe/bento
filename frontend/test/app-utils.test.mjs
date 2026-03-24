import test from "node:test";
import assert from "node:assert/strict";
import { buildAdminOrders, employeeMenusForDate, employeeOrderableDatesFor } from "../test-dist/app-utils.js";

test("employeeMenusForDate filters menus by valid range", () => {
  const menus = [
    { id: 1, name: "A", category: "肉類", description: "", validFrom: "2026-03-28", validTo: "2026-03-30" },
    { id: 2, name: "B", category: "素食", description: "", validFrom: "2026-03-31", validTo: "2026-04-03" },
  ];

  assert.deepEqual(
    employeeMenusForDate(menus, "2026-03-29").map((menu) => menu.id),
    [1],
  );
});

test("employeeOrderableDatesFor returns dates with menu coverage in current cycle", () => {
  const menus = [
    { id: 1, name: "A", category: "肉類", description: "", validFrom: "2026-03-28", validTo: "2026-03-29" },
    { id: 2, name: "B", category: "素食", description: "", validFrom: "2026-04-02", validTo: "2026-04-03" },
  ];

  assert.deepEqual(
    employeeOrderableDatesFor(menus, new Date("2026-03-23T09:00:00+08:00")),
    ["2026-03-28", "2026-03-29", "2026-04-02", "2026-04-03"],
  );
});

test("buildAdminOrders enriches supplier and creator details", () => {
  const orders = [
    {
      id: 1,
      employeeId: 7,
      employeeName: "Alice",
      menuId: 10,
      menuName: "雞腿便當",
      orderDate: "2026-03-25",
      createdBy: 1,
      createdAt: "2026-03-24T10:00:00Z",
    },
  ];
  const employees = [
    {
      id: 1,
      username: "admin",
      name: "System Admin",
      email: "admin@company.local",
      department: { id: 1, name: "IT" },
      isAdmin: true,
      isActive: true,
      createdAt: "2026-03-24T10:00:00Z",
      updatedAt: "2026-03-24T10:00:00Z",
    },
    {
      id: 7,
      username: "alice",
      name: "Alice",
      email: "alice@company.local",
      department: { id: 2, name: "Operations" },
      isAdmin: false,
      isActive: true,
      createdAt: "2026-03-24T10:00:00Z",
      updatedAt: "2026-03-24T10:00:00Z",
    },
  ];
  const menus = [
    {
      id: 10,
      supplierId: 5,
      name: "雞腿便當",
      category: "肉類",
      description: "",
      price: 120,
      validFrom: "2026-03-25",
      validTo: "2026-03-25",
      createdBy: 1,
      createdAt: "2026-03-24T10:00:00Z",
      updatedAt: "2026-03-24T10:00:00Z",
    },
  ];
  const suppliers = [
    {
      id: 5,
      name: "好食便當",
      email: "supplier@company.local",
      phone: "02-1234-5678",
      contactPerson: "王小明",
      businessRegistrationNo: "12345678",
      isActive: true,
      createdAt: "2026-03-24T10:00:00Z",
    },
  ];

  const result = buildAdminOrders(orders, employees, menus, suppliers);

  assert.equal(result[0].supplierName, "好食便當");
  assert.equal(result[0].menuPrice, 120);
  assert.equal(result[0].createdByName, "System Admin");
});
