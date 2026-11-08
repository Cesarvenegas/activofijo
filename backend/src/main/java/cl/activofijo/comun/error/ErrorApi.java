package cl.activofijo.comun.error;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con estado HTTP y código estable que el frontend puede interpretar
 * (ej. {@code CUENTA_BLOQUEADA}, {@code DEBE_CAMBIAR_PASSWORD}). Se responde como ProblemDetail
 * con la propiedad {@code codigo}.
 */
public class ErrorApi extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ErrorApi(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public static ErrorApi conflicto(String codigo, String mensaje) {
        return new ErrorApi(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public static ErrorApi reglaNegocio(String codigo, String mensaje) {
        return new ErrorApi(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensaje);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
