package cl.activofijo.comun.error;

/** El recurso pedido no existe (o no pertenece a la empresa del usuario). Responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException de(String recurso, Object id) {
        return new RecursoNoEncontradoException(recurso + " con id " + id + " no existe");
    }
}
