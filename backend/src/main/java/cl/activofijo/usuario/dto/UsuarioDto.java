package cl.activofijo.usuario.dto;

import cl.activofijo.comun.rut.Rut;
import cl.activofijo.usuario.Rol;
import cl.activofijo.usuario.Usuario;

import java.time.Instant;

/** Usuario para listados y formularios (nunca incluye el hash de la contraseña). */
public record UsuarioDto(
        Long id,
        String rut,
        String rutFormateado,
        String nombre,
        String email,
        String telefonoAnexo,
        String cargo,
        Rol rol,
        boolean activo,
        boolean debeCambiarPassword,
        boolean bloqueado,
        Instant ultimoAcceso,
        Instant creadoEn) {

    public static UsuarioDto de(Usuario u, Instant ahora) {
        return new UsuarioDto(u.getId(), u.getRut(), Rut.formatear(u.getRut()), u.getNombre(), u.getEmail(),
                u.getTelefonoAnexo(), u.getCargo(), u.getRol(), u.isActivo(), u.isDebeCambiarPassword(),
                u.estaBloqueado(ahora), u.getUltimoAcceso(), u.getCreadoEn());
    }
}
