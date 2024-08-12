import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { CommandeService } from './commande.service';
import { saveAs } from 'file-saver';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';

@Component({
  templateUrl: './commande-import-response-dialog.component.html',
  standalone: true,
  imports: [WarehouseCommonModule, ButtonModule],
})
export class CommandeImportResponseDialogComponent {
  responseCommande?: ICommandeResponse;
  hiddenInfo = true;

  constructor(
    public activeModal: NgbActiveModal,
    protected commandeService: CommandeService,
  ) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  onClickLink(): void {
    this.commandeService.getRuptureCsv(this.responseCommande.reference).subscribe({
      next: blod => {
        saveAs(new Blob([blod], { type: 'text/csv' }), `${this.responseCommande.reference}.csv`);
        // blod => saveAs(blod),
        this.hiddenInfo = false;
      },
    });
  }
}
