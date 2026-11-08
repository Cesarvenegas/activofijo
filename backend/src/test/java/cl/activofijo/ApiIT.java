package cl.activofijo;

import cl.activofijo.comun.rut.Rut;
import cl.activofijo.usuario.Rol;
import cl.activofijo.usuario.Usuario;
import cl.activofijo.usuario.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de API: contexto completo + PostgreSQL real (Testcontainers) + MockMvc
 * con la cadena de seguridad y Spring Session. El contexto se comparte entre clases; cada test
 * usa RUN propios para no interferir con los demás.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ApiIT.RelojConfig.class})
public abstract class ApiIT {

    protected static final String CLAVE = "Clave1234";
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UsuarioRepository usuarios;
    @Autowired
    protected PasswordEncoder encoder;
    @Autowired
    protected RelojPrueba reloj;

    @AfterEach
    void restaurarReloj() {
        reloj.reiniciar();
    }

    /** RUN válido y único (la BD de Testcontainers es nueva en cada ejecución). */
    protected static String nuevoRut() {
        int cuerpo = 20_000_000 + SECUENCIA.incrementAndGet();
        return cuerpo + "-" + Rut.calcularDv(cuerpo);
    }

    /** Crea un usuario activo con clave {@link #CLAVE} que no necesita cambiarla. */
    protected Usuario crearUsuario(Rol rol) {
        Usuario u = new Usuario(nuevoRut(), "Usuario " + rol, rol, encoder.encode(CLAVE));
        u.cambiarPassword(u.getPasswordHash(), false);
        return usuarios.save(u);
    }

    protected ResultActions intentarLogin(String rut, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rut\":\"" + rut + "\",\"password\":\"" + password + "\"}"));
    }

    /** Hace login y devuelve la cookie de sesión. */
    protected Cookie login(String rut, String password) throws Exception {
        MvcResult r = intentarLogin(rut, password).andExpect(status().isOk()).andReturn();
        Cookie sesion = r.getResponse().getCookie("SESSION");
        assertThat(sesion).as("cookie SESSION").isNotNull();
        return sesion;
    }

    protected Cookie login(Usuario u) throws Exception {
        return login(u.getRut(), CLAVE);
    }

    /** Reloj del sistema que los tests pueden adelantar (p. ej. para que expire un bloqueo). */
    public static class RelojPrueba extends Clock {
        private Duration desfase = Duration.ZERO;

        public void adelantar(Duration d) {
            desfase = desfase.plus(d);
        }

        void reiniciar() {
            desfase = Duration.ZERO;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.now().plus(desfase);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RelojConfig {
        @Bean
        @Primary
        RelojPrueba relojPrueba() {
            return new RelojPrueba();
        }
    }
}
