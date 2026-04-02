package com.lovius.bento.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record MenuCheckNotificationResponse(
        @JsonProperty("missing_dates") List<LocalDate> missingDates) {
}
