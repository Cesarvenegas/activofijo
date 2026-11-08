package cl.activofijo.usuario;

import cl.activofijo.comun.error.ErrorApi;

import java.security.SecureRandom;

/** Política de contraseñas y generación de claves temporales. */
public final class PoliticaPassword {

    public static final int LARGO_MINIMO = 8;
    public static final int LARGO_MAXIMO = 72;   // límite de BCrypt
    public static final String DESCRIPCION =
            "La contraseña debe tener al menos " + LARGO_MINIMO + " caracteres e incluir letras y números";

    // Sin caracteres ambiguos (0/O, 1/l/I) para que la clave temporal se pueda dictar
    private static final String LETRAS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITOS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PoliticaPassword() {
    }

    public static boolean cumple(String password) {
        return password != null
                && password.length() >= LARGO_MINIMO
                && password.length() <= LARGO_MAXIMO
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }

    public static void validar(String password) {
        if (!cumple(password)) {
            throw ErrorApi.reglaNegocio("PASSWORD_DEBIL", DESCRIPCION);
        }
    }

    /** Genera una clave temporal de 10 caracteres que cumple la política. */
    public static String generarTemporal() {
        String todos = LETRAS + DIGITOS;
        char[] clave = new char[10];
        clave[0] = LETRAS.charAt(RANDOM.nextInt(LETRAS.length()));
        clave[1] = DIGITOS.charAt(RANDOM.nextInt(DIGITOS.length()));
        for (int i = 2; i < clave.length; i++) {
            clave[i] = todos.charAt(RANDOM.nextInt(todos.length()));
        }
        // Mezclar para que la letra y el dígito garantizados no queden siempre al inicio
        for (int i = clave.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = clave[i];
            clave[i] = clave[j];
            clave[j] = t;
        }
        return new String(clave);
    }
}
