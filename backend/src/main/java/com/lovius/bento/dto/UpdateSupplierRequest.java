package com.lovius.bento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

public record UpdateSupplierRequest(
        @NotBlank(message = "供應商名稱不可為空白")
        String name,
        @NotBlank(message = "供應商 Email 不可為空白")
        @Email(message = "供應商 Email 格式不正確")
        String email,
        @NotBlank(message = "供應商電話不可為空白")
        String phone,
        @NotBlank(message = "供應商負責人不可為空白")
        String contactPerson,
        @NotNull(message = "供應商啟用狀態不可為空白")
        Boolean isActive,
        @Null(message = "供應商 id 不可修改")
        Long id,
        @Null(message = "營業登記編號不可修改")
        String businessRegistrationNo) {}
