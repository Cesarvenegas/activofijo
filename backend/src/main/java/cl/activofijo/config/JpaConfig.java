package cl.activofijo.config;

import cl.activofijo.auth.UsuarioActual;
import cl.activofijo.auth.UsuarioSesion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/** Auditoría JPA (creado/modificado por y en) y reloj del sistema (reemplazable en tests). */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditor", dateTimeProviderRef = "proveedorFecha")
public class JpaConfig {

    /** Usuario que se registra en creado_por/modificado_por; "sistema" si no hay sesión. */
    @Bean
    AuditorAware<String> auditor() {
        return () -> Optional.of(UsuarioActual.obtener().map(UsuarioSesion::rut).orElse("sistema"));
    }

    @Bean
    DateTimeProvider proveedorFecha(Clock clock) {
        return () -> Optional.of(Instant.now(clock));
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
