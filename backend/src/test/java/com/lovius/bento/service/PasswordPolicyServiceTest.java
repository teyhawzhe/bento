package com.lovius.bento.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService passwordPolicyService = new PasswordPolicyService();

    @Test
    void generateTemporaryPasswordAlwaysMatchesPolicy() {
        for (int index = 0; index < 500; index++) {
            String generated = passwordPolicyService.generateTemporaryPassword();

            Assertions.assertDoesNotThrow(() -> passwordPolicyService.validatePassword(generated));
            Assertions.assertTrue(generated.length() >= 8 && generated.length() <= 16);
            Assertions.assertTrue(generated.chars().anyMatch(Character::isUpperCase));
            Assertions.assertTrue(generated.chars().anyMatch(Character::isLowerCase));
            Assertions.assertTrue(generated.chars().anyMatch(Character::isDigit));
        }
    }
}
