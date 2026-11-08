package cl.activofijo.salud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Caso sin base de datos: responde 503 con un mensaje claro, no un error 500. */
class SaludControllerTest {

    @Test
    void baseDeDatosCaidaResponde503ConMensaje() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(String.class)))
                .thenThrow(new CannotGetJdbcConnectionException("Connection refused"));
        var sinBuildInfo = new StaticListableBeanFactory().getBeanProvider(BuildProperties.class);

        var respuesta = new SaludController(jdbc, sinBuildInfo, "kromas-inventa-gov").salud();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().estado()).isEqualTo(SaludDto.Estado.ERROR);
        assertThat(respuesta.getBody().baseDatos().mensaje()).isEqualTo("No se pudo conectar con la base de datos");
        assertThat(respuesta.getBody().version()).isEqualTo("desarrollo");
    }
}
