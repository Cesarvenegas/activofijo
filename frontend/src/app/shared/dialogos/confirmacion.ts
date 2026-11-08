import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Observable, map } from 'rxjs';

export interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  aceptar?: string;
  peligroso?: boolean;
}

/** Diálogo de confirmación genérico. Usar con {@link confirmar}. */
@Component({
  selector: 'app-confirmacion',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>{{ datos.mensaje }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button [mat-dialog-close]="true" [class.peligroso]="datos.peligroso" cdkFocusInitial>
        {{ datos.aceptar ?? 'Aceptar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .peligroso {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class Confirmacion {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
}

/** Abre el diálogo y emite true si el usuario acepta. */
export function confirmar(dialog: MatDialog, datos: DatosConfirmacion): Observable<boolean> {
  return dialog
    .open(Confirmacion, { data: datos, width: '420px', maxWidth: '95vw' })
    .afterClosed()
    .pipe(map((r) => r === true));
}
