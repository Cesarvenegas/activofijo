package cl.activofijo.usuario;

import cl.activofijo.auditoria.AuditoriaService;
import cl.activofijo.auditoria.TipoEvento;
import cl.activofijo.auth.SesionesUsuario;
import cl.activofijo.auth.UsuarioSesion;
import cl.activofijo.comun.PaginaDto;
import cl.activofijo.comun.error.ErrorApi;
import cl.activofijo.comun.error.RecursoNoEncontradoException;
import cl.activofijo.comun.rut.Rut;
import cl.activofijo.usuario.dto.PasswordTemporalDto;
import cl.activofijo.usuario.dto.UsuarioCrearRequest;
import cl.activofijo.usuario.dto.UsuarioDto;
import cl.activofijo.usuario.dto.UsuarioEditarRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<String> ORDEN_PERMITIDO = Set.of("nombre", "rut", "rol", "activo", "ultimoAcceso", "creadoEn");

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;
    private final SesionesUsuario sesiones;
    private final Clock clock;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder, AuditoriaService auditoria,
                          SesionesUsuario sesiones, Clock clock) {
        this.repository = repository;
        this.encoder = encoder;
        this.auditoria = auditoria;
        this.sesiones = sesiones;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaginaDto<UsuarioDto> listar(String texto, Rol rol, Boolean activo, Pageable pageable) {
        for (Sort.Order orden : pageable.getSort()) {
            if (!ORDEN_PERMITIDO.contains(orden.getProperty())) {
                throw new ErrorApi(HttpStatus.BAD_REQUEST, "ORDEN_INVALIDO",
                        "No se puede ordenar por '" + orden.getProperty() + "'");
            }
        }
        Instant ahora = Instant.now(clock);
        return PaginaDto.de(repository.findAll(UsuarioRepository.filtro(texto, rol, activo), pageable),
                u -> UsuarioDto.de(u, ahora));
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtener(Long id) {
        return UsuarioDto.de(buscar(id), Instant.now(clock));
    }

    @Transactional
    public PasswordTemporalDto crear(UsuarioCrearRequest req) {
        String rut = Rut.normalizar(req.rut())
                .orElseThrow(() -> new ErrorApi(HttpStatus.BAD_REQUEST, "RUT_INVALIDO", "RUN inválido"));
        if (repository.existsByRut(rut)) {
            throw ErrorApi.conflicto("RUT_DUPLICADO", "Ya existe un usuario con el RUN " + Rut.formatear(rut));
        }
        String temporal = PoliticaPassword.generarTemporal();
        Usuario u = new Usuario(rut, req.nombre().trim(), req.rol(), encoder.encode(temporal));
        u.actualizarDatos(req.nombre().trim(), vacioANulo(req.email()), vacioANulo(req.telefonoAnexo()),
                vacioANulo(req.cargo()), req.rol());
        repository.save(u);
        auditoria.registrar(TipoEvento.USUARIO_CREADO, u.getId(), u.getRut(), "Rol " + u.getRol());
        return new PasswordTemporalDto(UsuarioDto.de(u, Instant.now(clock)), temporal);
    }

    @Transactional
    public UsuarioDto editar(Long id, UsuarioEditarRequest req, UsuarioSesion actual) {
        Usuario u = buscar(id);
        if (u.getId().equals(actual.id()) && req.rol() != Rol.ADMIN) {
            throw ErrorApi.reglaNegocio("AUTO_QUITAR_ADMIN", "No puede quitarse a sí mismo el rol de administrador");
        }
        Rol rolAnterior = u.getRol();
        u.actualizarDatos(req.nombre().trim(), vacioANulo(req.email()), vacioANulo(req.telefonoAnexo()),
                vacioANulo(req.cargo()), req.rol());
        String detalle = rolAnterior != req.rol() ? "Rol " + rolAnterior + " → " + req.rol() : null;
        auditoria.registrar(TipoEvento.USUARIO_MODIFICADO, u.getId(), u.getRut(), detalle);
        if (rolAnterior != req.rol()) {
            sesiones.cerrarTodas(u.getRut(), null);   // el nuevo rol se aplica de inmediato
        }
        return UsuarioDto.de(u, Instant.now(clock));
    }

    @Transactional
    public UsuarioDto cambiarActivo(Long id, boolean activo, UsuarioSesion actual) {
        Usuario u = buscar(id);
        if (u.getId().equals(actual.id()) && !activo) {
            throw ErrorApi.reglaNegocio("AUTO_DESACTIVAR", "No puede desactivar su propio usuario");
        }
        if (u.isActivo() != activo) {
            u.setActivo(activo);
            auditoria.registrar(activo ? TipoEvento.USUARIO_ACTIVADO : TipoEvento.USUARIO_DESACTIVADO,
                    u.getId(), u.getRut(), null);
            if (!activo) {
                sesiones.cerrarTodas(u.getRut(), null);
            }
        }
        return UsuarioDto.de(u, Instant.now(clock));
    }

    /** Genera una clave temporal (se muestra una vez), desbloquea la cuenta y cierra sus sesiones. */
    @Transactional
    public PasswordTemporalDto resetearPassword(Long id) {
        Usuario u = buscar(id);
        String temporal = PoliticaPassword.generarTemporal();
        u.cambiarPassword(encoder.encode(temporal), true);
        auditoria.registrar(TipoEvento.RESET_PASSWORD, u.getId(), u.getRut(), null);
        sesiones.cerrarTodas(u.getRut(), null);
        return new PasswordTemporalDto(UsuarioDto.de(u, Instant.now(clock)), temporal);
    }

    private Usuario buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
