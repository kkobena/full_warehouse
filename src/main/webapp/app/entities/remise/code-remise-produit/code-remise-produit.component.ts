import { Component, inject, OnInit } from '@angular/core';
import { RemiseService } from '../remise.service';
import { CodeRemise, IRemise } from '../../../shared/model/remise.model';
import { HttpResponse } from '@angular/common/http';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CodeRemiseProduitsModalComponent } from '../code-remise-produits-modal/code-remise-produits-modal.component';

@Component({
    selector: 'jhi-code-remise-produit',
    providers: [MessageService, ConfirmationService],
    imports: [FaIconComponent],
    templateUrl: './code-remise-produit.component.html'
})
export class CodeRemiseProduitComponent implements OnInit {
  entityService = inject(RemiseService);
  entites?: CodeRemise[];
  ngModalService = inject(NgbModal);
  messageService = inject(MessageService);
  modalService = inject(ConfirmationService);

  load(): void {
    this.entityService.queryFullCodes().subscribe({
      next: (res: HttpResponse<CodeRemise[]>) => {
        this.entites = res.body || [];
      },
    });
  }

  onOpenModal(codeRemise?: CodeRemise): void {
    const modalRef = this.ngModalService.open(CodeRemiseProduitsModalComponent, {
      backdrop: 'static',
      size: 'xl',
      centered: true,
      animation: true,
    });
    modalRef.componentInstance.codeRemise = codeRemise;

    modalRef.closed.subscribe(r => {});
  }

  ngOnInit(): void {
    this.load();
  }

  protected getVnoTaux(entity: IRemise): string {
    const taut = entity?.grilles.filter(grille => grille.grilleType === 'VNO')[0]?.remiseValue;
    if (taut) {
      return taut + ' %';
    }
    return '';
  }

  protected getVoTaux(entity: IRemise): string {
    const taut = entity?.grilles.filter(grille => grille.grilleType === 'VO')[0]?.remiseValue;
    if (taut) {
      return taut + ' %';
    }
    return '';
  }
}
