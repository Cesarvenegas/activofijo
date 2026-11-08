import { Component, inject, signal } from '@angular/core';
import { Clipboard } from '@angular/cdk/clipboard';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PasswordTemporal } from './usuarios.service';

/** Muestra la clave temporal UNA sola vez (el sistema no la guarda en texto plano). */
@Component({
  selector: 'app-password-temporal-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Clave temporal</h2>
    <mat-dialog-content>
      <p>
        Entregue esta clave a <strong>{{ datos.usuario.nombre }}</strong> ({{ datos.usuario.rutFormateado }}).
        Deberá cambiarla al ingresar.
      </p>
      <div class="clave">
        <code data-testid="clave-temporal">{{ datos.passwordTemporal }}</code>
        <button mat-icon-button (click)="copiar()" aria-label="Copiar clave">
          <mat-icon>{{ copiada() ? 'check' : 'content_copy' }}</mat-icon>
        </button>
      </div>
      <p class="aviso">
        <mat-icon>warning</mat-icon>
        Esta clave no se volverá a mostrar. Si se pierde, deberá resetearla nuevamente.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-flat-button mat-dialog-close>Entendido</button>
    </mat-dialog-actions>
  `,
  styles: `
    .clave {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin: 16px 0;
      padding: 12px;
      border-radius: 8px;
      background: var(--mat-sys-surface-container);
    }
    code {
      font-size: 24px;
      letter-spacing: 2px;
      font-weight: 500;
    }
    .aviso {
      display: flex;
      gap: 8px;
      align-items: center;
      color: var(--mat-sys-on-surface-variant);
      font-size: 13px;
    }
  `,
})
export class PasswordTemporalDialog {
  protected readonly datos = inject<PasswordTemporal>(MAT_DIALOG_DATA);
  private readonly clipboard = inject(Clipboard);
  protected readonly copiada = signal(false);

  protected copiar(): void {
    this.copiada.set(this.clipboard.copy(this.datos.passwordTemporal));
  }
}
