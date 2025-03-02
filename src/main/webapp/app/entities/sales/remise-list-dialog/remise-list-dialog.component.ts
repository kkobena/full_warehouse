import { Component, inject, OnInit } from '@angular/core';
import { IRemise, RemiseType } from '../../../shared/model/remise.model';
import { RemiseService } from '../../remise/remise.service';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-remise-list-dialog',
  imports: [WarehouseCommonModule, FormsModule],
  templateUrl: './remise-list-dialog.component.html',
})
export class RemiseListDialogComponent implements OnInit {
  types: RemiseType[] = [RemiseType.remiseProduit, RemiseType.remiseClient];
  entityService = inject(RemiseService);
  entites?: IRemise[];
  type: RemiseType = null;
  activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.entityService.query().subscribe({
      next: (res: HttpResponse<IRemise[]>) => {
        this.entites = res.body.filter(remise => remise.enable);
      },
    });
  }

  onDbleClick(remise: IRemise): void {
    this.onSelect(remise);
  }

  onSelect(remise: IRemise): void {
    this.activeModal.close(remise);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
