package cl.activofijo.usuario.dto;

/**
 * Resultado de crear un usuario o resetear su clave. La clave temporal se entrega una sola vez:
 * no se guarda en texto plano ni se puede volver a consultar.
 */
public record PasswordTemporalDto(UsuarioDto usuario, String passwordTemporal) {
}
