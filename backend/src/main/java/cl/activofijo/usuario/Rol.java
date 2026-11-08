package cl.activofijo.usuario;

/** Roles del sistema. Spring Security los usa como autoridad {@code ROLE_<nombre>}. */
public enum Rol {
    /** Todo, incluidos usuarios y tablas maestras. */
    ADMIN,
    /** Registrar y editar bienes, ingresos, altas, movimientos e inventario. */
    OPERADOR,
    /** Solo ver, buscar e imprimir/exportar. */
    CONSULTA
}
