import { Component, inject, OnInit } from '@angular/core';
import { IProduit } from '../../../shared/model/produit.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PrixReferenceService } from '../prix-reference.service';
import { PrixReference } from '../model/prix-reference.model';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { ConfirmationService } from 'primeng/api';
import { CommonModule } from '@angular/common';
import { AddPrixFormComponent } from '../add-prix-form/add-prix-form.component';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmDialog } from 'primeng/confirmdialog';

@Component({
  selector: 'jhi-list-prix-reference',
  imports: [CommonModule, Button, Tooltip, ConfirmDialog],
  templateUrl: './list-prix-reference.component.html',
  providers: [ConfirmationService],
})
export class ListPrixReferenceComponent implements OnInit {
  produit: IProduit | null = null;
  tiersPayant: ITiersPayant | null = null;
  isFromProduit = true;

  protected prixReferences: PrixReference[] = [];
  private readonly activeModal = inject(NgbActiveModal);
  private readonly entityService = inject(PrixReferenceService);
  private readonly modalService = inject(NgbModal);
  private confirmationService = inject(ConfirmationService);

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
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer cette ligne ?',
      header: 'SUPPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.onDelete(prixReference),
    });
  }

  protected onCancel(prixReference: PrixReference): void {
    const message = prixReference.enabled ? 'Voulez-vous vraiment désactver cette ligne ?' : 'Voulez-vous vraiment activer cette ligne ?';
    this.confirmationService.confirm({
      message: message,
      header: 'ACTIVATION/DESACTIVATION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        prixReference.enabled = !prixReference.enabled;
        this.entityService.update(prixReference).subscribe(() => {
          this.load();
        });
      },
    });
  }

  protected onAddNew(): void {
    const modalRef = this.modalService.open(AddPrixFormComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.isFromProduit = this.isFromProduit;
    modalRef.componentInstance.produit = this.produit;
    modalRef.componentInstance.tiersPayant = this.tiersPayant;
    modalRef.result.then(
      () => {
        this.load();
      },
      () => {
        this.load();
      },
    );
  }

  protected onEdit(prixReference: PrixReference): void {
    const modalRef = this.modalService.open(AddPrixFormComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.isFromProduit = this.isFromProduit;
    modalRef.componentInstance.produit = this.produit;
    modalRef.componentInstance.tiersPayant = this.tiersPayant;
    modalRef.componentInstance.entity = prixReference;
    modalRef.result.then(
      () => {
        this.load();
      },
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
