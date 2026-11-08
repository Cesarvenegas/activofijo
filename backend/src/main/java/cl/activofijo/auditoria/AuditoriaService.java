package cl.activofijo.auditoria;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;

/** Registra eventos de auditoría. Se une a la transacción en curso (o abre una si no hay). */
@Service
public class AuditoriaService {

    private final EventoAuditoriaRepository repository;
    private final Clock clock;

    public AuditoriaService(EventoAuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void registrar(TipoEvento tipo, Long usuarioId, String rut, String detalle) {
        repository.save(new EventoAuditoria(tipo, usuarioId, rut, detalle, ipActual(), Instant.now(clock)));
    }

    private static String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
