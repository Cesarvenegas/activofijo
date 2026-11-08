package cl.activofijo.salud;

import cl.activofijo.ApiIT;
import cl.activofijo.usuario.Rol;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contexto completo contra PostgreSQL real (Testcontainers) con Flyway aplicado. */
class SaludControllerIT extends ApiIT {

    @Test
    void saludEsPublicaYRespondeOkConVersionDeBaseDeDatos() throws Exception {
        mvc.perform(get("/api/v1/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicacion").value("kromas-inventa-gov"))
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.baseDatos.estado").value("OK"))
                .andExpect(jsonPath("$.baseDatos.version").value(startsWith("PostgreSQL 17")))
                .andExpect(jsonPath("$.version").isNotEmpty());
    }

    @Test
    void rutaInexistenteDevuelveProblemDetailEnEspanol() throws Exception {
        mvc.perform(get("/api/v1/no-existe").cookie(login(crearUsuario(Rol.CONSULTA))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("No encontrado"))
                .andExpect(jsonPath("$.detail").value("El recurso solicitado no existe."));
    }

    @Test
    void sinSesionLaApiResponde401() throws Exception {
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
