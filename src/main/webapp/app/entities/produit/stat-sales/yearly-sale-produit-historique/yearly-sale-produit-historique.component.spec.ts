import { ComponentFixture, TestBed } from '@angular/core/testing';

import { YearlySaleProduitHistoriqueComponent } from './yearly-sale-produit-historique.component';

describe('HearlySaleProduitHistoriqueComponent', () => {
  let component: YearlySaleProduitHistoriqueComponent;
  let fixture: ComponentFixture<YearlySaleProduitHistoriqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [YearlySaleProduitHistoriqueComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(YearlySaleProduitHistoriqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
