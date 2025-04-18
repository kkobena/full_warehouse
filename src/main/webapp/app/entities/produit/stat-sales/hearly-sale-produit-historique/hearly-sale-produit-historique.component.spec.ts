import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HearlySaleProduitHistoriqueComponent } from './hearly-sale-produit-historique.component';

describe('HearlySaleProduitHistoriqueComponent', () => {
  let component: HearlySaleProduitHistoriqueComponent;
  let fixture: ComponentFixture<HearlySaleProduitHistoriqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HearlySaleProduitHistoriqueComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HearlySaleProduitHistoriqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
