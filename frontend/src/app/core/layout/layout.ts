import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { map } from 'rxjs';
import { AuthService, NOMBRE_ROL } from '../auth/auth.service';
import { menuPara } from './menu';

/** Estructura base: barra superior con la marca y el usuario + menú lateral + contenido. */
@Component({
  selector: 'app-layout',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule,
  ],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly usuario = this.auth.usuario;
  protected readonly nombreRol = NOMBRE_ROL;
  protected readonly menu = computed(() => menuPara(this.usuario()?.rol));

  protected readonly esMovil = toSignal(
    inject(BreakpointObserver).observe('(max-width: 959px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );
  protected readonly modoSidenav = computed(() => (this.esMovil() ? 'over' : 'side'));
  protected readonly sidenavAbierto = computed(() => !this.esMovil());

  protected cerrarSesion(): void {
    this.auth.logout().subscribe(() => this.router.navigateByUrl('/login'));
  }
}
