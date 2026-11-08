package cl.activofijo.auth;

import cl.activofijo.auditoria.AuditoriaService;
import cl.activofijo.auditoria.TipoEvento;
import cl.activofijo.comun.error.ErrorApi;
import cl.activofijo.comun.rut.Rut;
import cl.activofijo.usuario.PoliticaPassword;
import cl.activofijo.usuario.Usuario;
import cl.activofijo.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    static final String MSG_CREDENCIALES = "RUN o contraseña incorrectos";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;
    private final Clock clock;
    /** Hash ficticio para que un RUN inexistente tarde lo mismo que uno existente. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, AuditoriaService auditoria, Clock clock) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.auditoria = auditoria;
        this.clock = clock;
        this.hashFicticio = encoder.encode("hash-ficticio-para-tiempo-constante1");
    }

    /**
     * Verifica RUN y contraseña. Los intentos fallidos y el bloqueo se guardan aunque se lance
     * el error ({@code noRollbackFor}).
     */
    @Transactional(noRollbackFor = ErrorApi.class)
    public UsuarioSesion autenticar(String rutIngresado, String password) {
        Instant ahora = Instant.now(clock);
        String rut = Rut.normalizar(rutIngresado).orElse(null);
        Usuario usuario = rut == null ? null : usuarios.findByRut(rut).orElse(null);

        if (usuario == null) {
            encoder.matches(password, hashFicticio);
            auditoria.registrar(TipoEvento.LOGIN_FALLIDO, null, recortar(rut != null ? rut : rutIngresado),
                    "RUN no registrado");
            throw credencialesInvalidas();
        }
        if (usuario.estaBloqueado(ahora)) {
            auditoria.registrar(TipoEvento.LOGIN_BLOQUEADO, usuario.getId(), usuario.getRut(), "Intento con cuenta bloqueada");
            throw cuentaBloqueada(usuario, ahora);
        }
        if (!encoder.matches(password, usuario.getPasswordHash())) {
            usuario.registrarIntentoFallido(ahora);
            auditoria.registrar(TipoEvento.LOGIN_FALLIDO, usuario.getId(), usuario.getRut(), "Contraseña incorrecta");
            if (usuario.estaBloqueado(ahora)) {
                auditoria.registrar(TipoEvento.LOGIN_BLOQUEADO, usuario.getId(), usuario.getRut(),
                        "Cuenta bloqueada tras " + Usuario.MAX_INTENTOS + " intentos fallidos");
                throw cuentaBloqueada(usuario, ahora);
            }
            throw credencialesInvalidas();
        }
        if (!usuario.isActivo()) {
            auditoria.registrar(TipoEvento.LOGIN_FALLIDO, usuario.getId(), usuario.getRut(), "Usuario desactivado");
            throw new ErrorApi(HttpStatus.UNAUTHORIZED, "USUARIO_INACTIVO",
                    "Su usuario está desactivado. Contacte al administrador.");
        }
        usuario.registrarAccesoExitoso(ahora);
        auditoria.registrar(TipoEvento.LOGIN_OK, usuario.getId(), usuario.getRut(), null);
        return UsuarioSesion.de(usuario);
    }

    /** Cambio de contraseña del propio usuario. Devuelve el usuario de sesión actualizado. */
    @Transactional
    public UsuarioSesion cambiarPassword(Long usuarioId, String actual, String nueva) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ErrorApi(HttpStatus.UNAUTHORIZED, "SESION_INVALIDA", "La sesión ya no es válida"));
        if (!encoder.matches(actual, usuario.getPasswordHash())) {
            throw ErrorApi.reglaNegocio("PASSWORD_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
        }
        PoliticaPassword.validar(nueva);
        if (encoder.matches(nueva, usuario.getPasswordHash())) {
            throw ErrorApi.reglaNegocio("PASSWORD_REPETIDA", "La nueva contraseña debe ser distinta de la actual");
        }
        usuario.cambiarPassword(encoder.encode(nueva), false);
        auditoria.registrar(TipoEvento.CAMBIO_PASSWORD, usuario.getId(), usuario.getRut(), null);
        return UsuarioSesion.de(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerVigente(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ErrorApi(HttpStatus.UNAUTHORIZED, "SESION_INVALIDA", "La sesión ya no es válida"));
    }

    private static ErrorApi credencialesInvalidas() {
        return new ErrorApi(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", MSG_CREDENCIALES);
    }

    private static ErrorApi cuentaBloqueada(Usuario u, Instant ahora) {
        long minutos = Math.max(1, Duration.between(ahora, u.getBloqueadoHasta()).toMinutes() + 1);
        return new ErrorApi(HttpStatus.UNAUTHORIZED, "CUENTA_BLOQUEADA",
                "Cuenta bloqueada temporalmente por intentos fallidos. Intente nuevamente en " + minutos + " minuto"
                        + (minutos == 1 ? "" : "s") + ".");
    }

    private static String recortar(String texto) {
        return texto == null || texto.length() <= 12 ? texto : texto.substring(0, 12);
    }
}
