import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { IMagasin, Magasin, TypeMagasin } from '../../shared/model/magasin.model';
import { MagasinService } from '../magasin/magasin.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { FloatLabelModule } from 'primeng/floatlabel';
import { TextareaModule } from 'primeng/textarea';
import { Toolbar } from 'primeng/toolbar';

@Component({
  selector: 'jhi-depot-form',
  imports: [
    WarehouseCommonModule,
    ReactiveFormsModule,
    RouterModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    Select,
    TextareaModule,
    FloatLabelModule,
    Toolbar,
  ],
  templateUrl: './depot-form.component.html',
  styleUrl: './depot-form.component.scss',
})
export class DepotFormComponent implements OnInit, AfterViewInit {
  protected depotForm!: FormGroup;
  protected isEditMode = false;
  protected isSaving = false;
  protected depotId?: number;

  protected typeMagasinOptions = [
    /*    { label: 'Officine', value: TypeMagasin.OFFICINE },*/
    { label: 'Dépôt extension', value: TypeMagasin.DEPOT },
  ];

  private fb = inject(FormBuilder);
  private readonly magasinService = inject(MagasinService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly name = viewChild.required<ElementRef>('name');

  get formControls() {
    return this.depotForm.controls;
  }

  ngOnInit(): void {
    this.createForm();

    this.route.params.subscribe(params => {
      if (params['id'] && params['id'] !== 'new') {
        this.isEditMode = true;
        this.depotId = +params['id'];
        this.loadDepot(this.depotId);
      }
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.name().nativeElement.focus();
    }, 100);
  }

  createForm(): void {
    this.depotForm = this.fb.group({
      id: [null],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      fullName: ['', [Validators.required, Validators.maxLength(200)]],
      typeMagasin: [TypeMagasin.DEPOT, [Validators.required]],
      phone: ['', [Validators.maxLength(20)]],
      email: ['', [Validators.email, Validators.maxLength(100)]],
      address: ['', [Validators.maxLength(500)]],
      registre: ['', [Validators.maxLength(100)]],
      compteContribuable: ['', [Validators.maxLength(100)]],
      numComptable: ['', [Validators.maxLength(100)]],
      compteBancaire: ['', [Validators.maxLength(100)]],
      registreImposition: ['', [Validators.maxLength(100)]],
      welcomeMessage: ['', [Validators.maxLength(500)]],
      note: ['', [Validators.maxLength(1000)]],
      managerLastName: ['', [Validators.maxLength(100)]],
      managerFirstName: ['', [Validators.maxLength(200)]],
    });
  }

  loadDepot(id: number): void {
    this.magasinService.find(id).subscribe({
      next: (res: HttpResponse<IMagasin>) => {
        if (res.body) {
          this.updateForm(res.body);
        }
      },
      error: () => {
        this.router.navigate(['/depot']);
      },
    });
  }

  updateForm(depot: IMagasin): void {
    this.depotForm.patchValue({
      id: depot.id,
      name: depot.name,
      fullName: depot.fullName,
      typeMagasin: depot.typeMagasin,
      phone: depot.phone,
      email: depot.email,
      address: depot.address,
      registre: depot.registre,
      compteContribuable: depot.compteContribuable,
      numComptable: depot.numComptable,
      compteBancaire: depot.compteBancaire,
      registreImposition: depot.registreImposition,
      welcomeMessage: depot.welcomeMessage,
      note: depot.note,
      managerLastName: depot.managerLastName,
      managerFirstName: depot.managerFirstName,
    });
  }

  save(): void {
    if (this.depotForm.valid) {
      this.isSaving = true;
      const depot = this.createFromForm();

      if (depot.id) {
        this.magasinService.update(depot).subscribe({
          next: () => {
            this.isSaving = false;
            this.router.navigate(['/depot']);
          },
          error: () => {
            this.isSaving = false;
          },
        });
      } else {
        this.magasinService.create(depot).subscribe({
          next: () => {
            this.isSaving = false;
            this.router.navigate(['/depot']);
          },
          error: () => {
            this.isSaving = false;
          },
        });
      }
    }
  }

  createFromForm(): IMagasin {
    return {
      ...new Magasin(),
      id: this.depotForm.get(['id'])!.value,
      name: this.depotForm.get(['name'])!.value,
      fullName: this.depotForm.get(['fullName'])!.value,
      typeMagasin: this.depotForm.get(['typeMagasin'])!.value,
      phone: this.depotForm.get(['phone'])!.value,
      email: this.depotForm.get(['email'])!.value,
      address: this.depotForm.get(['address'])!.value,
      registre: this.depotForm.get(['registre'])!.value,
      compteContribuable: this.depotForm.get(['compteContribuable'])!.value,
      numComptable: this.depotForm.get(['numComptable'])!.value,
      compteBancaire: this.depotForm.get(['compteBancaire'])!.value,
      registreImposition: this.depotForm.get(['registreImposition'])!.value,
      welcomeMessage: this.depotForm.get(['welcomeMessage'])!.value,
      note: this.depotForm.get(['note'])!.value,
      managerLastName: this.depotForm.get(['managerLastName'])!.value,
      managerFirstName: this.depotForm.get(['managerFirstName'])!.value,
    };
  }

  cancel(): void {
    this.router.navigate(['/depot']);
  }
}
