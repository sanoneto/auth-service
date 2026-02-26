package com.aneto.authService.models;

import java.time.Year;
import java.util.regex.Pattern;

public class SocioUtils {
    private static final String PREFIX = "SOC";
    private static final String REGEX = "^SOC-\\d{4}-\\d{5}$";

    public static String gerarNumero(Long id) {
        String ano = String.valueOf(Year.now().getValue());
        return String.format("%s-%s-%05d", PREFIX, ano, id);
    }

    public static boolean isValido(String numeroSocio) {
        if (numeroSocio == null) return false;
        return Pattern.matches(REGEX, numeroSocio);
    }
}