import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RaportProduitVingtQuatreVingtComponent } from './raport-produit-vingt-quatre-vingt.component';

describe('RaportProduitVingtQuatreVingtComponent', () => {
  let component: RaportProduitVingtQuatreVingtComponent;
  let fixture: ComponentFixture<RaportProduitVingtQuatreVingtComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RaportProduitVingtQuatreVingtComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(RaportProduitVingtQuatreVingtComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
