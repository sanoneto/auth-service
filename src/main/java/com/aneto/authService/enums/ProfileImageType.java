package com.aneto.authService.enums;

import java.util.Locale;

public enum ProfileImageType {
    AVATAR,
    COVER,
    BACKGROUND;

    public static ProfileImageType fromString(String value) {
        if (value == null || value.isBlank()) {
            return AVATAR;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "avatar", "profile", "profilepicture" -> AVATAR;
            case "cover", "banner", "capa" -> COVER;
            case "background", "bg", "fundo" -> BACKGROUND;
            default -> throw new IllegalArgumentException("Tipo de imagem inválido: " + value);
        };
    }

    public String toPathSegment() {
        return name().toLowerCase(Locale.ROOT);
    }
}