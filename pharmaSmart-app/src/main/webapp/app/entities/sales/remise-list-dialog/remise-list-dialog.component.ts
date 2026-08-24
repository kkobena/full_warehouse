import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { IRemise, RemiseType } from '../../../shared/model/remise.model';
import { RemiseService } from '../../remise/remise.service';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from "@fortawesome/angular-fontawesome";

@Component({
  selector: 'app-remise-list-dialog',
  imports: [FormsModule, FaIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './remise-list-dialog.component.html',
  styleUrls: ['./remise-list-dialog.component.scss'],
})
export class RemiseListDialogComponent implements OnInit {
  entityService = inject(RemiseService);
  protected readonly entites = signal<IRemise[] | undefined>(undefined);
  type: RemiseType = null;
  activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.entityService.query().subscribe({
      next: (res: HttpResponse<IRemise[]>) => {
        this.entites.set(res.body.filter(remise => remise.enable));
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
