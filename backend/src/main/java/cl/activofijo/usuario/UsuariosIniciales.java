package cl.activofijo.usuario;

import cl.activofijo.comun.rut.Rut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Usuarios iniciales.
 * <ul>
 *   <li><b>dev</b>: admin, operador y consulta de prueba (clave {@value #CLAVE_DEMO}).</li>
 *   <li><b>cualquier perfil</b>: si la tabla está vacía y se definen {@code APP_ADMIN_RUT} y
 *       {@code APP_ADMIN_PASSWORD}, crea el primer ADMIN con cambio de clave obligatorio.</li>
 * </ul>
 */
@Configuration
public class UsuariosIniciales {

    static final String CLAVE_DEMO = "Kromas2026";
    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciales.class);

    @Bean
    ApplicationRunner primerAdministrador(UsuarioRepository repo, PasswordEncoder encoder, TransactionTemplate tx,
                                          @Value("${app.admin-inicial.rut:}") String rut,
                                          @Value("${app.admin-inicial.password:}") String password) {
        return args -> tx.executeWithoutResult(s -> {
            if (rut.isBlank() || password.isBlank() || repo.count() > 0) {
                return;
            }
            String normalizado = Rut.normalizar(rut)
                    .orElseThrow(() -> new IllegalStateException("APP_ADMIN_RUT no es un RUN válido: " + rut));
            PoliticaPassword.validar(password);
            repo.save(new Usuario(normalizado, "Administrador", Rol.ADMIN, encoder.encode(password)));
            log.info("Creado el primer administrador con RUN {}", Rut.formatear(normalizado));
        });
    }

    @Bean
    @Profile("dev")
    ApplicationRunner usuariosDemo(UsuarioRepository repo, PasswordEncoder encoder, TransactionTemplate tx) {
        return (ApplicationArguments args) -> tx.executeWithoutResult(s -> {
            crearDemo(repo, encoder, "11111111-1", "Administrador Demo", Rol.ADMIN);
            crearDemo(repo, encoder, "22222222-2", "Operador Demo", Rol.OPERADOR);
            crearDemo(repo, encoder, "33333333-3", "Consulta Demo", Rol.CONSULTA);
        });
    }

    private static void crearDemo(UsuarioRepository repo, PasswordEncoder encoder, String rut, String nombre, Rol rol) {
        if (repo.existsByRut(rut)) {
            return;
        }
        Usuario u = new Usuario(rut, nombre, rol, encoder.encode(CLAVE_DEMO));
        u.cambiarPassword(u.getPasswordHash(), false);
        repo.save(u);
        log.info("Usuario demo {} ({}) creado con clave {}", Rut.formatear(rut), rol, CLAVE_DEMO);
    }
}
