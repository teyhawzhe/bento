package com.lovius.bento.dao;

import com.lovius.bento.model.MonthlyBillingLog;
import com.lovius.bento.model.MonthlyBillingLogView;
import java.util.List;

public interface MonthlyBillingLogRepository {
    MonthlyBillingLog save(MonthlyBillingLog log);
    List<MonthlyBillingLogView> findAll();
}
