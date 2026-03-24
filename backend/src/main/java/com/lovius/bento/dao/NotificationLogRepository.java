package com.lovius.bento.dao;

import com.lovius.bento.model.NotificationLog;

public interface NotificationLogRepository {
    NotificationLog save(NotificationLog log);
}
