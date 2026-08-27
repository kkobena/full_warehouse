import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs/operators';
import { ButtonComponent, CardComponent } from '../../../../shared/ui';
import { NotificationService } from '../../../../shared/services/notification.service';
import { IResponseDto } from '../../../../shared/util/response-dto';
import { DciApiService } from '../../data-access/services/dci-api.service';

/**
 * Import CSV des DCI, en modale.
 *
 * Format attendu : `code;libelle`, séparateur point-virgule, première ligne
 * d'en-tête ignorée.
 *
 * Le libellé est OBLIGATOIRE, le code FACULTATIF : laissé vide, il est dérivé du
 * libellé côté serveur puis rendu unique. Une ligne sans libellé, ou dont le
 * libellé existe déjà, est ignorée — pas rejetée en erreur : réimporter le même
 * fichier ne doit pas échouer.
 */
@Component({
  selector: 'app-dci-import',
  templateUrl: './dci-import.component.html',
  styleUrl: './dci-import.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, CardComponent],
})
export class DciImportComponent {
  protected readonly fichier = signal<File | null>(null);
  protected readonly enCours = signal(false);
  protected readonly resultat = signal<IResponseDto | null>(null);

  private readonly api = inject(DciApiService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notification = inject(NotificationService);

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fichier.set(input.files?.length ? input.files[0] : null);
    this.resultat.set(null);
  }

  protected importer(): void {
    const file = this.fichier();
    if (!file) {
      return;
    }
    const formData = new FormData();
    // Le nom de la part doit correspondre au @RequestPart du contrôleur.
    formData.append('importcsv', file, file.name);

    this.enCours.set(true);
    this.api
      .uploadFile(formData)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe({
        next: reponse => {
          const dto = reponse.body;
          this.resultat.set(dto);
          if (dto?.success) {
            this.notification.success(dto.message ?? 'Import terminé.');
          } else {
            this.notification.error(dto?.message ?? 'Import impossible.');
          }
        },
        error: () => this.notification.error('Import impossible : le fichier n’a pas pu être traité.'),
      });
  }

  /** Ferme en signalant si des lignes ont été créées, pour que l'appelant rafraîchisse. */
  protected fermer(): void {
    this.activeModal.close((this.resultat()?.size ?? 0) > 0);
  }

  protected annuler(): void {
    this.activeModal.dismiss();
  }
}
