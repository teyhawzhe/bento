import type {
  AdminOrder,
  EmployeeMenuOption,
  EmployeeSummary,
  Menu,
  Order,
  Supplier,
  WorkCalendarDay,
} from "./types.js";

export function employeeMenusForDate(menus: EmployeeMenuOption[], orderDate: string) {
  return menus.filter((menu) => menu.validFrom <= orderDate && orderDate <= menu.validTo);
}

export function employeeOrderableDatesFor(menus: EmployeeMenuOption[], now: Date) {
  const deadline = new Date(now);
  const day = deadline.getDay();
  const fridayOffset = (5 - day + 7) % 7;
  deadline.setDate(deadline.getDate() + fridayOffset);
  deadline.setHours(12, 0, 0, 0);

  if (now >= deadline) {
    deadline.setDate(deadline.getDate() + 7);
  }

  const rangeStart = new Date(deadline);
  rangeStart.setDate(rangeStart.getDate() + 1);

  const rangeEnd = new Date(deadline);
  rangeEnd.setDate(rangeEnd.getDate() + 7);

  const orderableDates: string[] = [];
  for (const cursor = new Date(rangeStart); cursor <= rangeEnd; cursor.setDate(cursor.getDate() + 1)) {
    const date = toDateInputValue(cursor);
    if (employeeMenusForDate(menus, date).length) {
      orderableDates.push(date);
    }
  }

  return orderableDates;
}

export function buildAdminOrders(
  orders: Order[],
  employees: EmployeeSummary[],
  menus: Menu[],
  suppliers: Supplier[],
): AdminOrder[] {
  const employeesById = new Map(employees.map((employee) => [employee.id, employee]));
  const menusById = new Map(menus.map((menu) => [menu.id, menu]));
  const suppliersById = new Map(suppliers.map((supplier) => [supplier.id, supplier]));

  return orders.map((order) => {
    const menu = menusById.get(order.menuId);
    const supplier = menu ? suppliersById.get(menu.supplierId) : null;
    const createdByEmployee =
      order.createdBy != null && order.createdBy !== order.employeeId
        ? employeesById.get(order.createdBy)
        : null;

    return {
      ...order,
      supplierId: menu?.supplierId ?? 0,
      supplierName: supplier?.name ?? "未知供應商",
      menuPrice: menu ? Number(menu.price) : 0,
      createdByName: createdByEmployee?.name ?? null,
    };
  });
}

export function buildEmployeeCalendarCells(
  year: number,
  month: number,
  days: WorkCalendarDay[],
  orderableDates: string[],
  orders: Order[],
) {
  const totalDays = new Date(year, month, 0).getDate();
  const leadingBlanks = new Date(year, month - 1, 1).getDay();
  const dayMap = new Map(days.map((day) => [day.date, day.isWorkday]));
  const orderableDateSet = new Set(orderableDates);
  const ordersByDate = new Map(orders.map((order) => [order.orderDate, order]));
  const cells: Array<{
    key: string;
    label: string;
    date: string | null;
    isWorkday: boolean;
    isOrderable: boolean;
    order: Order | null;
  }> = [];

  for (let index = 0; index < leadingBlanks; index += 1) {
    cells.push({
      key: `blank-${index}`,
      label: "",
      date: null,
      isWorkday: false,
      isOrderable: false,
      order: null,
    });
  }

  for (let day = 1; day <= totalDays; day += 1) {
    const date = toDateInputValue(new Date(year, month - 1, day));
    const isWorkday = dayMap.get(date) ?? defaultWorkdayForDate(year, month, day);
    cells.push({
      key: date,
      label: String(day),
      date,
      isWorkday,
      isOrderable: isWorkday && orderableDateSet.has(date),
      order: ordersByDate.get(date) ?? null,
    });
  }

  while (cells.length % 7 !== 0) {
    cells.push({
      key: `tail-${cells.length}`,
      label: "",
      date: null,
      isWorkday: false,
      isOrderable: false,
      order: null,
    });
  }

  return cells;
}

function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function defaultWorkdayForDate(year: number, month: number, day: number) {
  const weekday = new Date(year, month - 1, day).getDay();
  return weekday !== 0 && weekday !== 6;
}
