package com.lovius.bento.model;

public record SupplierOrderNotificationRow(
        Long supplierId,
        String supplierName,
        String supplierEmail,
        String menuName,
        long quantity) {
}
