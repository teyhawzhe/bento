package com.lovius.bento.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record WorkCalendarDayDto(
        @NotNull LocalDate date,
        @NotNull @JsonProperty("is_workday") Boolean isWorkday) {
}
