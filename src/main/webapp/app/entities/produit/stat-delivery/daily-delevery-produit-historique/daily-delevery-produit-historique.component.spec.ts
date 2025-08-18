import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DailyDeleveryProduitHistoriqueComponent } from './daily-delevery-produit-historique.component';

describe('DaylyDeleveryProduitHistoriqueComponent', () => {
  let component: DailyDeleveryProduitHistoriqueComponent;
  let fixture: ComponentFixture<DailyDeleveryProduitHistoriqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DailyDeleveryProduitHistoriqueComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(DailyDeleveryProduitHistoriqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
