package com.astral.express.pccms.user.service;

final class UserText {

    private UserText() {
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
