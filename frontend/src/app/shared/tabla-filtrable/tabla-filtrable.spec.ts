import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { ColumnaTabla, ConsultaTabla, Pagina, TablaFiltrable } from './tabla-filtrable';

interface Fila {
  id: number;
  nombre: string;
}

@Component({
  imports: [TablaFiltrable],
  template: `<app-tabla-filtrable [columnas]="columnas" [cargar]="cargar" [filtros]="filtros()"
                                  [ordenInicial]="{ active: 'nombre', direction: 'asc' }" />`,
})
class Anfitrion {
  readonly columnas: ColumnaTabla<Fila>[] = [{ id: 'nombre', titulo: 'Nombre', ordenable: true }];
  readonly filtros = signal<{ texto: string }>({ texto: '' });
  readonly consultas: ({ texto: string } & ConsultaTabla)[] = [];
  readonly cargar = (c: { texto: string } & ConsultaTabla): Observable<Pagina<Fila>> => {
    this.consultas.push(c);
    return of({ content: [{ id: 1, nombre: 'Ana' }, { id: 2, nombre: 'Luis' }], page: c.page, size: c.size,
      totalElements: 2, totalPages: 1 });
  };
}

describe('TablaFiltrable', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Anfitrion] });
  });

  it('consulta con página, tamaño y orden, y muestra filas y total', async () => {
    const fixture = TestBed.createComponent(Anfitrion);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.consultas[0]).toEqual({ texto: '', page: 0, size: 50, sort: 'nombre,asc' });
    expect(el.querySelectorAll('tr.mat-mdc-row').length).toBe(2);
    expect(el.textContent).toContain('Luis');
    expect(el.textContent).toContain('2 registros');
  });

  it('al cambiar los filtros vuelve a consultar', async () => {
    const fixture = TestBed.createComponent(Anfitrion);
    await fixture.whenStable();

    fixture.componentInstance.filtros.set({ texto: 'ana' });
    await fixture.whenStable();

    const ultima = fixture.componentInstance.consultas.at(-1);
    expect(ultima).toEqual({ texto: 'ana', page: 0, size: 50, sort: 'nombre,asc' });
  });
});
