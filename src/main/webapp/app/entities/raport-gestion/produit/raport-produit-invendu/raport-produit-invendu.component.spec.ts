import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RaportProduitInvenduComponent } from './raport-produit-invendu.component';

describe('RaportProduitInvenduComponent', () => {
  let component: RaportProduitInvenduComponent;
  let fixture: ComponentFixture<RaportProduitInvenduComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RaportProduitInvenduComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RaportProduitInvenduComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
