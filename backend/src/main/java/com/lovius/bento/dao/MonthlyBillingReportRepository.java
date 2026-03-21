package com.lovius.bento.dao;

import com.lovius.bento.model.MonthlyBillingAggregationRow;
import java.time.LocalDate;
import java.util.List;

public interface MonthlyBillingReportRepository {
    List<MonthlyBillingAggregationRow> findBillingRows(LocalDate periodStart, LocalDate periodEnd);
}
