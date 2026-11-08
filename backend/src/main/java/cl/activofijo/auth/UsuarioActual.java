package cl.activofijo.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Acceso estático al usuario en sesión (para auditoría y servicios). */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Optional<UsuarioSesion> obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioSesion u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }
}
