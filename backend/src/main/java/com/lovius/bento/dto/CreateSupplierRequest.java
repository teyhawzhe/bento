package com.lovius.bento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
        @NotBlank(message = "供應商名稱不可為空白")
        String name,
        @NotBlank(message = "供應商 Email 不可為空白")
        @Email(message = "供應商 Email 格式不正確")
        String email,
        @NotBlank(message = "供應商電話不可為空白")
        String phone,
        @NotBlank(message = "供應商負責人不可為空白")
        String contactPerson,
        @NotBlank(message = "營業登記編號不可為空白")
        String businessRegistrationNo) {}
