package com.lovius.bento.dao;

import com.lovius.bento.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    void revokeById(Long id);

    void revokeByEmployeeId(Long employeeId);
}
