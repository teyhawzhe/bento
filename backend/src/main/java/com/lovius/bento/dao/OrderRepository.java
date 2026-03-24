package com.lovius.bento.dao;

import com.lovius.bento.model.BentoOrder;
import com.lovius.bento.model.SupplierOrderNotificationRow;
import com.lovius.bento.model.AdminOrderView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    BentoOrder save(BentoOrder order);
    Optional<BentoOrder> findById(Long id);
    Optional<BentoOrder> findByEmployeeIdAndOrderDate(Long employeeId, LocalDate orderDate);
    List<BentoOrder> findByEmployeeId(Long employeeId);
    List<AdminOrderView> findAdminOrders(LocalDate dateFrom, LocalDate dateTo, Long employeeId);
    List<SupplierOrderNotificationRow> findSupplierOrderNotificationRows(LocalDate notifyDate);
    void deleteById(Long id);
}
