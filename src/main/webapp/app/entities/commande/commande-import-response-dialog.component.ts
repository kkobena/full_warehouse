import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { CommandeService } from './commande.service';
import { saveAs } from 'file-saver';

@Component({
  templateUrl: './commande-import-response-dialog.component.html',
})
export class CommandeImportResponseDialogComponent {
  responseCommande?: ICommandeResponse;
  hiddenInfo = true;

  constructor(public activeModal: NgbActiveModal, protected eventManager: JhiEventManager, protected commandeService: CommandeService) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  onClickLink(): void {
    this.commandeService.getRuptureCsv(this.responseCommande?.reference!).subscribe(
      blod => saveAs(blod),
      () => (this.hiddenInfo = false)
    );
  }
}
