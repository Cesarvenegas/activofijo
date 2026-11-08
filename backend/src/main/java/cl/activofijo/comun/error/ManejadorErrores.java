package cl.activofijo.comun.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Manejador global de errores. Todas las respuestas de error son {@link ProblemDetail} (RFC 9457)
 * con título y detalle en español.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final Map<Integer, String> TITULOS = Map.of(
            400, "Solicitud inválida",
            401, "No autenticado",
            403, "Acceso denegado",
            404, "No encontrado",
            405, "Método no permitido",
            409, "Conflicto",
            415, "Tipo de contenido no soportado",
            422, "Regla de negocio no cumplida",
            500, "Error interno",
            503, "Servicio no disponible");

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ErrorApi.class)
    ResponseEntity<ProblemDetail> errorApi(ErrorApi ex) {
        ProblemDetail pd = problema(ex.getStatus(), ex.getMessage());
        pd.setProperty("codigo", ex.getCodigo());
        return ResponseEntity.status(ex.getStatus()).body(pd);
    }

    /** Seguridad a nivel de método: se responde igual que en los filtros. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesoDenegado(AccessDeniedException ex) {
        ProblemDetail pd = problema(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación.");
        pd.setProperty("codigo", "SIN_PERMISO");
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail noAutenticado(AuthenticationException ex) {
        ProblemDetail pd = problema(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para continuar.");
        pd.setProperty("codigo", "NO_AUTENTICADO");
        return pd;
    }

    /** Orden (?sort=) por una propiedad que no existe. */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail propiedadInvalida(PropertyReferenceException ex) {
        return problema(HttpStatus.BAD_REQUEST, "No se puede ordenar ni filtrar por '" + ex.getPropertyName() + "'");
    }

    /** Restricción de BD violada (normalmente un duplicado que la validación previa no alcanzó a ver). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes (posible duplicado).");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Si persiste, contacte al administrador.");
    }

    /** Errores de Bean Validation: devuelve la lista de campos con su mensaje. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Hay datos con errores. Revise los campos indicados.");
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(),
                        "mensaje", e.getDefaultMessage() == null ? "valor inválido" : e.getDefaultMessage()))
                .toList();
        pd.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(pd);
    }

    /** Traduce al español el título y detalle de los errores estándar de Spring MVC. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (respuesta != null && respuesta.getBody() instanceof ProblemDetail pd) {
            pd.setTitle(titulo(statusCode.value()));
            pd.setDetail(detalleEstandar(statusCode.value()));
        }
        return respuesta;
    }

    private static ProblemDetail problema(HttpStatus status, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo(status.value()));
        return pd;
    }

    private static String titulo(int status) {
        return TITULOS.getOrDefault(status, "Error");
    }

    private static String detalleEstandar(int status) {
        return switch (status) {
            case 400 -> "La solicitud tiene un formato inválido.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "La operación no está permitida sobre este recurso.";
            case 415 -> "El tipo de contenido enviado no es soportado.";
            default -> "No se pudo procesar la solicitud.";
        };
    }
}
