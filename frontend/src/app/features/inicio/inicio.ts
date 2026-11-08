import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EstadoSistema, SaludService } from '../../core/salud/salud.service';

/** Página de inicio. En la Etapa 0 solo muestra el estado del sistema. */
@Component({
  selector: 'app-inicio',
  imports: [DatePipe, MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  private readonly saludService = inject(SaludService);

  protected readonly cargando = signal(true);
  protected readonly estado = signal<EstadoSistema | null>(null);

  constructor() {
    this.consultar();
  }

  protected consultar(): void {
    this.cargando.set(true);
    this.saludService.consultar().subscribe((e) => {
      this.estado.set(e);
      this.cargando.set(false);
    });
  }
}
