import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Customer, ICustomer } from '../../../shared/model/customer.model';
import TranslateDirective from '../../../shared/language/translate.directive';
import { AssureFormStepService } from './assure-form-step.service';
import { DateNaissDirective } from '../../../shared/date-naiss.directive';
import {
  ButtonComponent,
  CardComponent,
  KeyFilterDirective,
  RadioComponent
} from '../../../shared/ui';

/**
 * Bénéficiaires du contrat : une liste, pas une seule personne. Un contrat couvre en général le
 * conjoint ET plusieurs enfants ; les saisir en une fois évite de créer d'autres « clients » pour
 * contourner la limite, ce qui fausse les statistiques de famille et la facturation au tiers payant.
 *
 * Une ligne entièrement vide est ignorée (la première est proposée d'office) ; une ligne à moitié
 * remplie bloque l'enregistrement.
 */
@Component({
  selector: 'jhi-ayant-droit-step',
  imports: [
    ReactiveFormsModule,
    TranslateDirective,
    DateNaissDirective,
    ButtonComponent,
    CardComponent,
    KeyFilterDirective,
    RadioComponent
  ],
  templateUrl: './ayant-droit-step.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./assured-form-step.component.scss'],
})
export class AyantDroitStepComponent implements OnInit {
  assureFormStepService = inject(AssureFormStepService);
  fb = inject(UntypedFormBuilder);
  editForm = this.fb.group({
    ayantDroits: this.fb.array([]),
  });

  get lignes(): FormArray {
    return this.editForm.get('ayantDroits') as FormArray;
  }

  ngOnInit(): void {
    // Le brouillon (lignes brutes, même incomplètes) prime : on ne perd rien en changeant d'onglet.
    const brouillon = this.assureFormStepService.ayantDroitsBrouillon();
    const enregistres = this.assureFormStepService.assure()?.ayantDroits ?? [];
    const depart = brouillon.length > 0 ? brouillon : enregistres;
    depart.forEach(ayantDroit => this.lignes.push(this.creerLigne(ayantDroit)));
    if (this.lignes.length === 0) {
      this.ajouter();
    }
  }

  ajouter(): void {
    this.lignes.push(this.creerLigne());
  }

  supprimer(index: number): void {
    this.lignes.removeAt(index);
    this.editForm.markAsDirty();
    if (this.lignes.length === 0) {
      this.ajouter();
    }
  }

  isValidForm(): boolean {
    return this.lignes.controls.every(ligne => this.estLigneVide(ligne as FormGroup) || ligne.valid);
  }

  /** Les bénéficiaires complets, prêts à être envoyés. */
  createFromForm(): ICustomer[] {
    return this.lignes.controls
      .filter(ligne => !this.estLigneVide(ligne as FormGroup) && ligne.valid)
      .map(ligne => this.versClient((ligne as FormGroup).value));
  }

  saveFormState(): void {
    const brouillon = this.lignes.controls.filter(ligne => !this.estLigneVide(ligne as FormGroup)).map(ligne => (ligne as FormGroup).value);
    this.assureFormStepService.ayantDroitsBrouillon.set(brouillon);
    this.assureFormStepService.ayantDroitsValides.set(this.isValidForm());
    const currentAssure = this.assureFormStepService.assure();
    if (currentAssure) {
      currentAssure.ayantDroits = this.createFromForm();
      this.assureFormStepService.setAssure(currentAssure);
    }
  }

  goBack(): void {
    this.saveFormState();
  }

  protected libelleLigne(index: number): string {
    const ligne = this.lignes.at(index) as FormGroup;
    const nom = [ligne.get('firstName')?.value, ligne.get('lastName')?.value].filter(Boolean).join(' ');
    return nom ? `Ayant droit n°${index + 1} — ${nom}` : `Ayant droit n°${index + 1}`;
  }

  private creerLigne(ayantDroit?: Partial<ICustomer>): FormGroup {
    return this.fb.group({
      id: [ayantDroit?.id ?? null],
      firstName: [ayantDroit?.firstName ?? null, [Validators.required]],
      lastName: [ayantDroit?.lastName ?? null, [Validators.required]],
      numAyantDroit: [ayantDroit?.numAyantDroit ?? null, [Validators.required]],
      datNaiss: [ayantDroit?.datNaiss ?? null],
      sexe: [ayantDroit?.sexe ?? null],
    });
  }

  /** Une ligne dont aucun champ n'est renseigné est ignorée : elle n'empêche pas l'enregistrement. */
  private estLigneVide(ligne: FormGroup): boolean {
    const valeur = ligne.value;
    return !valeur.firstName && !valeur.lastName && !valeur.numAyantDroit && !valeur.datNaiss && !valeur.sexe;
  }

  private versClient(valeur: any): ICustomer {
    return {
      ...new Customer(),
      id: valeur.id,
      firstName: valeur.firstName,
      lastName: valeur.lastName,
      numAyantDroit: valeur.numAyantDroit,
      datNaiss: valeur.datNaiss,
      sexe: valeur.sexe,
      type: 'ASSURE',
    };
  }
}
