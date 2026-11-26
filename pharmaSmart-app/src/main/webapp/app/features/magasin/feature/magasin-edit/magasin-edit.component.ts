import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { IMagasin, TypeMagasin } from 'app/shared/model/magasin.model';
import { ButtonComponent, ToolbarComponent } from 'app/shared/ui';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { MagasinService } from '../../data-access/services/magasin.service';
import { MagasinFormComponent } from '../../ui/magasin-form/magasin-form.component';

/**
 * Création d'une unité de gestion, ou modification d'un magasin.
 *
 * Le magasin arrive par le resolver de la route (`withComponentInputBinding` le lie à l'entrée
 * `magasin`) ; la page enregistre, puis revient à la fiche.
 */
@Component({
  selector: 'app-magasin-edit',
  templateUrl: './magasin-edit.component.html',
  styleUrl: './magasin-edit.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, ToolbarComponent, MagasinFormComponent],
})
export class MagasinEditComponent {
  /** Fourni par le resolver. */
  readonly magasin = input.required<IMagasin>();

  protected readonly isSaving = signal(false);
  protected readonly titre = computed(() => {
    const m = this.magasin();
    if (m.id) {
      return m.typeMagasin === TypeMagasin.UNITE_GESTION ? `Éditer l'unité de gestion ${m.name ?? ''}`.trim() : 'Éditer les informations du magasin';
    }
    return 'Nouvelle unité de gestion';
  });

  private readonly magasinService = inject(MagasinService);
  private readonly router = inject(Router);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  protected enregistrer(magasin: IMagasin): void {
    this.isSaving.set(true);
    const requete = magasin.id ? this.magasinService.update(magasin) : this.magasinService.create(magasin);
    requete.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.notification.success(magasin.id ? 'Magasin enregistré.' : 'Unité de gestion créée.');
        this.retourALaFiche();
      },
      error: (err: HttpErrorResponse) => {
        this.isSaving.set(false);
        this.notification.error(this.errorService.getErrorMessage(err));
      },
    });
  }

  protected retourALaFiche(): void {
    void this.router.navigate(['/magasin']);
  }
}
