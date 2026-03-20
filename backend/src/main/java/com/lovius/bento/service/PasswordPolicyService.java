package com.lovius.bento.service;

import com.lovius.bento.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])[A-Za-z\\d]{8,16}$");
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public void validatePassword(String rawPassword) {
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密碼需為 8 到 16 碼，且同時包含大小寫英文字母");
        }
    }

    public String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder();
        while (builder.length() < 10) {
            int index = secureRandom.nextInt(PASSWORD_CHARS.length());
            builder.append(PASSWORD_CHARS.charAt(index));
        }
        String candidate = builder.substring(0, 8) + "A1";
        validatePassword(candidate);
        return candidate;
    }

    public String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return hash(rawPassword).equals(hashedPassword);
    }
}
