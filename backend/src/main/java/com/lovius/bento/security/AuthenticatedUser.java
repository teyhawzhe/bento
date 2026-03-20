package com.lovius.bento.security;

public record AuthenticatedUser(Long employeeId, String username, String role) {
}
