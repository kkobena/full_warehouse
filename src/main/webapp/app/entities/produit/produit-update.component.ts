import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { Dci, IProduit, Produit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { IRayon } from '../../shared/model/rayon.model';
import { IFamilleProduit } from '../../shared/model/famille-produit.model';
import { ITva } from '../../shared/model/tva.model';
import { CodeRemise } from '../../shared/model/remise.model';
import { IFormProduit } from '../../shared/model/form-produit.model';
import { IGammeProduit } from '../../shared/model/gamme-produit.model';
import { ILaboratoire } from '../../shared/model/laboratoire.model';
import { RayonService } from '../rayon/rayon.service';
import { LaboratoireProduitService } from '../laboratoire-produit/laboratoire-produit.service';
import { FormeProduitService } from '../forme-produit/forme-produit.service';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { FamilleProduitService } from '../famille-produit/famille-produit.service';
import { GammeProduitService } from '../gamme-produit/gamme-produit.service';
import { TvaService } from '../tva/tva.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputMaskModule } from 'primeng/inputmask';
import { RemiseService } from '../remise/remise.service';
import { DciService } from '../dci/dci.service';
import { SelectModule } from 'primeng/select';
import { Card } from 'primeng/card';
import { Checkbox } from 'primeng/checkbox';
import { Toolbar } from 'primeng/toolbar';

@Component({
  selector: 'jhi-produit-update',
  templateUrl: './produit-update.component.html',
  styleUrl: './produit-update.component.scss',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    KeyFilterModule,
    InputMaskModule,
    SelectModule,
    Card,
    Checkbox,
    Toolbar
  ]
})
export class ProduitUpdateComponent implements OnInit {
  protected isSaving = false;
  protected isValid = true;
  protected isDeconditionnable = false;
  protected isDatePeremptionChecked = false;
  protected formeProduits: IFormProduit[] = [];
  protected familleProduits: IFamilleProduit[] = [];
  protected laboratoires: ILaboratoire[] = [];
  protected gammes: IGammeProduit[] = [];
  protected tvas: ITva[] = [];
  protected fournisseurs: IFournisseur[] = [];
  protected rayons: IRayon[] = [];
  protected remisesCodes: CodeRemise[] = [];
  protected dcis: Dci[] = [];
  protected categories = [
    { code: 'A', libelle: 'Produits à forte rotation', z: 1.96 },
    { code: 'B', libelle: 'Produits à rotation moyenne', z: 1.65 },
    { code: 'C', libelle: 'Produits à faible rotation', z: 1.28 }
  ];

