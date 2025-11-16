import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RaportProduitVenduComponent } from './raport-produit-vendu.component';

describe('RaportProduitVenduComponent', () => {
  let component: RaportProduitVenduComponent;
  let fixture: ComponentFixture<RaportProduitVenduComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RaportProduitVenduComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RaportProduitVenduComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
