export function employeeMenusForDate(menus, orderDate) {
    return menus.filter((menu) => menu.validFrom <= orderDate && orderDate <= menu.validTo);
}
export function employeeOrderableDatesFor(menus, now) {
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
    const orderableDates = [];
    for (const cursor = new Date(rangeStart); cursor <= rangeEnd; cursor.setDate(cursor.getDate() + 1)) {
        const date = toDateInputValue(cursor);
        if (employeeMenusForDate(menus, date).length) {
            orderableDates.push(date);
        }
    }
    return orderableDates;
}
export function buildAdminOrders(orders, employees, menus, suppliers) {
    const employeesById = new Map(employees.map((employee) => [employee.id, employee]));
    const menusById = new Map(menus.map((menu) => [menu.id, menu]));
    const suppliersById = new Map(suppliers.map((supplier) => [supplier.id, supplier]));
    return orders.map((order) => {
        const menu = menusById.get(order.menuId);
        const supplier = menu ? suppliersById.get(menu.supplierId) : null;
        const createdByEmployee = order.createdBy != null && order.createdBy !== order.employeeId
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
function toDateInputValue(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}
