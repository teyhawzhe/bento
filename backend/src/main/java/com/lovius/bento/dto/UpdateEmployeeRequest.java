package com.lovius.bento.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRequest(
        @NotBlank(message = "username 為必填")
        String username,
        @NotBlank(message = "姓名為必填")
        String name,
        @NotBlank(message = "Email 為必填")
        @Email(message = "Email 格式錯誤")
        String email,
        @JsonAlias({"department_id", "departmentId"})
        @NotNull(message = "departmentId 為必填")
        Long departmentId,
        @JsonAlias({"is_admin", "isAdmin"})
        @NotNull(message = "isAdmin 為必填")
        Boolean isAdmin) {
}
