import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { IMagasin, TypeMagasin } from 'app/shared/model/magasin.model';
import { ButtonComponent, CardComponent } from 'app/shared/ui';
import { MagasinFormValue, porteIdentiteFne } from '../../models/magasin.model';

/** Une saisie vide est une absence : le serveur ne doit pas recevoir de chaîne vide (l'email la refuserait). */
const vide = (valeur: string | null): string | null => (valeur?.trim() ? valeur.trim() : null);

/**
 * Saisie de la fiche d'un magasin — officine ou unité de gestion.
 *
 * Composant de présentation : il reçoit le magasin, émet le magasin complété, et laisse la page
 * enregistrer. Tout ce qu'il n'affiche pas (type, rattachement, capacité de commander, compte
 * bancaire, responsable) est repris tel quel : l'ancien formulaire repartait d'une officine vierge,
 * si bien qu'enregistrer une unité de gestion la changeait en officine détachée.
 */
@Component({
  selector: 'app-magasin-form',
  templateUrl: './magasin-form.component.html',
  styleUrl: './magasin-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ButtonComponent, CardComponent],
})
export class MagasinFormComponent {
  readonly magasin = input.required<IMagasin>();
  readonly saving = input(false);

  readonly enregistrer = output<IMagasin>();
  readonly annuler = output();

  protected readonly avecIdentiteFne = computed(() => porteIdentiteFne(this.magasin()));
  protected readonly estUniteDeGestion = computed(() => this.magasin().typeMagasin === TypeMagasin.UNITE_GESTION);

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly fullNameInput = viewChild.required<ElementRef<HTMLInputElement>>('fullNameInput');

  protected readonly form = this.fb.group({
    fullName: ['', Validators.required],
    name: ['', Validators.required],
    phone: ['', Validators.required],
    email: this.fb.control<string | null>(null, [Validators.minLength(5), Validators.maxLength(254), Validators.email]),
    address: this.fb.control<string | null>(null),
    registre: this.fb.control<string | null>(null),
    compteContribuable: this.fb.control<string | null>(null),
    numComptable: this.fb.control<string | null>(null),
    registreImposition: this.fb.control<string | null>(null),
    fnePointOfSale: this.fb.control<string | null>(null),
    fneSecretKey: this.fb.control<string | null>(null),
    welcomeMessage: this.fb.control<string | null>(null),
    note: this.fb.control<string | null>(null),
  });

  constructor() {
    effect(() => {
      const m = this.magasin();
      this.form.reset({
        fullName: m.fullName ?? '',
        name: m.name ?? '',
        phone: m.phone ?? '',
        email: m.email ?? null,
        address: m.address ?? null,
        registre: m.registre ?? null,
        compteContribuable: m.compteContribuable ?? null,
        numComptable: m.numComptable ?? null,
        registreImposition: m.registreImposition ?? null,
        fnePointOfSale: m.fnePointOfSale ?? null,
        fneSecretKey: m.fneSecretKey ?? null,
        welcomeMessage: m.welcomeMessage ?? null,
        note: m.note ?? null,
      });
    });
    // Après le rendu, à chaque magasin reçu : le champ existe alors à coup sûr.
    afterRenderEffect(() => {
      this.magasin();
      this.fullNameInput().nativeElement.focus();
    });
  }

  protected invalide(champ: keyof MagasinFormValue): boolean {
    const controle = this.form.controls[champ];
    return controle.invalid && (controle.touched || controle.dirty);
  }

  protected erreur(champ: keyof MagasinFormValue, code: string): boolean {
    return this.invalide(champ) && this.form.controls[champ].hasError(code);
  }

  protected soumettre(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const m = this.magasin();
    const avecFne = this.avecIdentiteFne();
    this.enregistrer.emit({
      // Repris tels quels : non saisis ici.
      id: m.id,
      typeMagasin: m.typeMagasin,
      parentId: m.parentId,
      commandeEnPropre: m.commandeEnPropre,
      compteBancaire: m.compteBancaire,
      managerLastName: m.managerLastName,
      managerFirstName: m.managerFirstName,
      // Saisis.
      fullName: v.fullName.trim(),
      name: v.name.trim(),
      phone: v.phone.trim(),
      email: vide(v.email),
      address: vide(v.address),
      registre: vide(v.registre),
      compteContribuable: vide(v.compteContribuable),
      numComptable: vide(v.numComptable),
      registreImposition: vide(v.registreImposition),
      welcomeMessage: vide(v.welcomeMessage),
      note: vide(v.note),
      // Hors de la racine, le serveur refuserait une identité FNE : elle n'est jamais envoyée.
      fnePointOfSale: avecFne ? vide(v.fnePointOfSale) : null,
      fneSecretKey: avecFne ? vide(v.fneSecretKey) : null,
    });
  }
}
