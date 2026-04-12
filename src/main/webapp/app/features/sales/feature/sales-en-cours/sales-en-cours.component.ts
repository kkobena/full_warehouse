import {Component, DestroyRef, inject, OnInit, signal, viewChild} from '@angular/core';
import {CommonModule} from "@angular/common";
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Toolbar} from 'primeng/toolbar';
import {Select} from 'primeng/select';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {TooltipModule} from 'primeng/tooltip';

import {ISales, SalesStatut} from '../../../../shared/model';
import {SalesApiService} from '../../data-access/services/sales-api.service';
import {ConfirmDialogComponent} from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {ButtonGroup} from "primeng/buttongroup";
import {AbilityService} from '../../../../core/auth/ability.service';


@Component({
  selector: 'app-sales-en-cours',
  templateUrl: './sales-en-cours.component.html',
  styleUrl: './sales-en-cours.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TableModule,
    Toolbar,
    Select,
    IconField,
    InputIcon,
    InputText,
    TooltipModule,
    ConfirmDialogComponent,
    ButtonGroup,
    RouterLink,
  ],
})
export class SalesEnCoursComponent implements OnInit {
  private readonly api = inject(SalesApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ability = inject(AbilityService);
  protected readonly confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  protected readonly canDeleteEnCours = this.ability.canSignal('execute', 'ventes.en-cours.delete');

  protected loading = signal(false);
  protected sales: ISales[] = [];
  protected typeVentes = ['TOUT', 'VNO', 'VO'];
  protected typeVenteSelected = 'TOUT';
  protected search = '';
  protected useSimpleSale = false;

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .queryManagement({search: this.search || null, type: this.typeVenteSelected, statut: [SalesStatut.ACTIVE]})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.sales = res.body || [];
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }


  protected confirmDelete(sale: ISales): void {
    this.confirmDialog().onConfirm(
      () => this.deleteSale(sale),
      'Suppression',
      'Voulez-vous supprimer cette vente en cours ?',
    );
  }

  private deleteSale(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.deletePreventeById(sale.saleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  protected navigateToSale(sale: ISales): void {
    this.router.navigate(['/sales-home'], {
      state: {saleInfo: {saleId: sale.saleId}},
    });
  }

  protected openNewSalesHome(): void {
    this.router.navigate(['/sales-home']);
  }
}
