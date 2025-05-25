import {Component, inject, OnInit, signal} from '@angular/core';
import {AbstractCommande} from "../../../shared/model/abstract-commande.model";
import {CommonModule} from "@angular/common";
import {OrderStatut} from "../../../shared/model/enumerations/order-statut.model";
import {CommandeUpdateComponent} from "../commande-update.component";
import {CommandeStockEntryComponent} from "../commande-stock-entry.component";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'jhi-edition-commande',
  imports: [CommonModule, CommandeUpdateComponent, CommandeStockEntryComponent],
  templateUrl: './edition-commande.component.html',
})
export class EditionCommandeComponent implements OnInit {
  commande = signal<AbstractCommande>(null);
  protected readonly RECEIVED = OrderStatut.RECEIVED;
  protected readonly REQUESTED = OrderStatut.REQUESTED;
  private readonly activatedRoute = inject(ActivatedRoute);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({commande}) => {
      this.commande.set(commande)
    });
  }
}
