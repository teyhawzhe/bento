package com.lovius.bento.model;

import java.time.LocalDate;

public class WorkCalendar {
    private LocalDate date;
    private boolean workday;

    public WorkCalendar(LocalDate date, boolean workday) {
        this.date = date;
        this.workday = workday;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isWorkday() {
        return workday;
    }

    public void setWorkday(boolean workday) {
        this.workday = workday;
    }
}
