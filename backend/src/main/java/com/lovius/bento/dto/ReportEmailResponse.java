package com.lovius.bento.dto;

import java.time.Instant;

public record ReportEmailResponse(
        Long id,
        String email,
        Long createdBy,
        Instant createdAt) {}
