package com.lovius.bento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateReportEmailRequest(
        @NotBlank(message = "報表收件 Email 不可為空白")
        @Email(message = "報表收件 Email 格式錯誤")
        String email) {}
