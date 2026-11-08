package cl.activofijo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarPasswordRequest(
        @NotBlank(message = "Ingrese su contraseña actual") String passwordActual,
        @NotBlank(message = "Ingrese la nueva contraseña") String passwordNueva) {

    @Override
    public String toString() {
        return "CambiarPasswordRequest[***]";
    }
}
