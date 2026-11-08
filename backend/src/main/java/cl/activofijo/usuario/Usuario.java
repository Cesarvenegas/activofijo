package cl.activofijo.usuario;

import cl.activofijo.comun.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "usuario")
public class Usuario extends EntidadBase {

    /** Intentos fallidos consecutivos que provocan el bloqueo temporal. */
    public static final int MAX_INTENTOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    /** RUN normalizado ({@code 12345678-K}); es el identificador de login. No se modifica. */
    @Column(nullable = false, updatable = false, length = 12)
    private String rut;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String email;

    @Column(name = "telefono_anexo", length = 30)
    private String telefonoAnexo;

    @Column(length = 100)
    private String cargo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    protected Usuario() {
    }

    public Usuario(String rut, String nombre, Rol rol, String passwordHash) {
        this.rut = rut;
        this.nombre = nombre;
        this.rol = rol;
        this.passwordHash = passwordHash;
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Registra un intento fallido; al llegar al máximo bloquea la cuenta y reinicia el contador. */
    public void registrarIntentoFallido(Instant ahora) {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS) {
            bloqueadoHasta = ahora.plus(DURACION_BLOQUEO);
            intentosFallidos = 0;
        }
    }

    public void registrarAccesoExitoso(Instant ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = ahora;
    }

    /** Cambia la contraseña. {@code temporal} obliga a cambiarla en el próximo ingreso. */
    public void cambiarPassword(String nuevoHash, boolean temporal) {
        this.passwordHash = nuevoHash;
        this.debeCambiarPassword = temporal;
        if (temporal) {
            // Un reseteo hecho por el administrador también desbloquea la cuenta
            this.intentosFallidos = 0;
            this.bloqueadoHasta = null;
        }
    }

    public void actualizarDatos(String nombre, String email, String telefonoAnexo, String cargo, Rol rol) {
        this.nombre = nombre;
        this.email = email;
        this.telefonoAnexo = telefonoAnexo;
        this.cargo = cargo;
        this.rol = rol;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getRut() {
        return rut;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefonoAnexo() {
        return telefonoAnexo;
    }

    public String getCargo() {
        return cargo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public Instant getUltimoAcceso() {
        return ultimoAcceso;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
