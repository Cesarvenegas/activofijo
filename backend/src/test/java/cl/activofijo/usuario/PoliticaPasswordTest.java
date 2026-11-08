package cl.activofijo.usuario;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaPasswordTest {

    @ParameterizedTest
    @ValueSource(strings = {"Kromas2026", "abc12345", "12345abc", "ñandú2026"})
    void aceptaClavesConLetrasYNumerosDe8OMas(String clave) {
        assertThat(PoliticaPassword.cumple(clave)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc1234", "solotexto", "12345678"})
    void rechazaClavesDebiles(String clave) {
        assertThat(PoliticaPassword.cumple(clave)).isFalse();
    }

    @RepeatedTest(100)
    void laClaveTemporalCumpleLaPolitica() {
        String temporal = PoliticaPassword.generarTemporal();
        assertThat(temporal).hasSize(10);
        assertThat(PoliticaPassword.cumple(temporal)).as(temporal).isTrue();
    }
}
