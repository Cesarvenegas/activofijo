package cl.activofijo.auth;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Cierra sesiones de un usuario (al desactivarlo, cambiar su rol o resetear su clave), para que
 * los cambios de permisos se apliquen de inmediato y no al próximo login.
 */
@Component
public class SesionesUsuario {

    private final FindByIndexNameSessionRepository<? extends Session> sesiones;

    public SesionesUsuario(FindByIndexNameSessionRepository<? extends Session> sesiones) {
        this.sesiones = sesiones;
    }

    /** Cierra todas las sesiones del RUN, excepto la indicada (puede ser {@code null}). */
    public int cerrarTodas(String rut, String exceptoSessionId) {
        var abiertas = sesiones.findByPrincipalName(rut);
        int cerradas = 0;
        for (String id : abiertas.keySet()) {
            if (!id.equals(exceptoSessionId)) {
                sesiones.deleteById(id);
                cerradas++;
            }
        }
        return cerradas;
    }
}
