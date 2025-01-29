import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastModule } from 'primeng/toast';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ProduitService } from '../../produit/produit.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { RemiseService } from '../remise.service';
import { CodeRemise, GrilleRemise, IRemise, Remise } from '../../../shared/model/remise.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { MessagesModule } from 'primeng/messages';

@Component({
  selector: 'jhi-remise-produit-form-modal',
  providers: [MessageService, ConfirmationService],
  imports: [ReactiveFormsModule, ToastModule, MessagesModule, ButtonModule],
  templateUrl: './remise-produit-form-modal.component.html',
  styles: ``,
})
export class RemiseProduitFormModalComponent implements OnInit, AfterViewInit {
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  produitService = inject(ProduitService);
  messageService = inject(MessageService);
  entityService = inject(RemiseService);
  libelle = viewChild.required<ElementRef>('libelle');
  fb = inject(FormBuilder);
  editForm = this.fb.group({
    id: new FormControl<number | null>(null),
    valeur: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    codeRemise: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),

    vno: this.fb.group({
      id: new FormControl<number | null>(null),
      code: new FormControl<string | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
      remiseValue: new FormControl<number | null>(null, {
        validators: [Validators.min(0)],
      }),
    }),

    vo: this.fb.group({
      id: new FormControl<number | null>(null),
      code: new FormControl<string | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
      remiseValue: new FormControl<number | null>(null, {
        validators: [Validators.min(0)],
      }),
    }),
  });
  entity: IRemise | null = null;
  protected isSaving = false;
  protected title: string | null = null;
  protected remisesCodes: CodeRemise[] = [];

  get grilleVno(): FormGroup {
    return this.editForm.get('vno') as FormGroup;
  }

  get grilleVo(): FormGroup {
    return this.editForm.get('vo') as FormGroup;
  }

  get isValid(): boolean {
    return this.editForm?.get('vno')?.get('remiseValue')?.value > 0 || this.editForm?.get('vo')?.get('remiseValue')?.value > 0;
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  updateForm(entity: IRemise): void {
    this.editForm.patchValue({
      id: entity.id,
      valeur: entity.valeur,
      codeRemise: entity.grilles?.length ? entity.grilles[0].codeRemise.value : null,
      vo: this.buildGrille(entity.grilles?.find(grille => grille.grilleType === 'VO')),
      vno: this.buildGrille(entity.grilles?.find(grille => grille.grilleType === 'VNO')),
    });
  }

  ngAfterViewInit(): void {
    this.editForm.get('codeRemise')?.valueChanges.subscribe(value => {
      if (value) {
        this.editForm.get('grilles')?.reset();
        const cordeRemise = this.remisesCodes.find(code => code.value === value);
        this.addGrille(cordeRemise);
      }
    });
    if (this.entity) {
      this.updateForm(this.entity);
      this.editForm.get('codeRemise')?.disable();
    }
    this.libelle().nativeElement.focus();
  }

  ngOnInit(): void {
    if (this.entity) {
      this.fetchAllCode();
    } else {
      this.fetchCode();
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IRemise>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private buildGrille(grille: GrilleRemise): any {
    return {
      id: grille.id,
      code: grille.code,
      remiseValue: grille.remiseValue,
    };
  }

  private fetchCode(): void {
    this.entityService.queryCodes().subscribe(async (res: HttpResponse<CodeRemise[]>) => {
      let codes = res.body?.filter(code => code.value != '0') || [];
      const grilles = await this.fetchGrilles();
      this.remisesCodes = codes.filter(code => !grilles.find(grille => grille.code === code.codeVo || grille.code === code.codeVno));
    });
  }

  private fetchAllCode(): void {
    this.entityService.queryCodes().subscribe((res: HttpResponse<CodeRemise[]>) => {
      this.remisesCodes = res.body?.filter(code => code.value != '0') || [];
    });
  }

  private fetchGrilles(): Promise<GrilleRemise[]> {
    return new Promise((resolve, reject) => {
      this.entityService.queryGrilles().subscribe({
        next: (res: HttpResponse<GrilleRemise[]>) => {
          const grillesRemises = res.body || [];
          resolve(grillesRemises);
        },
        error: err => reject(err),
      });
    });
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close();
  }

  private onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: "L'enregistrement n'a pas été effectué!",
    });
  }

  private addGrille(codeRemise: CodeRemise): void {
    this.grilleVno.patchValue({ code: codeRemise.codeVno });
    this.grilleVo.patchValue({ code: codeRemise.codeVo });
  }

  private buildGrilles(): GrilleRemise[] {
    const grilles: GrilleRemise[] = [];

    grilles.push({
      id: this.editForm.get('vno')?.get('id')?.value,
      code: this.editForm.get('vno')?.get('code')?.value,
      remiseValue: this.editForm.get('vno')?.get('remiseValue')?.value || 0,
      grilleType: 'VNO',
    });

    grilles.push({
      id: this.editForm.get('vo')?.get('id')?.value,
      code: this.editForm.get('vo')?.get('code')?.value,
      remiseValue: this.editForm.get('vo')?.get('remiseValue')?.value || 0,
      grilleType: 'VO',
    });

    return grilles;
  }

  private createFromForm(): IRemise {
    return {
      ...new Remise(),
      id: this.editForm.get(['id'])!.value,
      valeur: this.editForm.get(['valeur'])!.value,
      type: 'remiseProduit',
      grilles: this.buildGrilles(),
    };
  }
}
