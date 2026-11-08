package cl.activofijo.auth;

import cl.activofijo.TestcontainersConfiguration;
import cl.activofijo.usuario.Rol;
import cl.activofijo.usuario.Usuario;
import cl.activofijo.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flujo de login por HTTP real (Tomcat), como lo hace el navegador.
 * <p>
 * Usa un contexto propio <b>sin MockMvc</b>: al crear MockMvc, spring-security-test reemplaza el
 * repositorio CSRF del filtro (bean compartido) por uno de prueba basado en sesión, y entonces el
 * servidor real dejaría de emitir la cookie XSRF-TOKEN.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CookiesIT {

    private static final String CLAVE = "Clave1234";

    @LocalServerPort
    int puerto;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder encoder;

    @Test
    void flujoCompletoDelNavegadorConCookies() throws Exception {
        Usuario u = new Usuario("19999999-0", "Usuario Cookies", Rol.OPERADOR, encoder.encode(CLAVE));
        u.cambiarPassword(u.getPasswordHash(), false);
        usuarios.save(u);
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

        // 1. Obtener el token CSRF: cookie legible por JavaScript (no HttpOnly)
        HttpResponse<String> csrf = http.send(get("/api/v1/auth/csrf"), HttpResponse.BodyHandlers.ofString());
        assertThat(csrf.statusCode()).isEqualTo(204);
        assertThat(setCookies(csrf)).anySatisfy(c -> assertThat(c).startsWith("XSRF-TOKEN=").doesNotContain("HttpOnly"));
        String token = xsrf(cookies);

        // 2. Login enviando el token en la cabecera, como Angular
        HttpResponse<String> login = http.send(HttpRequest.newBuilder(url("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"rut\":\"" + u.getRut() + "\",\"password\":\"" + CLAVE + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(setCookies(login))
                .anySatisfy(c -> assertThat(c).startsWith("SESSION=").contains("HttpOnly").contains("SameSite=Lax"));
        // El token CSRF se renueva al autenticarse
        assertThat(xsrf(cookies)).isNotEqualTo(token);

        // 3. La sesión funciona en las siguientes llamadas
        HttpResponse<String> yo = http.send(get("/api/v1/auth/yo"), HttpResponse.BodyHandlers.ofString());
        assertThat(yo.statusCode()).isEqualTo(200);
        assertThat(yo.body()).contains(u.getRut());

        // 4. Un POST con el token anterior (ya rotado) es rechazado
        HttpResponse<String> conTokenViejo = http.send(HttpRequest.newBuilder(url("/api/v1/auth/logout"))
                .header("X-XSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(conTokenViejo.statusCode()).isEqualTo(403);

        // 5. Logout con el token vigente
        HttpResponse<String> logout = http.send(HttpRequest.newBuilder(url("/api/v1/auth/logout"))
                .header("X-XSRF-TOKEN", xsrf(cookies))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(http.send(get("/api/v1/auth/yo"), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    private URI url(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }

    private HttpRequest get(String ruta) {
        return HttpRequest.newBuilder(url(ruta)).GET().build();
    }

    private static List<String> setCookies(HttpResponse<?> r) {
        return r.headers().allValues("Set-Cookie");
    }

    private static String xsrf(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN") && !c.getValue().isEmpty())
                .reduce((a, b) -> b)
                .orElseThrow()
                .getValue();
    }
}
