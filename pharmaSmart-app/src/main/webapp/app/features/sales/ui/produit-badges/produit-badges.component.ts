import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';

/** `AAAA-MM-JJ` → `MM/AAAA` : en pharmacie la péremption se lit au mois. */
export function formaterMoisPeremption(date: string): string {
  const [annee, mois] = date.split('-');
  return `${mois}/${annee}`;
}

export function formaterDatePeremption(date: string): string {
  const [annee, mois, jour] = date.split('-');
  return `${jour}/${mois}/${annee}`;
}

/** Jours entre aujourd'hui et la date (négatif : déjà périmée). */
export function joursAvant(date: string): number {
  const aujourdhui = new Date();
  const debutJour = Date.UTC(aujourdhui.getFullYear(), aujourdhui.getMonth(), aujourdhui.getDate());
  const [annee, mois, jour] = date.split('-').map(Number);
  return Math.round((Date.UTC(annee, mois - 1, jour) - debutJour) / 86_400_000);
}

/** Repères visibles d'un produit au comptoir : princeps/générique, stupéfiant, péremption proche. */
@Component({
  selector: 'app-produit-badges',
  standalone: true,
  imports: [NgbTooltip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (typeGenerique() === 'PRINCEPS') {
      <span class="badge-produit badge-princeps" ngbTooltip="Princeps (référentiel médicament)">P</span>
    } @else if (typeGenerique()) {
      <span class="badge-produit badge-generique" ngbTooltip="Générique (référentiel médicament)">G</span>
    }
    @if (stupefiant()) {
      <span class="badge-produit badge-stupefiant" ngbTooltip="Stupéfiant : ordonnance sécurisée, traçabilité du lot">Stup</span>
    }
    @if (stock(); as s) {
      <span [class.badge-rupture]="s.rupture" [ngbTooltip]="s.infobulle" class="badge-produit badge-stock">
        <i class="pi pi-arrow-down"></i>{{ s.rupture ? 'Rupture' : 'Stock faible' }}
      </span>
    }
    @if (peremption(); as p) {
      <span [class.badge-perime]="p.perime" [ngbTooltip]="p.infobulle" class="badge-produit badge-peremption">
        <i class="pi pi-clock"></i>Lot {{ peremptionLot() }} · {{ p.mois }}
      </span>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      gap: 0.25rem;
      margin-left: 0.35rem;
      vertical-align: middle;
    }
    .badge-produit {
      padding: 0 0.3rem;
      border-radius: 3px;
      font-size: 0.65rem;
      font-weight: 700;
      line-height: 1.3;
      white-space: nowrap;
    }
    .badge-generique {
      background: #e3f2e6;
      color: #2d7a3e;
    }
    .badge-princeps {
      background: #e2ebf8;
      color: #1f4f9c;
    }
    .badge-stupefiant {
      background: #fde7e9;
      color: #b02a37;
    }
    .badge-peremption {
      background: #fff1d6;
      color: #8a5a00;
    }
    .badge-stock {
      background: #fff1d6;
      color: #8a5a00;
    }
    .badge-rupture {
      background: #fde7e9;
      color: #b02a37;
    }
    .badge-stock i {
      margin-right: 0.2rem;
      font-size: 0.6rem;
    }
    .badge-perime {
      background: #fde7e9;
      color: #b02a37;
    }
    .badge-peremption i {
      margin-right: 0.2rem;
      font-size: 0.6rem;
    }
  `,
})
export class ProduitBadgesComponent {
  readonly typeGenerique = input<string | null | undefined>(null);
  readonly statutLegal = input<string | null | undefined>(null);
  /** Lot concerné et sa date de péremption (AAAA-MM-JJ) : seul un lot porte une date. */
  readonly peremptionLot = input<string | null | undefined>(null);
  readonly peremptionDate = input<string | null | undefined>(null);

  /** Stock restant et seuil mini : sans seuil (0) il n'y a rien à signaler. */
  readonly stockRestant = input<number | null | undefined>(null);
  readonly seuilMini = input<number | null | undefined>(null);

  protected readonly stock = computed(() => {
    const restant = this.stockRestant();
    const seuil = this.seuilMini();
    if (restant == null || !seuil || seuil <= 0 || restant > seuil) {
      return null;
    }
    return {rupture: restant <= 0, infobulle: `Stock restant : ${restant} (seuil mini : ${seuil})`};
  });

  protected readonly stupefiant = computed(() => this.statutLegal() === 'STUPEFIANTS');

  protected readonly peremption = computed(() => {
    const date = this.peremptionDate();
    if (!date || !this.peremptionLot()) {
      return null;
    }
    const jours = joursAvant(date);
    const perime = jours < 0;
    const quand = perime ? `périmé depuis ${-jours} j` : `périme dans ${jours} j`;
    return {mois: formaterMoisPeremption(date), perime, infobulle: `Lot ${this.peremptionLot()} : ${formaterDatePeremption(date)} (${quand})`};
  });
}
