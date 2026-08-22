import { ChangeDetectionStrategy, Component } from '@angular/core';

import { BalanceMvtCaisseComponent } from 'app/entities/mvt-caisse/balance-mvt-caisse/balance-mvt-caisse.component';
import { TaxeReportComponent } from 'app/entities/mvt-caisse/taxe-report/taxe-report.component';
import { TableauPharmacienComponent } from 'app/entities/mvt-caisse/tableau-pharmacien/tableau-pharmacien.component';

/*
 * Les trois états de comptabilité, en chiffre d'affaires RÉEL.
 *
 * Les menus de comptabilité rendent le chiffre **déclaré** — c'est lui que la comptabilité exploite.
 * Ces trois écrans-ci donnent au pharmacien la vue sur ce qu'il a réellement encaissé, exclusions et
 * ponctions écartées.
 *
 * Ce sont des enveloppes et non des copies : le composant d'origine est réutilisé tel quel, seul le
 * mode change. Dupliquer les gabarits aurait produit deux versions à maintenir en parallèle, qui
 * divergeraient dès la première colonne ajoutée d'un côté seulement.
 *
 * Pourquoi une entrée de menu distincte plutôt qu'une bascule sur l'écran de comptabilité : un
 * interrupteur à portée de clic rend le chiffre affiché ambigu — on ne sait plus, en revenant sur
 * l'écran ou en imprimant, lequel des deux on regarde. Deux menus, deux chiffres, aucune ambiguïté.
 */

@Component({
  selector: 'app-balance-reelle',
  imports: [BalanceMvtCaisseComponent],
  template: `<app-balance-mvt-caisse mode="REEL" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BalanceReelleComponent {}

@Component({
  selector: 'app-taxe-report-reel',
  imports: [TaxeReportComponent],
  template: `<app-taxe-report mode="REEL" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxeReportReelComponent {}

@Component({
  selector: 'app-tableau-pharmacien-reel',
  imports: [TableauPharmacienComponent],
  template: `<app-tableau-pharmacien mode="REEL" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableauPharmacienReelComponent {}
