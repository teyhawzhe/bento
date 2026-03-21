package com.lovius.bento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateErrorEmailRequest(
        @NotBlank(message = "錯誤通知 Email 不可為空白")
        @Email(message = "錯誤通知 Email 格式錯誤")
        String email) {}
