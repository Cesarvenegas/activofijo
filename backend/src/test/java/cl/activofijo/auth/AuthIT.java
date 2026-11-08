package cl.activofijo.auth;

import cl.activofijo.ApiIT;
import cl.activofijo.auditoria.EventoAuditoria;
import cl.activofijo.auditoria.EventoAuditoriaRepository;
import cl.activofijo.auditoria.TipoEvento;
import cl.activofijo.comun.rut.Rut;
import cl.activofijo.usuario.Rol;
import cl.activofijo.usuario.Usuario;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends ApiIT {

    @Autowired
    EventoAuditoriaRepository eventos;

    @Test
    void loginCorrectoCreaSesionYYoDevuelveElUsuario() throws Exception {
        Usuario u = crearUsuario(Rol.OPERADOR);

        Cookie sesion = login(u);

        mvc.perform(get("/api/v1/auth/yo").cookie(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rut").value(u.getRut()))
                .andExpect(jsonPath("$.rutFormateado").value(Rut.formatear(u.getRut())))
                .andExpect(jsonPath("$.rol").value("OPERADOR"))
                .andExpect(jsonPath("$.debeCambiarPassword").value(false))
                .andExpect(jsonPath("$.ultimoAcceso").isNotEmpty());
    }

    @Test
    void elRunSeAceptaConPuntosOSinGuion() throws Exception {
        Usuario u = crearUsuario(Rol.CONSULTA);
        String conPuntos = Rut.formatear(u.getRut());
        String sinGuion = u.getRut().replace("-", "");

        intentarLogin(conPuntos, CLAVE).andExpect(status().isOk());
        intentarLogin(sinGuion.toLowerCase(), CLAVE).andExpect(status().isOk());
    }

    @Test
    void claveIncorrectaYRunInexistenteDanElMismoMensaje() throws Exception {
        Usuario u = crearUsuario(Rol.CONSULTA);

        intentarLogin(u.getRut(), "otraClave99")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("RUN o contraseña incorrectos"));
        intentarLogin(nuevoRut(), CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("RUN o contraseña incorrectos"));
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuentaQuinceMinutos() throws Exception {
        Usuario u = crearUsuario(Rol.OPERADOR);

        for (int i = 1; i <= 4; i++) {
            intentarLogin(u.getRut(), "mala" + i + "xx").andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        }
        intentarLogin(u.getRut(), "mala5xx")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));

        // Aun con la clave correcta sigue bloqueada
        intentarLogin(u.getRut(), CLAVE).andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));

        // Pasados 15 minutos puede volver a ingresar
        reloj.adelantar(Duration.ofMinutes(16));
        intentarLogin(u.getRut(), CLAVE).andExpect(status().isOk());

        assertThat(eventos.findByRutOrderByIdAsc(u.getRut())).extracting(EventoAuditoria::getTipo)
                .contains(TipoEvento.LOGIN_FALLIDO, TipoEvento.LOGIN_BLOQUEADO, TipoEvento.LOGIN_OK);
    }

    @Test
    void usuarioDesactivadoNoPuedeIngresar() throws Exception {
        Usuario u = crearUsuario(Rol.OPERADOR);
        u.setActivo(false);
        usuarios.save(u);

        intentarLogin(u.getRut(), CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("USUARIO_INACTIVO"));
    }

    @Test
    void loginSinTokenCsrfEsRechazado() throws Exception {
        Usuario u = crearUsuario(Rol.OPERADOR);

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"" + u.getRut() + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
    }

    @Test
    void sinSesionResponde401EnEspanol() throws Exception {
        mvc.perform(get("/api/v1/auth/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void logoutCierraLaSesion() throws Exception {
        Usuario u = crearUsuario(Rol.CONSULTA);
        Cookie sesion = login(u);

        mvc.perform(post("/api/v1/auth/logout").with(csrf()).cookie(sesion)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/yo").cookie(sesion)).andExpect(status().isUnauthorized());
        assertThat(eventos.findByRutOrderByIdAsc(u.getRut())).extracting(EventoAuditoria::getTipo)
                .contains(TipoEvento.LOGOUT);
    }

    @Test
    void cambiarPasswordValidaClaveActualYPolitica() throws Exception {
        Usuario u = crearUsuario(Rol.OPERADOR);
        Cookie sesion = login(u);

        cambiar(sesion, "incorrecta1", "NuevaClave1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("PASSWORD_ACTUAL_INCORRECTA"));
        cambiar(sesion, CLAVE, "solotexto")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("PASSWORD_DEBIL"));
        cambiar(sesion, CLAVE, "corta1")
                .andExpect(jsonPath("$.codigo").value("PASSWORD_DEBIL"));
        cambiar(sesion, CLAVE, CLAVE)
                .andExpect(jsonPath("$.codigo").value("PASSWORD_REPETIDA"));

        cambiar(sesion, CLAVE, "NuevaClave1").andExpect(status().isOk());

        // La sesión actual sigue válida; la clave antigua ya no sirve y la nueva sí
        mvc.perform(get("/api/v1/auth/yo").cookie(sesion)).andExpect(status().isOk());
        intentarLogin(u.getRut(), CLAVE).andExpect(status().isUnauthorized());
        intentarLogin(u.getRut(), "NuevaClave1").andExpect(status().isOk());
    }

    @Test
    void cambiarPasswordCierraLasOtrasSesionesDelUsuario() throws Exception {
        Usuario u = crearUsuario(Rol.OPERADOR);
        Cookie equipoA = login(u);
        Cookie equipoB = login(u);

        cambiar(equipoA, CLAVE, "NuevaClave2").andExpect(status().isOk());

        mvc.perform(get("/api/v1/auth/yo").cookie(equipoA)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/yo").cookie(equipoB)).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions cambiar(Cookie sesion, String actual, String nueva)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/cambiar-password").with(csrf()).cookie(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"" + actual + "\",\"passwordNueva\":\"" + nueva + "\"}"));
    }
}
