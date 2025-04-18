import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DaylyDeleveryProduitHistoriqueComponent } from './dayly-delevery-produit-historique.component';

describe('DaylyDeleveryProduitHistoriqueComponent', () => {
  let component: DaylyDeleveryProduitHistoriqueComponent;
  let fixture: ComponentFixture<DaylyDeleveryProduitHistoriqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DaylyDeleveryProduitHistoriqueComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DaylyDeleveryProduitHistoriqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
