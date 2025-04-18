import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HearlyDeleveryProduitHistoriqueComponent } from './hearly-delevery-produit-historique.component';

describe('HearlyDeleveryProduitHistoriqueComponent', () => {
  let component: HearlyDeleveryProduitHistoriqueComponent;
  let fixture: ComponentFixture<HearlyDeleveryProduitHistoriqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HearlyDeleveryProduitHistoriqueComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HearlyDeleveryProduitHistoriqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
