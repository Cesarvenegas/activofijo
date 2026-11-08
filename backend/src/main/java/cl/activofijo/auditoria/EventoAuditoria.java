package cl.activofijo.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Evento de seguridad o administración (login, cambio de clave, alta de usuario...). Solo inserción. */
@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private TipoEvento tipo;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(length = 12, updatable = false)
    private String rut;

    @Column(length = 500, updatable = false)
    private String detalle;

    @Column(length = 45, updatable = false)
    private String ip;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    protected EventoAuditoria() {
    }

    public EventoAuditoria(TipoEvento tipo, Long usuarioId, String rut, String detalle, String ip, Instant fecha) {
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.rut = rut;
        this.detalle = detalle == null || detalle.length() <= 500 ? detalle : detalle.substring(0, 500);
        this.ip = ip;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getRut() {
        return rut;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getIp() {
        return ip;
    }

    public Instant getFecha() {
        return fecha;
    }
}
