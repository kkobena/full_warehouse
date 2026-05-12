import { Component, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IProduit } from 'app/shared/model/produit.model';
import { IFournisseurProduit } from 'app/shared/model/fournisseur-produit.model';
import { ProduitService } from 'app/entities/produit/produit.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { FormProduitFournisseurComponent } from 'app/entities/produit/form-produit-fournisseur/form-produit-fournisseur.component';

@Component({
  selector: 'app-produit-fournisseurs-tab',
  templateUrl: './produit-fournisseurs-tab.component.html',
  styleUrls: ['./produit-fournisseurs-tab.component.scss'],
  imports: [CommonModule, FormsModule, TableModule, TagModule, TooltipModule, ButtonModule, ToggleSwitchModule],
})
export class ProduitFournisseursTabComponent {
  readonly produit = input.required<IProduit>();
  readonly refreshRequested = output<void>();

  private readonly modalService = inject(NgbModal);
  private readonly produitService = inject(ProduitService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  protected onAddFournisseur(): void {
    const modalRef = this.modalService.open(FormProduitFournisseurComponent, { centered:true, size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.header = 'Ajouter un fournisseur';
    modalRef.componentInstance.produit = this.produit();
    modalRef.result.then(
      () => this.refreshRequested.emit(),
      () => {},
    );
  }

  protected onEditFournisseur(fp: IFournisseurProduit): void {
    const modalRef = this.modalService.open(FormProduitFournisseurComponent, {centered:true, size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.header = 'Modifier le fournisseur';
    modalRef.componentInstance.produit = this.produit();
    modalRef.componentInstance.entity = fp;
    modalRef.result.then(
      () => this.refreshRequested.emit(),
      () => {},
    );
  }

  protected onDeleteFournisseur(fp: IFournisseurProduit): void {
    this.confirmDialog.onConfirm(
      () => this.execDelete(fp),
      'Supprimer le fournisseur',
      `Retirer "${fp.fournisseurLibelle}" de ce produit ?`,
    );
  }

  protected isPrincipal(fp: IFournisseurProduit): boolean {
    return this.produit().fournisseurProduit?.id === fp.id;
  }

  protected onTogglePrincipal(fp: IFournisseurProduit, checked: boolean): void {
    if (!checked || this.isPrincipal(fp)) return;
    this.produitService.updateDefaultFournisseur(fp.id!, this.produit().id!, true).subscribe({
      next: () => this.refreshRequested.emit(),
    });
  }

  protected formatPrix(montant?: number | null): string {
    if (montant == null) return '—';
    return montant.toLocaleString('fr-FR', { minimumFractionDigits: 0 }) + ' FCFA';
  }

  private execDelete(fp: IFournisseurProduit): void {
    this.produitService.deleteFournisseur(fp.id!).subscribe({
      next: () => this.refreshRequested.emit(),
    });
  }
}
