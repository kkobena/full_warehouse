import { Component, inject, OnDestroy } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { CommandeService } from './commande.service';
import { saveAs } from 'file-saver';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  templateUrl: './commande-import-response-dialog.component.html',
  imports: [WarehouseCommonModule, ButtonModule]
})
export class CommandeImportResponseDialogComponent implements OnDestroy {
  responseCommande?: ICommandeResponse;
  hiddenInfo = true;
  private activeModal = inject(NgbActiveModal);
  private commandeService = inject(CommandeService);
  private destroy$ = new Subject<void>();

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onClickLink(): void {
    this.commandeService.getRuptureCsv(this.responseCommande.reference).pipe(takeUntil(this.destroy$)).subscribe({
      next: blod => {
        saveAs(new Blob([blod], { type: 'text/csv' }), `${this.responseCommande.reference}.csv`);
        this.hiddenInfo = false;
      }
    });
  }
}
