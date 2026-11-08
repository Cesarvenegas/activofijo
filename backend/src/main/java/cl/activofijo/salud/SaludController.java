package cl.activofijo.salud;

import cl.activofijo.salud.SaludDto.BaseDatos;
import cl.activofijo.salud.SaludDto.Estado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Estado del sistema para la UI y el monitoreo. Responde 200 si todo está bien y 503 si la base
 * de datos no responde (el cuerpo indica el motivo en ambos casos).
 */
@RestController
@RequestMapping("/api/v1/salud")
public class SaludController {

    private static final Logger log = LoggerFactory.getLogger(SaludController.class);
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final JdbcTemplate jdbc;
    private final String aplicacion;
    private final String version;

    public SaludController(JdbcTemplate jdbc,
                           ObjectProvider<BuildProperties> buildProperties,
                           @Value("${spring.application.name}") String aplicacion) {
        this.jdbc = jdbc;
        this.aplicacion = aplicacion;
        BuildProperties build = buildProperties.getIfAvailable();
        this.version = build != null ? build.getVersion() : "desarrollo";
    }

    @GetMapping
    public ResponseEntity<SaludDto> salud() {
        BaseDatos bd = consultarBaseDatos();
        SaludDto dto = new SaludDto(aplicacion, version, bd.estado(), bd, OffsetDateTime.now(ZONA));
        HttpStatus status = bd.estado() == Estado.OK ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(dto);
    }

    private BaseDatos consultarBaseDatos() {
        try {
            String v = jdbc.queryForObject("SHOW server_version", String.class);
            return new BaseDatos(Estado.OK, "PostgreSQL " + v, null);
        } catch (RuntimeException ex) {
            log.warn("La base de datos no responde: {}", ex.getMessage());
            return new BaseDatos(Estado.ERROR, null, "No se pudo conectar con la base de datos");
        }
    }
}
