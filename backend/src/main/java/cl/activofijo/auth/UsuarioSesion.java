package cl.activofijo.auth;

import cl.activofijo.usuario.Rol;
import cl.activofijo.usuario.Usuario;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Usuario autenticado guardado en la sesión (Spring Session JDBC lo serializa, por eso es
 * {@link Serializable}). {@link #getName()} devuelve el RUN: Spring Session lo indexa como
 * {@code principal_name}, lo que permite cerrar todas las sesiones de un usuario.
 */
public record UsuarioSesion(Long id, String rut, String nombre, Rol rol, boolean debeCambiarPassword)
        implements AuthenticatedPrincipal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static UsuarioSesion de(Usuario u) {
        return new UsuarioSesion(u.getId(), u.getRut(), u.getNombre(), u.getRol(), u.isDebeCambiarPassword());
    }

    @Override
    public String getName() {
        return rut;
    }

    public Authentication comoAutenticacion() {
        return UsernamePasswordAuthenticationToken.authenticated(this, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
    }
}
