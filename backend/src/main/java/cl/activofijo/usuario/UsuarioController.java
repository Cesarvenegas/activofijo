package cl.activofijo.usuario;

import cl.activofijo.auth.UsuarioSesion;
import cl.activofijo.comun.PaginaDto;
import cl.activofijo.usuario.dto.PasswordTemporalDto;
import cl.activofijo.usuario.dto.UsuarioCrearRequest;
import cl.activofijo.usuario.dto.UsuarioDto;
import cl.activofijo.usuario.dto.UsuarioEditarRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administración de usuarios (solo ADMIN, ver SeguridadConfig). */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaDto<UsuarioDto> listar(@RequestParam(required = false) String texto,
                                        @RequestParam(required = false) Rol rol,
                                        @RequestParam(required = false) Boolean activo,
                                        @PageableDefault(size = 50, sort = "nombre", direction = Sort.Direction.ASC)
                                        Pageable pageable) {
        return service.listar(texto, rol, activo, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PasswordTemporalDto crear(@Valid @RequestBody UsuarioCrearRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public UsuarioDto editar(@PathVariable Long id, @Valid @RequestBody UsuarioEditarRequest req,
                             @AuthenticationPrincipal UsuarioSesion actual) {
        return service.editar(id, req, actual);
    }

    @PatchMapping("/{id}/activo")
    public UsuarioDto cambiarActivo(@PathVariable Long id, @Valid @RequestBody CambioActivo req,
                                    @AuthenticationPrincipal UsuarioSesion actual) {
        return service.cambiarActivo(id, req.activo(), actual);
    }

    @PostMapping("/{id}/resetear-password")
    public PasswordTemporalDto resetearPassword(@PathVariable Long id) {
        return service.resetearPassword(id);
    }

    public record CambioActivo(@NotNull(message = "Indique si el usuario queda activo") Boolean activo) {
    }
}
