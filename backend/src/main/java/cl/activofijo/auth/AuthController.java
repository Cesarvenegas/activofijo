package cl.activofijo.auth;

import cl.activofijo.auth.dto.CambiarPasswordRequest;
import cl.activofijo.auth.dto.LoginRequest;
import cl.activofijo.auth.dto.UsuarioActualDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login con RUN + contraseña y sesión en cookie {@code HttpOnly}. El logout lo atiende Spring
 * Security en {@code POST /api/v1/auth/logout} (ver SeguridadConfig).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository contextRepository;
    private final CsrfAuthenticationStrategy csrfStrategy;
    private final ChangeSessionIdAuthenticationStrategy fijacionSesion = new ChangeSessionIdAuthenticationStrategy();
    private final SesionesUsuario sesiones;

    public AuthController(AuthService authService, SecurityContextRepository contextRepository,
                          CsrfTokenRepository csrfTokenRepository, SesionesUsuario sesiones) {
        this.authService = authService;
        this.contextRepository = contextRepository;
        this.csrfStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
        this.sesiones = sesiones;
    }

    /** Entrega la cookie XSRF-TOKEN. El frontend la llama al iniciar, antes del login. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();   // fuerza la generación de la cookie
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public UsuarioActualDto login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                                  HttpServletResponse response) {
        UsuarioSesion usuario = authService.autenticar(req.rut(), req.password());
        Authentication auth = usuario.comoAutenticacion();
        // Nuevo id de sesión (evita fijación de sesión) y nuevo token CSRF tras autenticarse
        fijacionSesion.onAuthentication(auth, request, response);
        csrfStrategy.onAuthentication(auth, request, response);
        guardarEnSesion(auth, request, response);
        cargarCsrf(request);
        return UsuarioActualDto.de(authService.obtenerVigente(usuario.id()));
    }

    @GetMapping("/yo")
    public UsuarioActualDto yo(@AuthenticationPrincipal UsuarioSesion actual) {
        return UsuarioActualDto.de(authService.obtenerVigente(actual.id()));
    }

    @PostMapping("/cambiar-password")
    public UsuarioActualDto cambiarPassword(@AuthenticationPrincipal UsuarioSesion actual,
                                            @Valid @RequestBody CambiarPasswordRequest req,
                                            HttpServletRequest request, HttpServletResponse response) {
        UsuarioSesion actualizado = authService.cambiarPassword(actual.id(), req.passwordActual(), req.passwordNueva());
        guardarEnSesion(actualizado.comoAutenticacion(), request, response);
        // Las demás sesiones abiertas de este usuario (otros equipos) se cierran
        sesiones.cerrarTodas(actual.rut(), request.getSession().getId());
        return UsuarioActualDto.de(authService.obtenerVigente(actual.id()));
    }

    private void guardarEnSesion(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    private static void cargarCsrf(HttpServletRequest request) {
        if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
    }
}
