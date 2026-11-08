package cl.activofijo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Si el usuario tiene una clave temporal, solo puede usar los endpoints de autenticación
 * (yo, cambiar-password, logout). Todo lo demás responde 403 con código DEBE_CAMBIAR_PASSWORD.
 * <p>
 * Se instancia en SeguridadConfig (no es @Component para que no se registre dos veces).
 */
class FiltroCambioPassword extends OncePerRequestFilter {

    private static final List<String> PERMITIDOS = List.of("/api/v1/auth/", "/api/v1/salud");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruta = request.getRequestURI();
        boolean debeCambiar = UsuarioActual.obtener().map(UsuarioSesion::debeCambiarPassword).orElse(false);
        if (debeCambiar && ruta.startsWith("/api/") && PERMITIDOS.stream().noneMatch(ruta::startsWith)) {
            RespuestaProblema.escribir(response, 403, "Acceso denegado",
                    "Debe cambiar su contraseña temporal antes de continuar.", "DEBE_CAMBIAR_PASSWORD", ruta);
            return;
        }
        chain.doFilter(request, response);
    }
}
