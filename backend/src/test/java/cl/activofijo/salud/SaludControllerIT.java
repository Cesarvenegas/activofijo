package cl.activofijo.salud;

import cl.activofijo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Levanta el contexto completo contra un PostgreSQL real (Testcontainers) con Flyway aplicado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SaludControllerIT {

    @Autowired
    MockMvc mvc;

    @Test
    void saludRespondeOkConVersionDeBaseDeDatos() throws Exception {
        mvc.perform(get("/api/v1/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicacion").value("kromas-inventa-gov"))
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.baseDatos.estado").value("OK"))
                .andExpect(jsonPath("$.baseDatos.version").value(org.hamcrest.Matchers.startsWith("PostgreSQL 17")))
                .andExpect(jsonPath("$.version").isNotEmpty());
    }

    @Test
    void rutaInexistenteDevuelveProblemDetailEnEspanol() throws Exception {
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("No encontrado"))
                .andExpect(jsonPath("$.detail").value("El recurso solicitado no existe."));
    }
}
