import { Component, ElementRef, inject, OnInit, Renderer2, signal } from "@angular/core";
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { IDelivery } from 'app/shared/model/delevery.model';
import { IMotifRetourProduit } from 'app/shared/model/motif-retour-produit.model';
import { ModifRetourProduitService } from 'app/entities/motif-retour-produit/motif-retour-produit.service';

@Component({
  selector: 'app-retour-complet-modal',
  imports: [CommonModule, FormsModule, ButtonModule, SelectModule, InputTextModule],
  templateUrl: './retour-complet-modal.component.html',
  styleUrl: './retour-complet-modal.component.scss',
})
export class RetourCompletModalComponent implements OnInit {
  delivery: IDelivery | null = null;

  protected motifs = signal<IMotifRetourProduit[]>([]);
  protected selectedMotifId: number | null = null;
  protected commentaire = '';
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly motifService = inject(ModifRetourProduitService);

  ngOnInit(): void {
    this.motifService.query({ page: 0, size: 999 }).subscribe({
      next: res => this.motifs.set(res.body ?? []),
    });
  }

  protected confirm(): void {
    if (!this.selectedMotifId) return;
    this.activeModal.close({ motifRetourId: this.selectedMotifId, commentaire: this.commentaire });
  }

  protected dismiss(): void {
    this.activeModal.dismiss();
  }
  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.addClass(modalBody, 'overflow-visible');
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.removeClass(modalBody, 'overflow-visible');
    }
  }

}
