import { Component, OnInit } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ToastModule } from 'primeng/toast';
import { IResponseDto } from '../../shared/util/response-dto';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IRemise, Remise, RemiseType } from '../../shared/model/remise.model';
import { RemiseService } from './remise.service';
import { DropdownModule } from 'primeng/dropdown';
import { CalendarModule } from 'primeng/calendar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { StyleClassModule } from 'primeng/styleclass';

@Component({
  selector: 'jhi-remise',
  standalone: true,
  providers: [MessageService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ConfirmDialogModule,
    DialogModule,
    ToolbarModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    RouterModule,
    TableModule,
    TooltipModule,
    KeyFilterModule,
    ToastModule,
    DropdownModule,
    CalendarModule,
    InputSwitchModule,
    StyleClassModule,
  ],
  templateUrl: './remise.component.html',
  styleUrl: './remise.component.scss',
})
export class RemiseComponent implements OnInit {
  fileDialog?: boolean;
  responsedto!: IResponseDto;
  responseDialog?: boolean;
  entites?: IRemise[];
  loading = false;
  isSaving = false;
  displayDialog?: boolean;
  types: RemiseType[] = [RemiseType.remiseProduit, RemiseType.remiseClient];
  begin: Date = new Date();
  end: Date = new Date();
  editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    valeur: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    type: new FormControl<RemiseType | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),

    remiseValue: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true,
    }),
    /* begin: new FormControl<Date | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true,
    }),
    end: new FormControl<Date | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true,
    }), */
  });

  constructor(
    protected entityService: RemiseService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private messageService: MessageService,
    protected modalService: ConfirmationService,
    private fb: FormBuilder,
  ) {}

  loadPage(): void {
    this.loading = true;
    this.entityService.query().subscribe({
      next: (res: HttpResponse<IRemise[]>) => this.onSuccess(res.body),
      error: () => this.onError(),
    });
  }

  lazyLoading(): void {
    this.loadPage();
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
    });
  }

  updateForm(entity: IRemise): void {
    this.editForm.patchValue({
      id: entity.id,
      valeur: entity.valeur,
      remiseValue: entity.remiseValue,
      type: this.getRemiseTypeFromString(entity.type),
      // begin: entity.begin ? new Date(entity.begin) : null,
      // end: entity.end ? new Date(entity.end) : null,
    });
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id !== undefined) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  addNewEntity(): void {
    this.updateForm(new Remise());
    this.displayDialog = true;
  }

  onEdit(entity: IRemise): void {
    this.updateForm(entity);
    this.displayDialog = true;
  }

  delete(entity: IRemise): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  ngOnInit(): void {
    this.loadPage();
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  protected onStatusChange(entity: IRemise): void {
    this.subscribeToSaveResponse(this.entityService.changeStatus(entity));
  }

  protected onSuccess(data: IRemise[] | null): void {
    //    this.router.navigate(['/remises']);
    this.entites = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.displayDialog = false;
    this.loadPage();
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: "L'enregistrement n'a pas été effectué!",
    });
    this.loadPage();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IRemise>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private createFromForm(): IRemise {
    return {
      ...new Remise(),
      id: this.editForm.get(['id'])!.value,
      remiseValue: this.editForm.get(['remiseValue'])!.value,
      valeur: this.editForm.get(['valeur'])!.value,
      type: this.getRemiseType(this.editForm.get(['type'])!.value),
      // end: moment(this.editForm.get(['end'])!.value).format('yyyy-MM-DD'),
      // begin: moment(this.editForm.get(['begin'])!.value).format('yyyy-MM-DD'),
    };
  }

  private getRemiseTypeFromString(typeValue: string): RemiseType {
    let type;
    switch (typeValue) {
      case 'remiseClient':
        type = RemiseType.remiseClient;
        break;
      case 'remiseProduit':
        type = RemiseType.remiseProduit;
        break;
    }
    return type;
  }

  private getRemiseType(typeValue: string): string {
    let type;
    switch (typeValue) {
      case 'Remise client':
        type = 'remiseClient';
        break;
      case 'Remise produit':
        type = 'remiseProduit';
        break;
    }
    return type;
  }
}