  protected readonly fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    tvaId: [null, [Validators.required]],
    familleId: [null, [Validators.required]],
    codeCip: [null, [Validators.required]],
    rayonId: [null, [Validators.required]],
    libelle: [null, [Validators.required]],
    costAmount: [null, [Validators.required]],
    regularUnitPrice: [null, [Validators.required]],
    fournisseurId: [null, [Validators.required]],
    createdAt: [],
    codeEan: [],
    qtyAppro: [],
    qtySeuilMini: [],
    remiseCode: [],
    gammeId: [],
    formeId: [],
    laboratoireId: [],
    deconditionnable: [false],
    dateperemption: [false],
    itemQty: [],
    itemCostAmount: [],
    itemRegularUnitPrice: [],
    expirationDate: [],
    dciId: [],
    categorie: [],
    codeEanLaboratoire: []
  });
  private readonly produitService = inject(ProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly rayonService = inject(RayonService);
  private readonly laboratoireService = inject(LaboratoireProduitService);
  private readonly formeProduitService = inject(FormeProduitService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly familleService = inject(FamilleProduitService);
  private readonly gammeProduitService = inject(GammeProduitService);
  private readonly tvaService = inject(TvaService);
  private readonly remiseService = inject(RemiseService);
  private readonly dciService = inject(DciService);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => {
      if (!produit.id) {
        const today = moment().startOf('day');
        produit.createdAt = today;
        produit.updatedAt = today;
      }

      this.updateForm(produit);
      this.populate(produit);
    });
  }

  populate(produit: IProduit): void {
    if (produit) {
      if (produit.deconditionnable) {
        this.isDeconditionnable = true;
      }
      if (produit.dateperemption) {
        this.isDatePeremptionChecked = true;
      }
    }

    this.tvaService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<ITva[]>) => {
        this.tvas = res.body;
      });

    this.fournisseurService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body;
      });
    this.rayonService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IRayon[]>) => {
        this.rayons = res.body;
      });
    this.laboratoireService.query().subscribe((res: HttpResponse<ILaboratoire[]>) => {
      this.laboratoires = res.body;
    });
    this.gammeProduitService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IGammeProduit[]>) => {
        this.gammes = res.body || [];
      });
    this.familleService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IFamilleProduit[]>) => {
        this.familleProduits = res.body;
      });
    this.formeProduitService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IFormProduit[]>) => {
        this.formeProduits = res.body;
      });
    this.remiseService.queryCodes().subscribe((res: HttpResponse<CodeRemise[]>) => {
      this.remisesCodes = res.body;
    });
    this.dciService.queryUnpaged().subscribe((res: HttpResponse<Dci[]>) => {
      this.dcis = res.body;
    });
  }

  updateForm(produit: IProduit): void {
    this.editForm.patchValue({
      id: produit.id,
      libelle: produit.libelle,
      costAmount: produit.costAmount,
      regularUnitPrice: produit.regularUnitPrice,
      createdAt: produit.createdAt ? produit.createdAt.format(DATE_TIME_FORMAT) : null,
      itemQty: produit.itemQty,
      itemCostAmount: produit.itemCostAmount,
      itemRegularUnitPrice: produit.itemRegularUnitPrice,
      tvaId: produit.tvaId,
      familleId: produit.familleId,
      codeCip: produit.codeCip,
      rayonId: produit.rayonId,
      fournisseurId: produit.fournisseurId,
      codeEan: produit.codeEan,
      qtyAppro: produit.qtyAppro,
      qtySeuilMini: produit.qtySeuilMini,
      remiseCode: produit.remiseCode,
      gammeId: produit.gammeId,
      laboratoireId: produit.laboratoireId,
      deconditionnable: produit.deconditionnable,
      dateperemption: produit.dateperemption,
      expirationDate: produit.expirationDate,
      formeId: produit.formeId,
      dciId: produit.dciId,
      categorie: produit.categorie
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const produit = this.createFromForm();
    if (produit.id !== undefined) {
      this.subscribeToSaveResponse(this.produitService.update(produit));
    } else {
      this.subscribeToSaveResponse(this.produitService.create(produit));
    }
  }

  handleCostInput(event: any): void {
    const value = Number(event.target.value);
    const unitPrice = Number(this.editForm.get(['regularUnitPrice']).value);
    this.isValid = value < unitPrice;
  }

  handleUnitPriceInput(event: any): void {
    const value = Number(event.target.value);
    const costAmount = Number(this.editForm.get(['costAmount']).value);
    this.isValid = costAmount < value;
  }

  handleItemQty(event: any): void {
    const itemQty = event.target.value;
    if (Number(itemQty) > 0) {
      const costAmount = Number(this.editForm.get(['costAmount']).value);
      const regularUnitPrice = Number(this.editForm.get(['regularUnitPrice']).value);
      const itemCostAmount = costAmount / itemQty;
      const itemRegularUnitPrice = regularUnitPrice / itemQty;
      this.editForm.get(['itemCostAmount']).setValue(itemCostAmount.toFixed());
      this.editForm.get(['itemRegularUnitPrice']).setValue(itemRegularUnitPrice.toFixed());
    } else {
      this.editForm.get(['itemCostAmount']).setValue(null);
      this.editForm.get(['itemRegularUnitPrice']).setValue(null);
    }
  }

  handleItemCost(event: any): void {
    const value = Number(event.target.value);
    const itemRegularUnitPrice = Number(this.editForm.get(['itemRegularUnitPrice']).value);
    this.isValid = value < itemRegularUnitPrice;
  }

  handleItemPrice(event: any): void {
    const value = event.target.value;
    const itemCostAmount = Number(this.editForm.get(['itemCostAmount']).value);
    this.isValid = itemCostAmount < Number(value);
  }

  onDatePeremtionCheck(value: boolean): void {
    this.isDatePeremptionChecked = value;
    if (this.isDatePeremptionChecked) {
      this.editForm.get('perimeAt').setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('perimeAt').updateValueAndValidity();
    } else {
      this.editForm.get('perimeAt').clearValidators();
      this.editForm.get('perimeAt').updateValueAndValidity();
    }
  }

  onDeconditionnable(value: boolean): void {
    console.log(value);
    this.isDeconditionnable = value;
    if (this.isDeconditionnable) {
      this.editForm.get('itemRegularUnitPrice').setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('itemRegularUnitPrice').updateValueAndValidity();
      this.editForm.get('itemCostAmount').setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('itemCostAmount').updateValueAndValidity();
      this.editForm.get('itemQty').setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('itemQty').updateValueAndValidity();
    } else {
      this.editForm.get('itemRegularUnitPrice').clearValidators();
      this.editForm.get('itemRegularUnitPrice').updateValueAndValidity();
      this.editForm.get('itemQty').clearValidators();
      this.editForm.get('itemQty').updateValueAndValidity();
      this.editForm.get('itemCostAmount').clearValidators();
      this.editForm.get('itemCostAmount').updateValueAndValidity();
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduit>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError()
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  private createFromForm(): IProduit {
    return {
      ...new Produit(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
      costAmount: this.editForm.get(['costAmount']).value,
      regularUnitPrice: this.editForm.get(['regularUnitPrice']).value,
      createdAt: this.editForm.get(['createdAt']).value ? moment(this.editForm.get(['createdAt']).value, DATE_TIME_FORMAT) : undefined,
      itemQty: this.editForm.get(['itemQty']).value,
      itemCostAmount: this.editForm.get(['itemCostAmount']).value,
      itemRegularUnitPrice: this.editForm.get(['itemRegularUnitPrice']).value,
      typeProduit: TypeProduit.PACKAGE,
      tvaId: this.editForm.get(['tvaId']).value,
      familleId: this.editForm.get(['familleId']).value,
      codeCip: this.editForm.get(['codeCip']).value,
      rayonId: this.editForm.get(['rayonId']).value,
      codeEan: this.editForm.get(['codeEan']).value,
      qtyAppro: this.editForm.get(['qtyAppro']).value,
      qtySeuilMini: this.editForm.get(['qtySeuilMini']).value,
      remiseCode: this.editForm.get(['remiseCode']).value,
      gammeId: this.editForm.get(['gammeId']).value,
      fournisseurId: this.editForm.get(['fournisseurId']).value,
      laboratoireId: this.editForm.get(['laboratoireId']).value,
      deconditionnable: this.editForm.get(['deconditionnable']).value,
      dateperemption: this.editForm.get(['dateperemption']).value,
      expirationDate: this.editForm.get(['expirationDate']).value,
      formeId: this.editForm.get(['formeId']).value,
      dciId: this.editForm.get(['dciId']).value,
      categorie: this.editForm.get(['categorie']).value,
      codeEanLaboratoire: this.editForm.get(['codeEanLaboratoire']).value
    };
  }
}
