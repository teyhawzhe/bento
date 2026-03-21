package com.lovius.bento.dto;

import java.time.Instant;

public record ErrorEmailResponse(
        Long id,
        String email,
        Long createdBy,
        Instant createdAt) {}
