import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IProduit, Produit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
import { ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from 'app/entities/categorie/categorie.service';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { IRayon } from '../../shared/model/rayon.model';
import { IFamilleProduit } from '../../shared/model/famille-produit.model';
import { ITva } from '../../shared/model/tva.model';
import { ITypeEtiquette } from '../../shared/model/type-etiquette.model';
import { IRemiseProduit } from '../../shared/model/remise-produit.model';
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
import { TypeEtiquetteService } from '../type-etiquette/type-etiquette.service';

@Component({
  selector: 'jhi-produit-update',
  templateUrl: './produit-update.component.html',
})
export class ProduitUpdateComponent implements OnInit {
  isSaving = false;
  isValid = true;
  isDeconditionnable = false;
  isDatePeremptionChecked = false;
  etiquettes: ITypeEtiquette[] = [];
  etiquetteSelected!: ITypeEtiquette | null;
  formeProduits: IFormProduit[] = [];
  formeSelected!: IFormProduit | null;
  familleProduits: IFamilleProduit[] = [];
  familleSelected!: IFamilleProduit | null;
  laboratoires: ILaboratoire[] = [];
  laboratoireSelected!: ILaboratoire | null;
  gammes: IGammeProduit[] = [];
  gammeSelected!: IGammeProduit | null;
  tvas: ITva[] = [];
  tvaSelected!: ITva | null;
  fournisseurs: IFournisseur[] = [];
  fournisseurSelected!: IFournisseur | null;
  rayonSelected!: IRayon | null;
  rayons: IRayon[] = [];
  remisesProduits: IRemiseProduit[] = [];
  remiseSelected!: IRemiseProduit | null;
  editForm = this.fb.group({
    id: [],
    typeEtyquetteId: [null, [Validators.required]],
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
    remiseId: [],
    gammeId: [],
    formeId: [],
    laboratoireId: [],
    deconditionnable: [false],
    dateperemption: [false],
    itemQty: [],
    itemCostAmount: [],
    itemRegularUnitPrice: [],
    expirationDate: [],
  });

  constructor(
    protected produitService: ProduitService,
    protected categorieService: CategorieService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder,
    protected rayonService: RayonService,
    protected laboratoireService: LaboratoireProduitService,
    protected formeProduitService: FormeProduitService,
    protected fournisseurService: FournisseurService,
    protected familleService: FamilleProduitService,
    protected gammeProduitService: GammeProduitService,
    protected tvaService: TvaService,
    protected typeEtiquetteService: TypeEtiquetteService
  ) {}

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
    this.typeEtiquetteService.query().subscribe((res: HttpResponse<ITypeEtiquette[]>) => {
      this.etiquettes = res.body || [];
      if (produit) {
        this.etiquetteSelected = this.etiquettes.find(e => e.id === produit.typeEtyquetteId) || null;
      }
    });
    this.tvaService.query().subscribe((res: HttpResponse<ITva[]>) => {
      this.tvas = res.body || [];
      this.tvaSelected = this.tvas.find(e => e.id === produit.tvaId) || null;
    });

    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
        this.fournisseurSelected = this.fournisseurs.find(e => e.id === produit.fournisseurId) || null;
      });
    this.rayonService.query().subscribe((res: HttpResponse<IRayon[]>) => {
      this.rayons = res.body || [];
      this.rayonSelected = this.rayons.find(e => e.id === produit.rayonId) || null;
    });
    this.laboratoireService.query().subscribe((res: HttpResponse<ILaboratoire[]>) => {
      this.laboratoires = res.body || [];
      this.laboratoireSelected = this.laboratoires.find(e => e.id === produit.laboratoireId) || null;
    });
    this.gammeProduitService.query().subscribe((res: HttpResponse<IGammeProduit[]>) => {
      this.gammes = res.body || [];
      this.gammeSelected = this.gammes.find(e => e.id === produit.gammeId) || null;
    });
    this.familleService.query().subscribe((res: HttpResponse<IFamilleProduit[]>) => {
      this.familleProduits = res.body || [];
      this.familleSelected = this.familleProduits.find(e => e.id === produit.familleId) || null;
    });
    this.formeProduitService.query().subscribe((res: HttpResponse<IFormProduit[]>) => {
      this.formeProduits = res.body || [];
      this.formeSelected = this.formeProduits.find(e => e.id === produit.formeId) || null;
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
      typeEtyquetteId: produit.typeEtyquetteId,
      tvaId: produit.tvaId,
      familleId: produit.familleId,
      codeCip: produit.codeCip,
      rayonId: produit.rayonId,
      fournisseurId: produit.fournisseurId,
      codeEan: produit.codeEan,
      qtyAppro: produit.qtyAppro,
      qtySeuilMini: produit.qtySeuilMini,
      remiseId: produit.remiseId,
      gammeId: produit.gammeId,
      laboratoireId: produit.laboratoireId,
      deconditionnable: produit.deconditionnable,
      dateperemption: produit.dateperemption,
      expirationDate: produit.expirationDate,
      formeId: produit.formeId,
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

  trackById(index: number, item: ICategorie): any {
    return item.id;
  }

  handleCostInput(event: any): void {
    const value = Number(event.target.value);
    const unitPrice = Number(this.editForm.get(['regularUnitPrice'])!.value);
    if (value >= unitPrice) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  handleUnitPriceInput(event: any): void {
    const value = Number(event.target.value);
    const costAmount = Number(this.editForm.get(['costAmount'])!.value);
    if (costAmount >= value) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  handleItemQty(event: any): void {
    const itemQty = event.target.value;
    if (Number(itemQty) > 0) {
      const costAmount = Number(this.editForm.get(['costAmount'])!.value);
      const regularUnitPrice = Number(this.editForm.get(['regularUnitPrice'])!.value);
      const itemCostAmount = costAmount / itemQty;
      const itemRegularUnitPrice = regularUnitPrice / itemQty;
      this.editForm.get(['itemCostAmount'])!.setValue(itemCostAmount.toFixed());
      this.editForm.get(['itemRegularUnitPrice'])!.setValue(itemRegularUnitPrice.toFixed());
    } else {
      this.editForm.get(['itemCostAmount'])!.setValue(null);
      this.editForm.get(['itemRegularUnitPrice'])!.setValue(null);
    }
  }

  handleItemCost(event: any): void {
    const value = Number(event.target.value);
    const itemRegularUnitPrice = Number(this.editForm.get(['itemRegularUnitPrice'])!.value);
    if (value >= itemRegularUnitPrice) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  handleItemPrice(event: any): void {
    const value = event.target.value;
    const itemCostAmount = Number(this.editForm.get(['itemCostAmount'])!.value);
    if (itemCostAmount >= Number(value)) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  onDatePeremtionCheck(value: any): void {
    this.isDatePeremptionChecked = value.currentTarget.checked;
    if (this.isDatePeremptionChecked) {
      this.editForm.get('perimeAt')!.setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('perimeAt')!.updateValueAndValidity();
    } else {
      this.editForm.get('perimeAt')!.clearValidators();
      this.editForm.get('perimeAt')!.updateValueAndValidity();
    }
  }

  onDeconditionnable(value: any): void {
    this.isDeconditionnable = value.currentTarget.checked;
    if (this.isDeconditionnable) {
      this.editForm.get('itemRegularUnitPrice')!.setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('itemRegularUnitPrice')!.updateValueAndValidity();
      this.editForm.get('itemCostAmount')!.setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('itemCostAmount')!.updateValueAndValidity();
      this.editForm.get('itemQty')!.setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get('itemQty')!.updateValueAndValidity();
    } else {
      this.editForm.get('itemRegularUnitPrice')!.clearValidators();
      this.editForm.get('itemRegularUnitPrice')!.updateValueAndValidity();
      this.editForm.get('itemQty')!.clearValidators();
      this.editForm.get('itemQty')!.updateValueAndValidity();
      this.editForm.get('itemCostAmount')!.clearValidators();
      this.editForm.get('itemCostAmount')!.updateValueAndValidity();
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduit>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
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
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
      costAmount: this.editForm.get(['costAmount'])!.value,
      regularUnitPrice: this.editForm.get(['regularUnitPrice'])!.value,
      createdAt: this.editForm.get(['createdAt'])!.value ? moment(this.editForm.get(['createdAt'])!.value, DATE_TIME_FORMAT) : undefined,
      itemQty: this.editForm.get(['itemQty'])!.value,
      itemCostAmount: this.editForm.get(['itemCostAmount'])!.value,
      itemRegularUnitPrice: this.editForm.get(['itemRegularUnitPrice'])!.value,
      typeProduit: TypeProduit.PACKAGE,
      typeEtyquetteId: this.editForm.get(['typeEtyquetteId'])!.value?.id,
      tvaId: this.editForm.get(['tvaId'])!.value?.id,
      familleId: this.editForm.get(['familleId'])!.value?.id,
      codeCip: this.editForm.get(['codeCip'])!.value,
      rayonId: this.editForm.get(['rayonId'])!.value?.id,
      codeEan: this.editForm.get(['codeEan'])!.value,
      qtyAppro: this.editForm.get(['qtyAppro'])!.value,
      qtySeuilMini: this.editForm.get(['qtySeuilMini'])!.value,
      remiseId: this.editForm.get(['remiseId'])!.value?.id,
      gammeId: this.editForm.get(['gammeId'])!.value?.id,
      fournisseurId: this.editForm.get(['fournisseurId'])!.value?.id,
      laboratoireId: this.editForm.get(['laboratoireId'])!.value?.id,
      deconditionnable: this.editForm.get(['deconditionnable'])!.value,
      dateperemption: this.editForm.get(['dateperemption'])!.value,
      expirationDate: this.editForm.get(['expirationDate'])!.value,
      formeId: this.editForm.get(['formeId'])!.value?.id,
    };
  }
}
