import { Component, inject, input, output, ChangeDetectionStrategy } from '@angular/core';
import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../../../../shared/ui';
import { IProduit } from '../../../../shared/model/produit.model';
import { IRayonProduit } from '../../../../shared/model/rayon-produit.model';
import { RayonProduitApiService } from '../../../rayon/data-access/services/rayon-produit-api.service';
import { RayonAssignFormComponent, RayonAssignResult } from '../../../rayon/ui/rayon-assign-form/rayon-assign-form.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';

@Component({
  selector: 'app-produit-rayons-tab',
  templateUrl: './produit-rayons-tab.component.html',
  styleUrl: './produit-rayons-tab.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [ButtonComponent, NgbTooltip],
})
export class ProduitRayonsTabComponent {
  readonly produit = input.required<IProduit>();
  readonly refreshRequested = output<void>();

  private readonly api = inject(RayonProduitApiService);
  private readonly modalService = inject(NgbModal);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  protected get assignments(): IRayonProduit[] {
    return this.produit().rayonProduits ?? [];
  }

  protected onDeplacer(assignment: IRayonProduit): void {
    const ref = this.modalService.open(RayonAssignFormComponent, { size: 'md', centered: true, backdrop: 'static' });
    const inst = ref.componentInstance as RayonAssignFormComponent;
    inst.produit = this.produit();
    inst.mode = 'move';
    inst.title = assignment.codeRayon === 'SANS'
      ? `Assigner un emplacement dans "${assignment.libelleStorage}"`
      : `Déplacer "${this.produit().libelle}" dans "${assignment.libelleStorage}"`;
    inst.currentStorageId = assignment.storageId;
    inst.currentRayonId = assignment.rayonId;
    inst.currentRayonIsSans = assignment.codeRayon === 'SANS';

    ref.closed.subscribe((result: RayonAssignResult) => {
      this.api.move({ produitId: result.produitId, rayonId: result.rayonId }).subscribe({
        next: () => {
          this.notificationService.success('Emplacement mis à jour');
          this.refreshRequested.emit();
        },
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
      });
    });
  }

  protected onAjouterStockage(): void {
    // Exclut tous les stockages déjà assignés (SANS inclus) — "assigner un emplacement"
    // dans un stockage existant se fait via le bouton dédié sur la ligne.
    const occupiedStorageIds = (this.produit().rayonProduits ?? [])
      .map(rp => rp.storageId)
      .filter((id): id is number => id != null);

    const ref = this.modalService.open(RayonAssignFormComponent, { size: 'md', centered: true, backdrop: 'static' });
    const inst = ref.componentInstance as RayonAssignFormComponent;
    inst.produit = this.produit();
    inst.mode = 'add-storage';
    inst.title = `Affecter à un autre stockage`;
    inst.occupiedRealStorageIds = occupiedStorageIds;

    ref.closed.subscribe((result: RayonAssignResult) => {
      this.api.assign({ produitId: result.produitId, rayonId: result.rayonId }).subscribe({
        next: () => {
          this.notificationService.success('Affecté au nouveau stockage');
          this.refreshRequested.emit();
        },
        error: err => {
          this.notificationService.error(this.errorService.getErrorMessage(err));
        },
      });
    });
  }
}
