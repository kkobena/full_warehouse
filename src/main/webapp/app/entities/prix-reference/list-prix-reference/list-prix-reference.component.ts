import { Component, inject, OnInit, viewChild } from '@angular/core';
import { IProduit } from '../../../shared/model/produit.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PrixReferenceService } from '../prix-reference.service';
import { PrixReference } from '../model/prix-reference.model';
import { CommonModule } from '@angular/common';
import { AddPrixFormComponent } from '../add-prix-form/add-prix-form.component';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { Card } from 'primeng/card';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { ConfirmationService } from 'primeng/api';

@Component({
  selector: 'jhi-list-prix-reference',
  imports: [CommonModule, Button, Tooltip, ConfirmDialogComponent, Card],
  providers: [ConfirmationService],
  templateUrl: './list-prix-reference.component.html',
  styleUrls: ['../../common-modal.component.scss', './list-prix-reference.component.scss'],
})
export class ListPrixReferenceComponent implements OnInit {
  produit: IProduit | null = null;
  tiersPayant: ITiersPayant | null = null;
  isFromProduit = true;
  protected prixReferences: PrixReference[] = [];
  private readonly activeModal = inject(NgbActiveModal);
  private readonly entityService = inject(PrixReferenceService);
  private readonly modalService = inject(NgbModal);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  constructor() {
    this.load();
  }

  get header(): string {
    if (this.isFromProduit && this.produit) {
      return `Liste des prix de référence pour le produit [ ${this.produit.libelle} ]`;
    }
    return `Liste des prix de référence pour le tiers payant [ ${this.tiersPayant?.fullName} ]`;
  }

  ngOnInit(): void {
    this.load();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  protected load(): void {
    if (this.isFromProduit && this.produit) {
      this.entityService.query(this.produit.id).subscribe(res => {
        this.prixReferences = res.body || [];
      });
    } else if (this.tiersPayant) {
      this.entityService.queryByTiersPayantId(this.tiersPayant.id).subscribe(res => {
        this.prixReferences = res.body || [];
      });
    }
  }

  protected onConfirmDelete(prixReference: PrixReference): void {
    this.confimDialog().onConfirm(() => this.onDelete(prixReference), 'Suppression', 'Voulez-vous vraiment supprimer cette ligne ?');
  }

  protected onCancel(prixReference: PrixReference): void {
    const message = prixReference.enabled ? 'Voulez-vous vraiment désactver cette ligne ?' : 'Voulez-vous vraiment activer cette ligne ?';
    this.confimDialog().onConfirm(
      () => {
        prixReference.enabled = !prixReference.enabled;
        this.entityService.update(prixReference).subscribe(() => {
          this.load();
        });
      },
      'Activation/Désactivation',
      message,
    );
  }

  protected onAddNew(): void {
    this.showModal();
  }

  protected onEdit(prixReference: PrixReference): void {
    this.showModal(prixReference);
  }

  private showModal(entity?: PrixReference): void {
    showCommonModal(
      this.modalService,
      AddPrixFormComponent,
      {
        isFromProduit: this.isFromProduit,
        produit: this.produit,
        tiersPayant: this.tiersPayant,
        entity: entity ? entity : null,
      },
      () => {
        this.load();
      },
      'lg',
      null,
      () => {
        this.load();
      },
    );
  }

  private onDelete(prixReference: PrixReference): void {
    this.entityService.delete(prixReference.id).subscribe(() => {
      this.load();
    });
  }
}
