package cl.activofijo.usuario;

import cl.activofijo.ApiIT;
import cl.activofijo.comun.rut.Rut;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioIT extends ApiIT {

    @Test
    void soloAdminAccedeALaAdministracionDeUsuarios() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios").cookie(login(crearUsuario(Rol.CONSULTA))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mvc.perform(get("/api/v1/usuarios").cookie(login(crearUsuario(Rol.OPERADOR))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").cookie(login(crearUsuario(Rol.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void crearUsuarioEntregaClaveTemporalQueObligaACambiarla() throws Exception {
        Cookie admin = login(crearUsuario(Rol.ADMIN));
        String rut = nuevoRut();

        String json = crear(admin, Rut.formatear(rut), "María Pérez", "ADMIN")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rut").value(rut))
                .andExpect(jsonPath("$.usuario.debeCambiarPassword").value(true))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String temporal = JsonPath.read(json, "$.passwordTemporal");

        // Con la clave temporal puede entrar, pero no usar el sistema hasta cambiarla
        Cookie nueva = login(rut, temporal);
        mvc.perform(get("/api/v1/usuarios").cookie(nueva))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("DEBE_CAMBIAR_PASSWORD"));
        mvc.perform(get("/api/v1/auth/yo").cookie(nueva))
                .andExpect(jsonPath("$.debeCambiarPassword").value(true));

        mvc.perform(post("/api/v1/auth/cambiar-password").with(csrf()).cookie(nueva)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"" + temporal + "\",\"passwordNueva\":\"MiClave2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(false));
        mvc.perform(get("/api/v1/usuarios").cookie(nueva)).andExpect(status().isOk());
    }

    @Test
    void validaRunYDatosAlCrear() throws Exception {
        Cookie admin = login(crearUsuario(Rol.ADMIN));

        crear(admin, "12.345.678-9", "Juan", "OPERADOR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'rut')].mensaje").value(hasItem("RUN inválido")));
        crear(admin, nuevoRut(), "", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("nombre")))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("rol")));

        Usuario existente = crearUsuario(Rol.CONSULTA);
        crear(admin, Rut.formatear(existente.getRut()), "Duplicado", "CONSULTA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUT_DUPLICADO"));
    }

    @Test
    void listadoFiltraPorTextoRolYActivo() throws Exception {
        Cookie admin = login(crearUsuario(Rol.ADMIN));
        Usuario buscado = crearUsuario(Rol.CONSULTA);
        String marca = "Zapallar" + buscado.getId();
        buscado.actualizarDatos(marca + " Rojas", null, null, null, Rol.CONSULTA);
        usuarios.save(buscado);

        mvc.perform(get("/api/v1/usuarios").param("texto", marca.toLowerCase()).cookie(admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rut").value(buscado.getRut()));
        // Por RUN escrito con puntos
        mvc.perform(get("/api/v1/usuarios").param("texto", Rut.formatear(buscado.getRut())).cookie(admin))
                .andExpect(jsonPath("$.content[*].rut").value(hasItem(buscado.getRut())));
        mvc.perform(get("/api/v1/usuarios").param("rol", "CONSULTA").param("size", "200").cookie(admin))
                .andExpect(jsonPath("$.content[*].rol").value(everyItem(is("CONSULTA"))));
        mvc.perform(get("/api/v1/usuarios").param("activo", "true").param("size", "200").cookie(admin))
                .andExpect(jsonPath("$.content[*].activo").value(everyItem(is(true))));
        mvc.perform(get("/api/v1/usuarios").param("sort", "passwordHash,asc").cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ORDEN_INVALIDO"));
    }

    @Test
    void editarCambiaDatosYElCambioDeRolCierraSusSesiones() throws Exception {
        Cookie admin = login(crearUsuario(Rol.ADMIN));
        Usuario u = crearUsuario(Rol.OPERADOR);
        Cookie sesionDelUsuario = login(u);

        editar(admin, u.getId(), "Nombre Editado", "CONSULTA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre Editado"))
                .andExpect(jsonPath("$.rol").value("CONSULTA"));

        mvc.perform(get("/api/v1/auth/yo").cookie(sesionDelUsuario)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminNoPuedeDesactivarseNiQuitarseElRol() throws Exception {
        Usuario admin = crearUsuario(Rol.ADMIN);
        Cookie sesion = login(admin);

        activo(sesion, admin.getId(), false)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("AUTO_DESACTIVAR"));
        editar(sesion, admin.getId(), "Yo mismo", "OPERADOR")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("AUTO_QUITAR_ADMIN"));
    }

    @Test
    void desactivarCierraSusSesionesYNoPuedeVolverAEntrar() throws Exception {
        Cookie admin = login(crearUsuario(Rol.ADMIN));
        Usuario u = crearUsuario(Rol.OPERADOR);
        Cookie sesionDelUsuario = login(u);

        activo(admin, u.getId(), false).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/v1/auth/yo").cookie(sesionDelUsuario)).andExpect(status().isUnauthorized());
        intentarLogin(u.getRut(), CLAVE).andExpect(jsonPath("$.codigo").value("USUARIO_INACTIVO"));

        activo(admin, u.getId(), true).andExpect(jsonPath("$.activo").value(true));
        intentarLogin(u.getRut(), CLAVE).andExpect(status().isOk());
    }

    @Test
    void resetearClaveDesbloqueaYObligaACambiarla() throws Exception {
        Cookie admin = login(crearUsuario(Rol.ADMIN));
        Usuario u = crearUsuario(Rol.OPERADOR);
        for (int i = 0; i < 5; i++) {
            intentarLogin(u.getRut(), "malaClave" + i);
        }
        intentarLogin(u.getRut(), CLAVE).andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));
        mvc.perform(get("/api/v1/usuarios/" + u.getId()).cookie(admin)).andExpect(jsonPath("$.bloqueado").value(true));

        String json = mvc.perform(post("/api/v1/usuarios/" + u.getId() + "/resetear-password").with(csrf()).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.bloqueado").value(false))
                .andExpect(jsonPath("$.usuario.debeCambiarPassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String temporal = JsonPath.read(json, "$.passwordTemporal");

        intentarLogin(u.getRut(), CLAVE).andExpect(status().isUnauthorized());
        intentarLogin(u.getRut(), temporal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(true));
    }

    @Test
    void usuarioInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/v1/usuarios/987654321").cookie(login(crearUsuario(Rol.ADMIN))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuario con id 987654321 no existe"));
    }

    private ResultActions crear(Cookie sesion, String rut, String nombre, String rol) throws Exception {
        String rolJson = rol == null ? "null" : "\"" + rol + "\"";
        return mvc.perform(post("/api/v1/usuarios").with(csrf()).cookie(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rut\":\"" + rut + "\",\"nombre\":\"" + nombre + "\",\"email\":\"maria@ejemplo.cl\","
                        + "\"cargo\":\"Encargada de inventario\",\"rol\":" + rolJson + "}"));
    }

    private ResultActions editar(Cookie sesion, Long id, String nombre, String rol) throws Exception {
        return mvc.perform(put("/api/v1/usuarios/" + id).with(csrf()).cookie(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"rol\":\"" + rol + "\"}"));
    }

    private ResultActions activo(Cookie sesion, Long id, boolean activo) throws Exception {
        return mvc.perform(patch("/api/v1/usuarios/" + id + "/activo").with(csrf()).cookie(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":" + activo + "}"));
    }
}
