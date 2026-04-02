package com.lovius.bento.dao;

import com.lovius.bento.model.MenuNotificationLog;
import java.time.LocalDate;

public interface MenuNotificationLogRepository {
    boolean existsByNotifyDate(LocalDate notifyDate);

    MenuNotificationLog save(MenuNotificationLog log);
}
