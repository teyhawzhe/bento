package com.lovius.bento.service;

import com.lovius.bento.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])[A-Za-z\\d]{8,16}$");
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final String UPPERCASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGIT_CHARS = "23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public void validatePassword(String rawPassword) {
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密碼需為 8 到 16 碼，且同時包含大小寫英文字母");
        }
    }

    public String generateTemporaryPassword() {
        List<Character> chars = new java.util.ArrayList<>();
        chars.add(randomChar(UPPERCASE_CHARS));
        chars.add(randomChar(LOWERCASE_CHARS));
        chars.add(randomChar(DIGIT_CHARS));
        while (chars.size() < 10) {
            chars.add(randomChar(PASSWORD_CHARS));
        }
        shuffle(chars);
        StringBuilder builder = new StringBuilder();
        for (char value : chars) {
            builder.append(value);
        }
        String candidate = builder.toString();
        validatePassword(candidate);
        return candidate;
    }

    private char randomChar(String source) {
        return source.charAt(secureRandom.nextInt(source.length()));
    }

    private void shuffle(List<Character> chars) {
        for (int index = chars.size() - 1; index > 0; index--) {
            int swapIndex = secureRandom.nextInt(index + 1);
            Character current = chars.get(index);
            chars.set(index, chars.get(swapIndex));
            chars.set(swapIndex, current);
        }
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
