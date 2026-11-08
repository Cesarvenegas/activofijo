package cl.activofijo.salud;

import java.time.OffsetDateTime;

/** Respuesta de {@code GET /api/v1/salud}. */
public record SaludDto(
        String aplicacion,
        String version,
        Estado estado,
        BaseDatos baseDatos,
        OffsetDateTime fecha) {

    public enum Estado { OK, ERROR }

    /** Estado de la base de datos; {@code version} y {@code mensaje} son excluyentes. */
    public record BaseDatos(Estado estado, String version, String mensaje) {
    }
}
