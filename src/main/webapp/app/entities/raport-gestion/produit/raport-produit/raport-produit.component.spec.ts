import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RaportProduitComponent } from './raport-produit.component';

describe('RaportProduitComponent', () => {
  let component: RaportProduitComponent;
  let fixture: ComponentFixture<RaportProduitComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RaportProduitComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RaportProduitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
