/** Ítems del menú lateral. Los módulos se habilitan a medida que se implementa cada etapa. */
export interface ItemMenu {
  etiqueta: string;
  icono: string;
  ruta: string;
  habilitado: boolean;
}

export interface GrupoMenu {
  titulo?: string;
  items: ItemMenu[];
}

export const MENU: GrupoMenu[] = [
  {
    items: [{ etiqueta: 'Inicio', icono: 'home', ruta: '/inicio', habilitado: true }],
  },
  {
    titulo: 'Gestión de bienes',
    items: [
      { etiqueta: 'Ingreso', icono: 'inventory_2', ruta: '/ingresos', habilitado: false },
      { etiqueta: 'Alta', icono: 'add_location_alt', ruta: '/altas', habilitado: false },
      { etiqueta: 'Traspaso', icono: 'swap_horiz', ruta: '/traspasos', habilitado: false },
      { etiqueta: 'Baja', icono: 'remove_circle_outline', ruta: '/bajas', habilitado: false },
      { etiqueta: 'Mantención', icono: 'build', ruta: '/mantenciones', habilitado: false },
    ],
  },
  {
    titulo: 'Información',
    items: [
      { etiqueta: 'Consultas', icono: 'search', ruta: '/consultas', habilitado: false },
      { etiqueta: 'Informes', icono: 'description', ruta: '/informes', habilitado: false },
      { etiqueta: 'Depreciación', icono: 'trending_down', ruta: '/depreciacion', habilitado: false },
    ],
  },
  {
    titulo: 'Administración',
    items: [
      { etiqueta: 'Tablas maestras', icono: 'table_chart', ruta: '/tablas-maestras', habilitado: false },
      { etiqueta: 'Usuarios', icono: 'group', ruta: '/usuarios', habilitado: false },
    ],
  },
];
