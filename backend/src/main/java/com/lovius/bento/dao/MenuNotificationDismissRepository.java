package com.lovius.bento.dao;

import com.lovius.bento.model.MenuNotificationDismiss;
import java.time.LocalDate;

public interface MenuNotificationDismissRepository {
    boolean existsByDismissDate(LocalDate dismissDate);

    MenuNotificationDismiss save(MenuNotificationDismiss dismiss);
}
