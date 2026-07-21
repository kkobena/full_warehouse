import { Component, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, SelectComponent } from '../../../shared/ui';
import { ClassePareto } from 'app/shared/model/report/classe-pareto.enum';
import { CategorieABC } from 'app/shared/model/report/stock-rotation.model';
import ABCParetoComponent from '../abc-pareto/abc-pareto.component';
import StockRotationComponent from '../stock-rotation/stock-rotation.component';

@Component({
  selector: 'jhi-stock-abc',
  imports: [NgbNavModule, ABCParetoComponent, StockRotationComponent, FormsModule, SelectComponent, ButtonComponent],
  templateUrl: './stock-abc.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./stock-abc.component.scss'],
})
export default class StockABCComponent {
  @ViewChild(ABCParetoComponent) protected paretoComp?: ABCParetoComponent;
  @ViewChild(StockRotationComponent) protected rotationComp?: StockRotationComponent;

  protected readonly active = signal<string>('pareto');

  protected onActiveChange(tabId: string): void {
    this.active.set(tabId);
  }

  protected onParetoFamilleChange(v: string | null): void {
    this.paretoComp?.selectedFamille.set(v);
    this.paretoComp?.onFilterChange();
  }

  protected onParetoClasseChange(v: ClassePareto | '' | null): void {
    this.paretoComp?.selectedClassePareto.set(v || null);
    this.paretoComp?.onFilterChange();
  }

  protected onRotationCategorieChange(v: string | null): void {
    this.rotationComp?.selectedCategorie.set(v);
    this.rotationComp?.onFilterChange();
  }

  protected onRotationABCChange(v: CategorieABC | null): void {
    this.rotationComp?.selectedABC.set(v);
    this.rotationComp?.onFilterChange();
  }
}
