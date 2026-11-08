import { Component, computed, inject, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { debounceTime, filter, switchMap } from 'rxjs';
import { AuthService, NOMBRE_ROL, Rol } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../core/http/problema';
import { confirmar } from '../../shared/dialogos/confirmacion';
import { CeldaDirective, ColumnaTabla, TablaFiltrable } from '../../shared/tabla-filtrable/tabla-filtrable';
import { PasswordTemporal, Usuario, FiltroUsuarios, UsuariosService } from './usuarios.service';
import { ResultadoFormUsuario, UsuarioFormDialog } from './usuario-form-dialog';
import { PasswordTemporalDialog } from './password-temporal-dialog';

@Component({
  selector: 'app-usuarios',
  imports: [
    DatePipe, ReactiveFormsModule, MatButtonModule, MatIconModule, MatMenuModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatChipsModule, MatTooltipModule, TablaFiltrable, CeldaDirective,
  ],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
})
export class Usuarios {
  private readonly service = inject(UsuariosService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly yo = inject(AuthService).usuario;

  private readonly tabla = viewChild.required<TablaFiltrable<Usuario, FiltroUsuarios>>(TablaFiltrable);

  protected readonly nombreRol = NOMBRE_ROL;
  protected readonly roles = Object.entries(NOMBRE_ROL) as [Rol, string][];

  protected readonly filtroTexto = new FormControl('', { nonNullable: true });
  protected readonly filtroRol = new FormControl<Rol | ''>('', { nonNullable: true });
  protected readonly filtroActivo = new FormControl<boolean | ''>(true, { nonNullable: true });

  private readonly texto = toSignal(this.filtroTexto.valueChanges.pipe(debounceTime(300)), { initialValue: '' });
  private readonly rol = toSignal(this.filtroRol.valueChanges, { initialValue: '' as Rol | '' });
  private readonly activo = toSignal(this.filtroActivo.valueChanges, { initialValue: true as boolean | '' });
  protected readonly filtros = computed<FiltroUsuarios>(() => ({
    texto: this.texto(),
    rol: this.rol(),
    activo: this.activo(),
  }));

  protected readonly cargar = (c: Parameters<UsuariosService['listar']>[0]) => this.service.listar(c);

  protected readonly columnas: ColumnaTabla<Usuario>[] = [
    { id: 'rut', titulo: 'RUN', ordenable: true, valor: (u) => u.rutFormateado },
    { id: 'nombre', titulo: 'Nombre', ordenable: true },
    { id: 'cargo', titulo: 'Cargo', valor: (u) => u.cargo ?? '' },
    { id: 'rol', titulo: 'Rol', ordenable: true },
    { id: 'estado', titulo: 'Estado' },
    { id: 'ultimoAcceso', titulo: 'Último acceso', ordenable: true },
    { id: 'acciones', titulo: '', clase: 'acciones' },
  ];

  /** Las plantillas de celda reciben la fila sin tipo; esto la tipa para el template. */
  protected comoUsuario(fila: unknown): Usuario {
    return fila as Usuario;
  }

  protected limpiarFiltros(): void {
    this.filtroTexto.setValue('');
    this.filtroRol.setValue('');
    this.filtroActivo.setValue(true);
  }

  protected nuevo(): void {
    this.abrirFormulario(null);
  }

  protected editar(u: Usuario): void {
    this.abrirFormulario(u);
  }

  protected cambiarActivo(u: Usuario): void {
    const activar = !u.activo;
    confirmar(this.dialog, {
      titulo: activar ? 'Activar usuario' : 'Desactivar usuario',
      mensaje: activar
        ? `${u.nombre} podrá volver a ingresar al sistema.`
        : `${u.nombre} no podrá ingresar al sistema y se cerrarán sus sesiones abiertas.`,
      aceptar: activar ? 'Activar' : 'Desactivar',
      peligroso: !activar,
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.service.cambiarActivo(u.id, activar)),
      )
      .subscribe({
        next: () => {
          this.avisar(activar ? 'Usuario activado' : 'Usuario desactivado');
          this.tabla().recargar();
        },
        error: (e) => this.avisar(mensajeDeError(e)),
      });
  }

  protected resetearPassword(u: Usuario): void {
    confirmar(this.dialog, {
      titulo: 'Resetear contraseña',
      mensaje: `Se generará una clave temporal para ${u.nombre}. Su clave actual dejará de funcionar, `
        + 'se cerrarán sus sesiones y, si estaba bloqueado, se desbloqueará.',
      aceptar: 'Resetear',
      peligroso: true,
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.service.resetearPassword(u.id)),
      )
      .subscribe({
        next: (r) => {
          this.mostrarClave(r);
          this.tabla().recargar();
        },
        error: (e) => this.avisar(mensajeDeError(e)),
      });
  }

  private abrirFormulario(u: Usuario | null): void {
    this.dialog
      .open<UsuarioFormDialog, Usuario | null, ResultadoFormUsuario>(UsuarioFormDialog, {
        data: u,
        width: '640px',
        maxWidth: '95vw',
        disableClose: true,
      })
      .afterClosed()
      .pipe(filter((r): r is ResultadoFormUsuario => !!r))
      .subscribe((r) => {
        if (r.tipo === 'creado') {
          this.mostrarClave(r.resultado);
        } else {
          this.avisar('Usuario actualizado');
        }
        this.tabla().recargar();
      });
  }

  private mostrarClave(r: PasswordTemporal): void {
    this.dialog.open(PasswordTemporalDialog, { data: r, width: '460px', maxWidth: '95vw', disableClose: true });
  }

  private avisar(mensaje: string): void {
    this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
  }
}
