package cl.activofijo.comun;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Formato estándar de listados paginados: {@code { content, page, size, totalElements, totalPages }}. */
public record PaginaDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PaginaDto<T> de(Page<E> pagina, Function<E, T> mapeo) {
        return new PaginaDto<>(pagina.getContent().stream().map(mapeo).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
