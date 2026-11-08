package cl.activofijo.usuario.dto;

import cl.activofijo.usuario.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición de un usuario. El RUN no se modifica (es el login). */
public record UsuarioEditarRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @Email(message = "Correo electrónico inválido") @Size(max = 150) String email,
        @Size(max = 30) String telefonoAnexo,
        @Size(max = 100) String cargo,
        @NotNull(message = "El rol es obligatorio") Rol rol) {
}
