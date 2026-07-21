import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { IResponseDto } from '../../../shared/util/response-dto';
import { ProduitService } from '../produit.service';
import { saveAs } from 'file-saver';
import { finalize } from 'rxjs/operators';
import { ButtonComponent, CardComponent } from '../../../shared/ui';

@Component({
  selector: 'jhi-import-produit-reponse-modal',
  imports: [CardComponent, ButtonComponent],
  templateUrl: './import-produit-reponse-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['../../common-modal.component.scss'],
})
export class ImportProduitReponseModalComponent {
  header = '';
  responsedto: IResponseDto | null = null;
  private readonly activeModal = inject(NgbActiveModal);
  private readonly produitService = inject(ProduitService);

  cancel(): void {
    this.activeModal.dismiss();
  }

  protected onClickLink(): void {
    this.produitService
      .getRejectCsv(this.responsedto.rejectFileUrl)
      .pipe(finalize(() => this.activeModal.dismiss()))
      .subscribe({
        next: blod => {
          saveAs(new Blob([blod], { type: 'text/csv' }), this.responsedto.rejectFileUrl);
        },
      });
  }
}
