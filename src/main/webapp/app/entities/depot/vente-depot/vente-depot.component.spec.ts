import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VenteDepotComponent } from './vente-depot.component';

describe('VenteDepotComponent', () => {
  let component: VenteDepotComponent;
  let fixture: ComponentFixture<VenteDepotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VenteDepotComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(VenteDepotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
