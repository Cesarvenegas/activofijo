import {
  Component, Directive, TemplateRef, computed, contentChildren, inject, input, linkedSignal, output, signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { Observable, catchError, of, switchMap, tap } from 'rxjs';
import { mensajeDeError } from '../../core/http/problema';

/** Respuesta paginada estándar del backend. */
export interface Pagina<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Parámetros de paginación/orden que la tabla agrega a los filtros al consultar. */
export interface ConsultaTabla {
  page: number;
  size: number;
  /** Formato Spring: "campo,asc" | "campo,desc" */
  sort?: string;
}

export interface ColumnaTabla<T> {
  id: string;
  titulo: string;
  ordenable?: boolean;
  /** Valor a mostrar; si no se indica se usa fila[id]. Para celdas complejas usar <ng-template appCelda="id">. */
  valor?: (fila: T) => unknown;
  /** Clase CSS opcional para la columna (ej. 'acciones', 'numero'). */
  clase?: string;
}

/** Plantilla personalizada para una celda: <ng-template appCelda="estado" let-fila>…</ng-template> */
@Directive({ selector: 'ng-template[appCelda]' })
export class CeldaDirective {
  readonly appCelda = input.required<string>();
  readonly template = inject<TemplateRef<{ $implicit: unknown }>>(TemplateRef);
}

/**
 * Listado reutilizable: mat-table con paginación y orden en el servidor, panel de filtros
 * proyectado (atributo `filtros`) y barra de acciones (atributo `acciones`).
 * Al cambiar los filtros vuelve a la primera página.
 */
@Component({
  selector: 'app-tabla-filtrable',
  imports: [NgTemplateOutlet, MatTableModule, MatSortModule, MatPaginatorModule, MatProgressBarModule, MatIconModule],
  templateUrl: './tabla-filtrable.html',
  styleUrl: './tabla-filtrable.scss',
})
export class TablaFiltrable<T, F extends object = Record<string, unknown>> {
  readonly columnas = input.required<ColumnaTabla<T>[]>();
  readonly cargar = input.required<(consulta: F & ConsultaTabla) => Observable<Pagina<T>>>();
  readonly filtros = input<F>({} as F);
  readonly ordenInicial = input<Sort>({ active: '', direction: '' });
  readonly tamanoInicial = input(50);
  readonly filaClick = output<T>();

  protected readonly celdas = contentChildren(CeldaDirective);
  protected readonly idsColumnas = computed(() => this.columnas().map((c) => c.id));

  protected readonly pagina = linkedSignal({ source: this.filtros, computation: () => 0 });
  protected readonly tamano = linkedSignal(() => this.tamanoInicial());
  protected readonly orden = linkedSignal(() => this.ordenInicial());
  private readonly recargas = signal(0);

  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly datos = signal<Pagina<T> | null>(null);
  protected readonly filas = computed(() => this.datos()?.content ?? []);
  protected readonly total = computed(() => this.datos()?.totalElements ?? 0);

  constructor() {
    const consulta = computed(() => {
      const o = this.orden();
      this.recargas();
      return {
        ...this.filtros(),
        page: this.pagina(),
        size: this.tamano(),
        ...(o.active && o.direction ? { sort: `${o.active},${o.direction}` } : {}),
      } as F & ConsultaTabla;
    });

    toObservable(consulta)
      .pipe(
        tap(() => {
          this.cargando.set(true);
          this.error.set(null);
        }),
        switchMap((c) =>
          this.cargar()(c).pipe(
            catchError((e) => {
              this.error.set(mensajeDeError(e, 'No se pudo cargar el listado.'));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((p) => {
        if (p) this.datos.set(p);
        this.cargando.set(false);
      });
  }

  /** Vuelve a consultar con los mismos filtros (ej. después de crear o editar). */
  recargar(): void {
    this.recargas.update((n) => n + 1);
  }

  protected cambiarOrden(s: Sort): void {
    this.orden.set(s);
    this.pagina.set(0);
  }

  protected cambiarPagina(e: PageEvent): void {
    this.tamano.set(e.pageSize);
    this.pagina.set(e.pageIndex);
  }

  protected plantilla(id: string): TemplateRef<{ $implicit: unknown }> | undefined {
    return this.celdas().find((c) => c.appCelda() === id)?.template;
  }

  protected valor(fila: T, col: ColumnaTabla<T>): unknown {
    return col.valor ? col.valor(fila) : (fila as Record<string, unknown>)[col.id];
  }
}
