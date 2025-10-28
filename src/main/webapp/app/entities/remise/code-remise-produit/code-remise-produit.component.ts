import { Component, inject, OnInit } from '@angular/core';
import { RemiseService } from '../remise.service';
import { CodeRemise, IRemise } from '../../../shared/model/remise.model';
import { HttpResponse } from '@angular/common/http';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { CodeRemiseProduitsModalComponent } from '../code-remise-produits-modal/code-remise-produits-modal.component';
import { Toolbar } from 'primeng/toolbar';

@Component({
  selector: 'jhi-code-remise-produit',
  imports: [FaIconComponent, Toolbar],
  templateUrl: './code-remise-produit.component.html',
  styleUrls: ['./code-remise-produit.component.scss']
})
export class CodeRemiseProduitComponent implements OnInit {
  entites?: CodeRemise[];
  private readonly entityService = inject(RemiseService);
  private readonly ngModalService = inject(NgbModal);

  load(): void {
    this.entityService.queryFullCodes().subscribe({
      next: (res: HttpResponse<CodeRemise[]>) => {
        this.entites = res.body || [];
      }
    });
  }

  onOpenModal(codeRemise?: CodeRemise): void {
    const modalRef = this.ngModalService.open(CodeRemiseProduitsModalComponent, {
      backdrop: 'static',
      size: 'xl',
      centered: true
    });
    modalRef.componentInstance.codeRemise = codeRemise;

    modalRef.closed.subscribe(r => {
      this.load();
    });
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
