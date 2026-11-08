package cl.activofijo.auth;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escribe un ProblemDetail JSON desde los filtros de seguridad (fuera de Spring MVC, donde no
 * actúa el ManejadorErrores).
 */
final class RespuestaProblema {

    private RespuestaProblema() {
    }

    static void escribir(HttpServletResponse response, int status, String titulo, String detalle, String codigo,
                         String instancia) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = "{\"type\":\"about:blank\",\"title\":" + q(titulo) + ",\"status\":" + status
                + ",\"detail\":" + q(detalle) + ",\"instance\":" + q(instancia)
                + (codigo != null ? ",\"codigo\":" + q(codigo) : "") + "}";
        response.getWriter().write(json);
    }

    private static String q(String texto) {
        if (texto == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
