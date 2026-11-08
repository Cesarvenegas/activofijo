package cl.activofijo.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

    List<EventoAuditoria> findByRutOrderByIdAsc(String rut);
}
