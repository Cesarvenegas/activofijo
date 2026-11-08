package cl.activofijo.comun.error;

/** Se violó una regla de negocio (ej. bien ya incluido en otro movimiento pendiente). Responde 422. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
