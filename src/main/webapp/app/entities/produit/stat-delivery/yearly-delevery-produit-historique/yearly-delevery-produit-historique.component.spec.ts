import { ComponentFixture, TestBed } from '@angular/core/testing';

import { YearlyDeleveryProduitHistoriqueComponent } from './yearly-delevery-produit-historique.component';

describe('HearlyDeleveryProduitHistoriqueComponent', () => {
  let component: YearlyDeleveryProduitHistoriqueComponent;
  let fixture: ComponentFixture<YearlyDeleveryProduitHistoriqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [YearlyDeleveryProduitHistoriqueComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(YearlyDeleveryProduitHistoriqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
