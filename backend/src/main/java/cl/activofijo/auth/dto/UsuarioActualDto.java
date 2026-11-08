package cl.activofijo.auth.dto;

import cl.activofijo.comun.rut.Rut;
import cl.activofijo.usuario.Rol;
import cl.activofijo.usuario.Usuario;

import java.time.Instant;

/** Respuesta de login y de {@code GET /auth/yo}. */
public record UsuarioActualDto(
        Long id,
        String rut,
        String rutFormateado,
        String nombre,
        String email,
        String telefonoAnexo,
        String cargo,
        Rol rol,
        boolean debeCambiarPassword,
        Instant ultimoAcceso) {

    public static UsuarioActualDto de(Usuario u) {
        return new UsuarioActualDto(u.getId(), u.getRut(), Rut.formatear(u.getRut()), u.getNombre(), u.getEmail(),
                u.getTelefonoAnexo(), u.getCargo(), u.getRol(), u.isDebeCambiarPassword(), u.getUltimoAcceso());
    }
}
