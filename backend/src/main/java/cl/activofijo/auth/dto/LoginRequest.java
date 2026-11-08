package cl.activofijo.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** El RUN se acepta en cualquier formato (con o sin puntos/guion). */
public record LoginRequest(
        @NotBlank(message = "Ingrese su RUN") String rut,
        @NotBlank(message = "Ingrese su contraseña") String password) {

    @Override
    public String toString() {
        return "LoginRequest[rut=" + rut + "]";   // nunca loguear la contraseña
    }
}
