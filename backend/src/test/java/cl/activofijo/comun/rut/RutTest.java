package cl.activofijo.comun.rut;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RutTest {

    @ParameterizedTest
    @CsvSource({
            "11111111, 1",
            "12345678, 5",
            "76086428, 5",
            "10000013, K",
            "10000004, 0"
    })
    void calculaDigitoVerificador(int cuerpo, char dv) {
        assertThat(Rut.calcularDv(cuerpo)).isEqualTo(dv);
    }

    @ParameterizedTest
    @CsvSource({
            "'12.345.678-5', 12345678-5",
            "'12345678-5',   12345678-5",
            "'123456785',    12345678-5",
            "' 12.345.678-5 ', 12345678-5",
            "'10.000.013-k', 10000013-K",
            "'10000013K',    10000013-K"
    })
    void normalizaDistintasEscrituras(String entrada, String esperado) {
        assertThat(Rut.normalizar(entrada)).contains(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12.345.678-9", "abc", "1-9", "12345678-X", "12a45678-5", "999999-3", "1234567890-1"})
    void rechazaRutInvalidos(String entrada) {
        assertThat(Rut.esValido(entrada)).isFalse();
        assertThat(Rut.normalizar(entrada)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "12345678-5, 12.345.678-5",
            "10000013k,  10.000.013-K",
            "1.000.005-K, 1.000.005-K"
    })
    void formateaConPuntos(String entrada, String esperado) {
        assertThat(Rut.formatear(entrada)).isEqualTo(esperado);
    }

    @Test
    void formatearRutInvalidoLanzaExcepcion() {
        assertThatThrownBy(() -> Rut.formatear("12.345.678-9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RUT inválido");
    }

    @Test
    void validadorBeanValidationAceptaVacioYRechazaInvalido() {
        RutValido.Validador v = new RutValido.Validador();
        assertThat(v.isValid(null, null)).isTrue();
        assertThat(v.isValid("", null)).isTrue();
        assertThat(v.isValid("12.345.678-5", null)).isTrue();
        assertThat(v.isValid("12.345.678-9", null)).isFalse();
    }
}
