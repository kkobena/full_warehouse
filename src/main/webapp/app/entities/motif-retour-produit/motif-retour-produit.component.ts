import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TableModule, Table } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { InputText } from 'primeng/inputtext';
import { ConfirmationService, MessageService } from 'primeng/api';
import { IMotifRetourProduit } from 'app/shared/model/motif-retour-produit.model';
import { ModifRetourProduitService } from './motif-retour-produit.service';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { Tooltip } from 'primeng/tooltip';
import { MotifRetourProduitFormModalComponent } from './motif-retour-produit-form-modal.component';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';

@Component({
  selector: 'jhi-motif-retour-produit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    InputTextModule,
    ConfirmDialogModule,
    ToastModule,
    Tooltip,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './motif-retour-produit.component.html',
  styleUrl: './motif-retour-produit.component.scss',
})
export class MotifRetourProduitComponent implements OnInit {
  private readonly motifRetourService = inject(ModifRetourProduitService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly modalService = inject(NgbModal);

  protected motifRetours = signal<IMotifRetourProduit[]>([]);
  protected totalRecords = signal<number>(0);
  protected loading = signal<boolean>(false);
  protected itemsPerPage = ITEMS_PER_PAGE;

  ngOnInit(): void {
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    this.motifRetourService
      .query()
      .subscribe({
        next: (res: HttpResponse<IMotifRetourProduit[]>) => {
          this.motifRetours.set(res.body || []);
          this.totalRecords.set(res.body?.length || 0);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.showError('Erreur lors du chargement des motifs de retour');
        },
      });
  }


  protected openNew(): void {
    const modalRef = this.modalService.open(MotifRetourProduitFormModalComponent, {
      size: 'md',
      backdrop: 'static',
      centered: true,
      keyboard: false,
    });

    modalRef.result.then(
      () => {
          this.loadAll();

      },
      () => {
        // Modal dismissed
      }
    );
  }

  protected edit(motif: IMotifRetourProduit): void {
    const modalRef = this.modalService.open(MotifRetourProduitFormModalComponent, {
      size: 'md',
      backdrop: 'static',
      keyboard: false,
    });

    modalRef.componentInstance.motifToEdit = motif;

    modalRef.result.then(
      () => {
          this.loadAll();
      },
      () => {
        // Modal dismissed
      }
    );
  }

  protected confirmDelete(motif: IMotifRetourProduit): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le motif "${motif.libelle}" ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.delete(motif.id!);
      },
    });
  }

  protected delete(id: number): void {
    this.motifRetourService.delete(id).subscribe({
      next: () => {
        this.showSuccess('Motif de retour supprimé avec succès');
        this.loadAll();
      },
      error: () => {
        this.showError('Erreur lors de la suppression du motif de retour');
      },
    });
  }

  private showSuccess(message: string): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Succès',
      detail: message,
      life: 3000,
    });
  }

  private showError(message: string): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: message,
      life: 3000,
    });
  }
}
