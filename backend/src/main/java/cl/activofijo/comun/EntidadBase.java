package cl.activofijo.comun;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base de las entidades de negocio: id identity + columnas de auditoría
 * ({@code creado_en/por}, {@code modificado_en/por}) completadas por Spring Data JPA Auditing.
 * El "por" guarda el RUN del usuario en sesión.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @CreatedBy
    @Column(name = "creado_por", updatable = false)
    private String creadoPor;

    @LastModifiedDate
    @Column(name = "modificado_en")
    private Instant modificadoEn;

    @LastModifiedBy
    @Column(name = "modificado_por")
    private String modificadoPor;

    public Long getId() {
        return id;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public Instant getModificadoEn() {
        return modificadoEn;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }
}
