package cl.activofijo.usuario;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByRut(String rut);

    boolean existsByRut(String rut);

    /** Filtros del listado de usuarios; cada criterio nulo se ignora. */
    static Specification<Usuario> filtro(String texto, Rol rol, Boolean activo) {
        return (root, query, cb) -> {
            var condiciones = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + texto.trim().toLowerCase() + "%";
                // El RUN se busca sin puntos para que "12.345" encuentre "12345678-5"
                String patronRut = "%" + texto.replace(".", "").trim().toUpperCase() + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(root.get("rut"), patronRut),
                        cb.like(cb.lower(root.get("email")), patron),
                        cb.like(cb.lower(root.get("cargo")), patron)));
            }
            if (rol != null) {
                condiciones.add(cb.equal(root.get("rol"), rol));
            }
            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(condiciones.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
