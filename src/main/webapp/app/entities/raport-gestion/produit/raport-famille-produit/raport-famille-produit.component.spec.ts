import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RaportFamilleProduitComponent } from './raport-famille-produit.component';

describe('RaportFamilleProduitComponent', () => {
  let component: RaportFamilleProduitComponent;
  let fixture: ComponentFixture<RaportFamilleProduitComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RaportFamilleProduitComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RaportFamilleProduitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
