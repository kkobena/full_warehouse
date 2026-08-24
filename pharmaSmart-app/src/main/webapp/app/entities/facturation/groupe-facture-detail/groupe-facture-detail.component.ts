import { ChangeDetectionStrategy,Component, inject, input, OnInit, signal} from '@angular/core';

import {Facture} from '../facture.model';
import {CommonModule} from '@angular/common';
import {HttpResponse} from '@angular/common/http';
import {FactureService} from '../facture.service';
import {FormsModule} from '@angular/forms';
import {CardComponent, DataTableComponent, IconFieldComponent} from '../../../shared/ui';

@Component({
  selector: 'app-groupe-facture-detail',
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    DataTableComponent,
    IconFieldComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './groupe-facture-detail.component.html',
  styleUrls: ['./groupe-facture-detail.component.scss'],
})
export class GroupeFactureDetailComponent implements OnInit {
  readonly groupeFacture = input<Facture | null>(null);
  groupeFactureSignal = signal(this.groupeFacture());
  protected readonly selectedFacture = signal<Facture | null>(null);
  factureService = inject(FactureService);
  searchValue: string | undefined;

  onRowSelect(factureItem: Facture) {
    this.selectedFacture.set(factureItem);
  }

  ngOnInit(): void {
    this.groupeFactureSignal.set(this.groupeFacture());
    this.factureService.find(this.groupeFactureSignal().factureItemId).subscribe((res: HttpResponse<Facture>) => {
      this.groupeFactureSignal.set(res.body);
    });
  }
}
