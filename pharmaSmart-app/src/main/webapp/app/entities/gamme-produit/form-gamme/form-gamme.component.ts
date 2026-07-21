import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnInit,
  viewChild
} from '@angular/core';
import {FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators} from '@angular/forms';
import {Observable} from 'rxjs';
import {GammeProduitService} from '../gamme-produit.service';
import {GammeProduit, IGammeProduit} from '../../../shared/model/gamme-produit.model';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ErrorService} from '../../../shared/error.service';
import {CommonModule} from '@angular/common';
import {ButtonComponent, KeyFilterDirective} from '../../../shared/ui';
import {NotificationService} from "../../../shared/services/notification.service";

@Component({
  selector: 'app-form-gamme',
  templateUrl: './form-gamme.component.html',
  styleUrls: ['./form-gamme.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ButtonComponent, KeyFilterDirective],
})
export class FormGammeComponent implements OnInit, AfterViewInit {
  header = '';
  gamme: IGammeProduit | null = null;
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    code: [],
    libelle: [null, [Validators.required]],
  });
  private readonly entityService = inject(GammeProduitService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly libelle = viewChild.required<ElementRef>('libelle');
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    if (this.gamme) {
      this.updateForm(this.gamme);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.libelle().nativeElement.focus();
    }, 100);
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    if (entity.id !== undefined && entity.id !== null) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IGammeProduit>>): void {
    result.subscribe({
      next: (res: HttpResponse<IGammeProduit>) => this.onSaveSuccess(res.body),
      error: err => this.onSaveError(err),
    });
  }

  private onSaveSuccess(response: IGammeProduit | null): void {
    this.activeModal.close(response);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  private updateForm(entity: IGammeProduit): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
    });
  }

  private createFromForm(): IGammeProduit {
    return {
      ...new GammeProduit(),
      id: this.editForm.get(['id']).value,
      code: this.editForm.get(['code']).value,
      libelle: this.editForm.get(['libelle']).value,
    };
  }
}
