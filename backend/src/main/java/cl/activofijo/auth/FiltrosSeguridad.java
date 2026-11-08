package cl.activofijo.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;

/** Respuestas de la cadena de filtros de seguridad (401, 403) y filtros propios. */
public final class FiltrosSeguridad {

    private FiltrosSeguridad() {
    }

    /** 401: no hay sesión o expiró. */
    public static void noAutenticado(HttpServletRequest request, HttpServletResponse response,
                                     AuthenticationException ex) throws IOException {
        RespuestaProblema.escribir(response, 401, "No autenticado",
                "Debe iniciar sesión para continuar.", "NO_AUTENTICADO", request.getRequestURI());
    }

    /** 403: sin permisos para la operación, o token CSRF ausente/inválido. */
    public static void accesoDenegado(HttpServletRequest request, HttpServletResponse response,
                                      AccessDeniedException ex) throws IOException {
        if (ex instanceof CsrfException) {
            RespuestaProblema.escribir(response, 403, "Acceso denegado",
                    "La sesión de seguridad expiró. Recargue la página e intente nuevamente.", "CSRF_INVALIDO",
                    request.getRequestURI());
            return;
        }
        RespuestaProblema.escribir(response, 403, "Acceso denegado",
                "No tiene permisos para realizar esta operación.", "SIN_PERMISO", request.getRequestURI());
    }

    public static Filter cambioPassword() {
        return new FiltroCambioPassword();
    }
}
