package cl.activofijo.comun.rut;

import java.util.Optional;

/**
 * Utilidades para el RUT chileno (validación módulo 11).
 * <p>
 * Formato de almacenamiento: sin puntos, con guion y DV en mayúscula ({@code 12345678-K}).
 * Formato de presentación: con puntos ({@code 12.345.678-K}).
 */
public final class Rut {

    private static final int CUERPO_MIN = 1_000_000;   // RUT válidos razonables: desde 1.000.000
    private static final int CUERPO_MAX = 99_999_999;

    private Rut() {
    }

    /** Calcula el dígito verificador (0-9 o K) de un cuerpo de RUT. */
    public static char calcularDv(int cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        int resto = cuerpo;
        while (resto > 0) {
            suma += (resto % 10) * multiplicador;
            resto /= 10;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int dv = 11 - (suma % 11);
        return switch (dv) {
            case 11 -> '0';
            case 10 -> 'K';
            default -> (char) ('0' + dv);
        };
    }

    /**
     * Normaliza un RUT escrito por el usuario ("12.345.678-k", "12345678k", " 12345678-K ")
     * al formato de almacenamiento {@code 12345678-K}. Vacío si el texto no es un RUT válido.
     */
    public static Optional<String> normalizar(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.replace(".", "").replace("-", "").replace(" ", "").trim().toUpperCase();
        if (limpio.length() < 2 || limpio.length() > 9) {
            return Optional.empty();
        }
        String cuerpoTxt = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpoTxt.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        int cuerpo = Integer.parseInt(cuerpoTxt);
        if (cuerpo < CUERPO_MIN || cuerpo > CUERPO_MAX || calcularDv(cuerpo) != dv) {
            return Optional.empty();
        }
        return Optional.of(cuerpo + "-" + dv);
    }

    /** Indica si el texto es un RUT válido (en cualquier formato de escritura). */
    public static boolean esValido(String texto) {
        return normalizar(texto).isPresent();
    }

    /**
     * Formatea para mostrar: {@code 12345678-K} → {@code 12.345.678-K}.
     *
     * @throws IllegalArgumentException si el RUT no es válido
     */
    public static String formatear(String texto) {
        String normalizado = normalizar(texto)
                .orElseThrow(() -> new IllegalArgumentException("RUT inválido: " + texto));
        int guion = normalizado.indexOf('-');
        String cuerpo = normalizado.substring(0, guion);
        StringBuilder conPuntos = new StringBuilder();
        for (int i = 0; i < cuerpo.length(); i++) {
            if (i > 0 && (cuerpo.length() - i) % 3 == 0) {
                conPuntos.append('.');
            }
            conPuntos.append(cuerpo.charAt(i));
        }
        return conPuntos + normalizado.substring(guion);
    }
}
