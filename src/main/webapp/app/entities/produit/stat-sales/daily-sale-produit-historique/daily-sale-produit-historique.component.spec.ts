import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DailySaleProduitHistoriqueComponent } from './daily-sale-produit-historique.component';

describe('DailySaleProduitHistoriqueComponent', () => {
  let component: DailySaleProduitHistoriqueComponent;
  let fixture: ComponentFixture<DailySaleProduitHistoriqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DailySaleProduitHistoriqueComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DailySaleProduitHistoriqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
