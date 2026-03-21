package com.lovius.bento.model;

import java.math.BigDecimal;

public record MonthlyBillingAggregationRow(
        Long supplierId,
        String supplierName,
        String supplierEmail,
        String menuName,
        BigDecimal unitPrice,
        long quantity) {

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
